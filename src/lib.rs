// 🪶 hazel: Minimal, and easy HTTP proxy to map storage provider items into HTTP endpoints
// Copyright 2022-2023 Noelware, LLC. <team@noelware.org>
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

use regex::Regex;

#[macro_use]
extern crate tracing;

#[macro_use]
extern crate lazy_static;

#[macro_use]
extern crate eyre;

pub const VERSION: &str = env!("CARGO_PKG_VERSION");
pub const COMMIT_HASH: &str = env!("HAZEL_COMMIT_HASH");
pub const BUILD_DATE: &str = env!("HAZEL_BUILD_DATE");

pub mod app;
pub mod config;
pub mod logging;
pub mod remi;
pub mod server;

lazy_static! {
    pub static ref BOOL_REGEX: Regex = Regex::new(r#"^(yes|true|si*|1|enable|enabled)$"#).unwrap();
}
