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

use crate::config::Config;
use axum::{
    body::Body,
    extract::FromRequestParts,
    http::{header::USER_AGENT, Extensions, HeaderMap, HeaderValue, Method, Request, Uri, Version},
    middleware::Next,
    response::IntoResponse,
};
use rand::distributions::{Alphanumeric, DistString};
use std::{fmt::Display, ops::Deref, time::Instant};

#[derive(FromRequestParts)]
pub struct Metadata {
    extensions: Extensions,
    version: Version,
    headers: HeaderMap,
    method: Method,
    uri: Uri,
}

/// Represents the generated `x-request-id` header that the server creates on each
/// request invocation.
#[derive(Debug, Clone)]
pub struct XRequestId(String);

impl XRequestId {
    /// Generates a new [`XRequestId`].
    pub(self) fn generate() -> XRequestId {
        XRequestId(Alphanumeric.sample_string(&mut rand::thread_rng(), 12))
    }
}

impl Display for XRequestId {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(&self.0)
    }
}

impl Deref for XRequestId {
    type Target = str;
    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl From<XRequestId> for HeaderValue {
    fn from(value: XRequestId) -> HeaderValue {
        // we know that it'll always be valid UTF-8
        HeaderValue::from_str(&value).unwrap()
    }
}

pub async fn request_id(mut req: Request<Body>, next: Next) -> impl IntoResponse {
    let id = XRequestId::generate();
    req.extensions_mut().insert(id.clone());

    let config = req.extensions().get::<Config>().unwrap();

    let mut headers = HeaderMap::new();
    headers.insert("x-request-id", id.into());
    headers.insert(
        "server",
        HeaderValue::from_str(
            format!(
                "Noelware/Hazel{} (+https://github.com/Noelware/hazel; v{})",
                config
                    .server_name
                    .as_ref()
                    .map(|server| format!(": {server}"))
                    .unwrap_or_default(),
                crate::version(),
            )
            .as_str(),
        )
        .unwrap(),
    );

    (headers, next.run(req).await)
}

pub async fn log(metadata: Metadata, req: Request<Body>, next: Next) -> impl IntoResponse {
    let uri = metadata.uri.path();
    if uri.contains("/healthz") {
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
        _ => unimplemented!(),
    };

    let id = metadata.extensions.get::<XRequestId>().unwrap();
    let user_agent = metadata
        .headers
        .get(USER_AGENT)
        .map(|f| String::from_utf8_lossy(f.as_bytes()).to_string());

    let http_span = info_span!(
        "ume.http.request",
        req.ua = user_agent,
        req.id = %id,
        http.uri = uri,
        http.method = method,
        http.version = version
    );

    let _guard = http_span.enter();
    info!(
        http.uri = uri,
        http.method = method,
        http.version = version,
        req.id = %id,
        req.ua = user_agent,
        "processing request"
    );

    let res = next.run(req).await;
    let now = start.elapsed();

    info!(
        http.uri = uri,
        http.method = method,
        http.version = version,
        req.ua = user_agent,
        response.status = res.status().as_u16(),
        latency = ?now,
        req.id = %id,
        "processed request successfully"
    );

    res
}
