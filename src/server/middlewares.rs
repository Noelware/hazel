// 🪶 Hazel: Easy to use read-only proxy to map objects to URLs
// Copyright 2022-2025 Noelware, LLC. <team@noelware.org>
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
    Extension,
    body::Body,
    extract::{FromRequestParts, MatchedPath, Request},
    http::{Extensions, HeaderMap, HeaderValue, Method, Uri, Version, header::USER_AGENT},
    middleware::Next,
    response::IntoResponse,
};
use rand::distr::{Alphanumeric, SampleString};
use std::{fmt::Display, ops::Deref, time::Instant};
use tracing::{info, instrument};

/// Represents the generated `x-request-id` header that the server creates on each
/// request invocation.
#[derive(Debug, Clone)]
pub struct XRequestId(String);

impl XRequestId {
    /// Generates a new [`XRequestId`].
    pub(self) fn generate() -> XRequestId {
        XRequestId(Alphanumeric.sample_string(&mut rand::rng(), 12))
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

pub async fn request_id(
    Extension(config): Extension<Config>,
    mut req: Request<Body>,
    next: Next,
) -> impl IntoResponse {
    use std::fmt::Write;

    let id = XRequestId::generate();
    req.extensions_mut().insert(id.clone());

    let mut server = String::from("Noelware/hazel");
    if let Some(ref name) = config.server_name {
        write!(server, " [{name}]").unwrap();
    }

    write!(server, " (+https://github.com/Noelware/hazel; v{})", crate::version()).unwrap();

    let mut headers = HeaderMap::new();
    headers.insert("x-request-id", id.into());
    headers.insert("server", HeaderValue::from_str(&server).unwrap());

    (headers, next.run(req).await)
}

#[derive(FromRequestParts)]
pub struct Metadata {
    extensions: Extensions,
    version: Version,
    headers: HeaderMap,
    matched: Option<MatchedPath>,
    method: Method,
    uri: Uri,
}

#[instrument(name = "hazel.http.request", skip_all, fields(
    req.matched_path = %display_opt(metadata.matched.as_ref().map(MatchedPath::as_str)),
    req.ua = %display_opt(get_user_agent(&metadata)),
    req.id = %metadata.extensions.get::<XRequestId>().unwrap(),
    http.version = http_version(&metadata),
    http.method = metadata.method.as_str(),
    http.uri = metadata.uri.path(),
))]
pub async fn log(metadata: Metadata, req: Request<Body>, next: Next) -> impl IntoResponse {
    let uri = metadata.uri.path();
    if uri.contains("/heartbeat") {
        return next.run(req).await;
    }

    let start = Instant::now();
    info!("processing request");

    let res = next.run(req).await;
    let elapsed = start.elapsed();

    info!(latency = ?elapsed, "processed request");
    res
}

fn http_version(Metadata { version, .. }: &Metadata) -> &'static str {
    match *version {
        Version::HTTP_09 => "http/0.9",
        Version::HTTP_10 => "http/1.0",
        Version::HTTP_11 => "http/1.1",
        Version::HTTP_2 => "http/2.0",
        Version::HTTP_3 => "http/3.0",
        _ => unimplemented!(),
    }
}

fn get_user_agent(Metadata { headers, .. }: &Metadata) -> Option<String> {
    headers
        .get(USER_AGENT)
        .map(|f| String::from_utf8_lossy(f.as_bytes()).to_string())
}

fn display_opt<T: Display>(opt: Option<T>) -> impl Display {
    struct Helper<T: Display>(Option<T>);
    impl<T: Display> Display for Helper<T> {
        fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
            match self.0 {
                Some(ref display) => Display::fmt(display, f),
                None => f.write_str("<unknown>"),
            }
        }
    }

    Helper(opt)
}
