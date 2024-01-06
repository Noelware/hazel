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

use axum::{
    http::{header, HeaderMap, HeaderValue, Method, Request, Uri, Version},
    middleware::Next,
    response::IntoResponse,
};
use std::time::Instant;

#[derive(axum::extract::FromRequestParts)]
pub struct Metadata {
    pub(crate) uri: Uri,
    pub(crate) method: Method,
    pub(crate) version: Version,
    pub(crate) headers: HeaderMap,
}

pub async fn log<B>(metadata: Metadata, req: Request<B>, next: Next<B>) -> impl IntoResponse {
    let uri = metadata.uri.path();
    if uri.contains("/heartbeat") {
        return next.run(req).await;
    }

    let start = Instant::now();
    let method = metadata.method.as_str();
    let version = match metadata.version {
        Version::HTTP_09 => "http/0.9",
        Version::HTTP_10 => "http/1.0",
        Version::HTTP_11 => "http/1.1",
        Version::HTTP_2 => "http/2.0",
        Version::HTTP_3 => "http/3.0",
        _ => "http/???",
    };

    let default_ua = HeaderValue::from_static("unknown");
    let ua = metadata
        .headers
        .get(header::USER_AGENT)
        .unwrap_or(&default_ua)
        .to_str()
        .unwrap_or("unknown");

    info!(
        req.uri = uri,
        req.method = method,
        req.version = version,
        req.ua = ua,
        "processing request"
    );

    let span = info_span!(
        "hazel.http.request",
        req.uri = uri,
        req.method = method,
        req.version = version,
        req.ua = ua
    );

    let _guard = span.enter();
    let res = next.run(req).await;
    let now = format!("{:?}", start.elapsed());

    let status = res.status();
    info!(
        req.uri = uri,
        req.method = method,
        req.version = version,
        req.ua = ua,
        res.status = status.as_u16(),
        latency = now,
        "processed request",
    );

    res
}
