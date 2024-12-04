// 🪶 Hazel: Easy to use read-only proxy to map objects to URLs
// Copyright 2022-2024 Noelware, LLC. <team@noelware.org>
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use super::middlewares;
use crate::config::Config;
use axum::{
    body::Body,
    extract::Path,
    http::{header, StatusCode},
    response::{IntoResponse, Response},
    routing, Extension, Json, Router,
};
use azalia::remi::core::{Blob, StorageService as _};
use azalia::remi::StorageService;
use serde_json::json;
use std::any::Any;

pub fn create_router(storage: StorageService, config: Config) -> Router {
    Router::new()
        .route("/healthz", routing::get(healthz))
        .route("/*file", routing::get(query))
        .route("/", routing::get(main))
        .layer(sentry_tower::NewSentryLayer::new_from_top())
        .layer(sentry_tower::SentryHttpLayer::with_transaction())
        .layer(tower_http::catch_panic::CatchPanicLayer::custom(panic_handler))
        .layer(axum::middleware::from_fn(middlewares::log))
        .layer(axum::middleware::from_fn(middlewares::request_id))
        .layer(Extension(storage))
        .layer(Extension(config))
}

fn panic_handler(message: Box<dyn Any + Send + 'static>) -> Response<Body> {
    let details = azalia::message_from_panic(message);
    error!(%details, "route has panic'd");

    Response::builder()
        .status(StatusCode::INTERNAL_SERVER_ERROR)
        .header(header::CONTENT_TYPE, "application/json; charset=utf-8")
        .body(Body::from(
            serde_json::to_string(&json!({
                "status": "failed",
                "message": "was unable to complete request",
                "context": json!({
                    "new_issue_uri": "https://github.com/Noelware/hazel/issues/new"
                })
            }))
            .unwrap(),
        ))
        .unwrap()
}

#[cfg_attr(debug_assertions, axum::debug_handler)]
async fn main(Extension(config): Extension<Config>) -> impl IntoResponse {
    Json(json!({
        "hello": "world",
        "server": config.server_name.unwrap_or("unknown".into()),
        "build": {
            "version": crate::version(),
            "timestamp": crate::BUILD_TIMESTAMP,
        }
    }))
}

#[cfg_attr(debug_assertions, axum::debug_handler)]
async fn healthz() -> &'static str {
    "Ok."
}

#[instrument(name = "hazel.http.proxy", skip(storage))]
#[cfg_attr(debug_assertions, axum::debug_handler)]
async fn query(
    Path(path): Path<String>,
    Extension(storage): Extension<StorageService>,
) -> Result<Response<Body>, (StatusCode, Json<serde_json::Value>)> {
    let query = path.trim_start_matches('/').split('/').collect::<Vec<_>>();
    let query = match storage {
        StorageService::Filesystem(_) => format!("./{}", query.join("/")),
        _ => format!("/{}", query.join("/")),
    };

    info!(%query, "performing query");
    let Some(blob) = storage
        .blob(&query)
        .await
        .inspect_err(|e| {
            error!(error = %e, query, "unable to perform lookup on query");
            sentry::capture_error(e);
        })
        .map_err(|_| {
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(json!({
                    "status": "failed",
                    "message": "unable to perform lookup on query! try again later maybe?",
                    "context": {
                        "query": query
                    }
                })),
            )
        })?
    else {
        return Err((
            StatusCode::NOT_FOUND,
            Json(json!({
                "status": "not_found",
                "message": "object was not found",
                "context": {
                    "query": query
                }
            })),
        ));
    };

    match blob {
        Blob::File(file) => {
            let ct = file
                .content_type
                .unwrap_or_else(|| String::from(azalia::remi::fs::DEFAULT_CONTENT_TYPE));

            Ok(Response::builder()
                .status(StatusCode::OK)
                .header("Content-Type", ct)
                .body(file.data.into())
                .unwrap())
        }

        Blob::Directory(_) => Err((
            StatusCode::NOT_FOUND,
            Json(json!({
                "status": "not_found",
                "message": "object was not found",
                "context": {
                    "query": query
                }
            })),
        )),
    }
}
