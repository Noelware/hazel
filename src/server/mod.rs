// 🪶 hazel: Minimal, and easy HTTP proxy to map storage provider items into HTTP endpoints
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

use std::{any::Any, time::Duration};

use crate::config::Config;
use axum::{
    body::Body,
    extract::Path,
    http::{header, Response, StatusCode},
    response::IntoResponse,
    routing, Extension, Json, Router,
};
use axum_server::{tls_rustls::RustlsConfig, Handle};
use eyre::Context;
use noelware_remi::StorageService;
use remi::{Blob, File, StorageService as _};
use serde_json::json;

pub mod config;
mod middleware;

pub async fn start(storage: StorageService, config: Config) -> eyre::Result<()> {
    info!("starting Hazel HTTP service!");

    let router: Router = Router::new()
        .route("/healthz", routing::get(healthz))
        .route("/*file", routing::get(lookup_file))
        .route("/", routing::get(main))
        .layer(sentry_tower::NewSentryLayer::new_from_top())
        .layer(sentry_tower::SentryHttpLayer::with_transaction())
        .layer(tower_http::catch_panic::CatchPanicLayer::custom(panic_handler))
        .layer(axum::middleware::from_fn(middleware::log))
        .layer(axum::middleware::from_fn(middleware::request_id))
        .layer(Extension(storage))
        .layer(Extension(config.clone()));

    match config.server.ssl {
        Some(ref ssl) => start_https_server(&config.server, ssl, router.clone()).await,
        None => start_http_server(&config.server, router).await,
    }
}

async fn start_https_server(config: &config::Config, ssl: &config::ssl::Config, router: Router) -> eyre::Result<()> {
    let handle = Handle::new();
    tokio::spawn(shutdown_signal(Some(handle.clone())));

    let addr = config.addr();
    let config = RustlsConfig::from_pem_file(&ssl.cert, &ssl.cert_key).await?;

    info!(address = %addr, "listening on HTTPS");
    axum_server::bind_rustls(addr, config)
        .handle(handle)
        .serve(router.into_make_service())
        .await
        .context("failed to run HTTPS server")
}

async fn start_http_server(config: &config::Config, router: Router) -> eyre::Result<()> {
    let addr = config.addr();
    let listener = tokio::net::TcpListener::bind(addr).await?;
    info!(address = ?addr, "listening on HTTP");

    axum::serve(listener, router.into_make_service())
        .with_graceful_shutdown(shutdown_signal(None))
        .await
        .context("failed to run HTTP server")
}

async fn shutdown_signal(handle: Option<Handle>) {
    let ctrl_c = async {
        tokio::signal::ctrl_c().await.expect("unable to install CTRL+C handler");
    };

    #[cfg(unix)]
    let terminate = async {
        tokio::signal::unix::signal(tokio::signal::unix::SignalKind::terminate())
            .expect("unable to install signal handler")
            .recv()
            .await;
    };

    #[cfg(not(unix))]
    let terminate = std::future::pending::<()>();

    tokio::select! {
        _ = ctrl_c => {}
        _ = terminate => {}
    }

    warn!("received terminal signal! shutting down");
    if let Some(handle) = handle {
        handle.graceful_shutdown(Some(Duration::from_secs(10)));
    }
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
                "message": "was unable to complete request, report this to Noelware",
                "context": json!({
                    "new_issue_uri": "https://github.com/Noelware/hazel/issues/new"
                })
            }))
            .unwrap(),
        ))
        .unwrap()
}

async fn main() -> impl IntoResponse {
    Json(json!({
        "hello": "world",
        "build": json!({
            "version": crate::version(),
            "build": crate::BUILD_DATE
        })
    }))
}

async fn healthz() -> &'static str {
    "Ok."
}

#[instrument(name = "hazel.http.proxy", skip(storage))]
#[cfg_attr(debug_assertions, axum::debug_handler)]
async fn lookup_file(
    Path(path): Path<String>,
    Extension(storage): Extension<StorageService>,
) -> Result<Response<Body>, Json<serde_json::Value>> {
    let query = path.trim_start_matches('/').split('/').collect::<Vec<_>>();
    let query = match storage {
        StorageService::Filesystem(_) => format!("./{}", query.join("/")),
        _ => format!("/{}", query.join("/")),
    };

    trace!(%query, "performing lookup on");

    let blob = storage
        .blob(&query)
        .await
        .inspect_err(|e| {
            error!(error = %e, %query, "unable to perform lookup");
            sentry::capture_error(e);
        })
        .map_err(|_| {
            Json(json!({
                "status": "failed",
                "message": "unable to perform lookup on query, try again later",
                "context": json!({"query":query})
            }))
        })?;

    let Some(blob) = blob else {
        return Err(Json(json!({
            "status": "not_found",
            "message": "unable to find object by query",
            "context": json!({"query":query})
        })));
    };

    match blob {
        Blob::Directory(_) => {
            return Err(Json(json!({
                "status": "not_found",
                "message": "unable to find object by query",
                "context": json!({"query":query})
            })))
        }

        Blob::File(File { content_type, data, .. }) => {
            let mut ct = content_type.unwrap_or_else(|| String::from("application/octet-stream"));

            // detect JSON if we found any JSON data since remi_fs doesn't do it properly.
            if serde_json::from_slice::<serde_json::Value>(&data).is_ok() {
                ct = "application/json".into();
            }

            Ok(Response::builder()
                .header(header::CONTENT_TYPE, ct.as_str())
                .body(Body::from(data))
                .unwrap())
        }
    }
}
