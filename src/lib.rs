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

pub mod config;
pub mod server;

#[macro_use]
extern crate tracing;

#[macro_use]
extern crate eyre;

use std::sync::OnceLock;

/// Returns the `rustc` version that Hazel was compiled on. Mainly used for diagnostics.
pub const RUSTC: &str = env!("HAZEL_RUSTC_VERSION");

/// Timestamp in the [RFC3339] format of when this `hazel` binary was built at.
///
/// [RFC3339]: https://www.rfc-editor.org/rfc/rfc3339
pub const BUILD_TIMESTAMP: &str = env!("HAZEL_BUILD_TIMESTAMP");

/// Constant that refers to the Git commit hash from the [canonical repository]
///
/// [canonical repository]: https://github.com/Noelware/hazel
pub const COMMIT_HASH: Option<&str> = option_env!("HAZEL_GIT_COMMIT");

/// Constant that refers to the version of the `hazel` software
pub const VERSION: &str = env!("CARGO_PKG_VERSION");

#[inline]
pub fn version() -> &'static str {
    static ONCE: OnceLock<String> = OnceLock::new();
    ONCE.get_or_init(|| {
        use std::fmt::Write;

        let mut buf = String::new();
        write!(buf, "{}", VERSION).unwrap();

        if let Some(hash) = COMMIT_HASH {
            write!(buf, "+{}", hash).unwrap();
        }

        buf
    })
}
