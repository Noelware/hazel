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

mod middleware;
mod res;

use crate::{app::Hazel, remi::StorageServiceDelegate, BUILD_DATE, COMMIT_HASH, VERSION};
use axum::{
    extract::{Path, State},
    http::{header, Response, StatusCode},
    response::IntoResponse,
    routing::*,
    Router,
};
use remi_core::{Blob, StorageService};
use res::*;
use sentry::integrations::tower::SentryHttpLayer;
use sentry_tower::NewSentryLayer;
use serde_json::json;
use tower::ServiceBuilder;

const APPLICATION_OCTET_STREAM: &str = "application/octet-stream";

pub fn router(state: Hazel) -> Router {
    let layer = ServiceBuilder::new()
        .layer(NewSentryLayer::new_from_top())
        .layer(SentryHttpLayer::with_transaction());

    Router::new()
        .route("/", get(main))
        .route("/heartbeat", get(heartbeat))
        .route("/*file", get(match_all))
        .layer(layer)
        .layer(axum::middleware::from_fn(middleware::log))
        .with_state(state)
}

pub async fn main() -> impl IntoResponse {
    ok(
        StatusCode::OK,
        json!({
            "hello": "world",
            "build": json!({
                "version": VERSION,
                "commit": format!("https://github.com/Noelware/hazel/commit/{COMMIT_HASH}"),
                "build_date": BUILD_DATE
            })
        }),
    )
}

pub async fn heartbeat() -> &'static str {
    "Ok."
}

#[instrument(name = "hazel.proxy", skip(app))]
pub async fn match_all(
    Path(path): Path<String>,
    State(app): State<Hazel>,
) -> Result<impl IntoResponse, ApiResponse<Empty>> {
    let paths = path.trim_start_matches('/').split('/').collect::<Vec<_>>();
    let look_for = match app.storage {
        StorageServiceDelegate::Filesystem(_) => format!("./{}", paths.join("/")),
        _ => paths.join("/"),
    };

    let blob = app.storage.blob(look_for.clone()).await.map_err(|e| {
        error!(%e, "unable to locate blob");
        sentry::capture_error(&e);

        err(
            StatusCode::INTERNAL_SERVER_ERROR,
            ("INTERNAL_SERVER_ERROR", "Unable to fetch item").into(),
        )
    })?;

    if blob.is_none() {
        return Err::<Response<_>, ApiResponse<Empty>>(err(
            StatusCode::NOT_FOUND,
            ("NOT_FOUND", format!("Route '{look_for}' was not found").as_str()).into(),
        ));
    }

    let blob = blob.unwrap();
    match blob {
        Blob::Directory(_) => Err::<Response<_>, ApiResponse<Empty>>(err(
            StatusCode::NOT_FOUND,
            ("NOT_FOUND", format!("Route '{look_for}' was not found").as_str()).into(),
        )),

        Blob::File(file) => {
            let contents = file.data();
            let octet_str: &String = &APPLICATION_OCTET_STREAM.into();
            let headers = [(header::CONTENT_TYPE, file.content_type().unwrap_or(octet_str).as_str())];

            Ok((headers, contents.clone()).into_response())
        }
    }
}
