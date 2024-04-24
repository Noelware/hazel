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

#[macro_use]
extern crate tracing;

#[macro_use]
extern crate eyre;

pub mod config;
pub mod server;

/// Generic [`Regex`] implementation for possible truthy boolean values.
pub static TRUTHY_REGEX: once_cell::sync::Lazy<regex::Regex> =
    once_cell::sync::Lazy::new(|| regex::Regex::new(r#"^(yes|true|si*|e|enable|1)$"#).unwrap());

/// Constant that refers to the version of the Rust compiler that was used. This is mainly
/// for diagnostics and is never accessed by third parties.
pub const RUSTC: &str = env!("HAZEL_RUSTC_VERSION");

/// Constant in the format of [RFC3339] date format that refers to when `hazel` was last built
///
/// [RFC3339]: https://www.rfc-editor.org/rfc/rfc3339
pub const BUILD_DATE: &str = env!("HAZEL_BUILD_DATE");

/// Constant that refers to the Git commit hash from the [canonical repository]
///
/// [canonical repository]: https://github.com/Noelware/hazel
pub const COMMIT_HASH: &str = env!("HAZEL_COMMIT_HASH");

/// Constant that refers to the version of the `hazel` software
pub const VERSION: &str = env!("CARGO_PKG_VERSION");

/// Returns a formatted version of `v2.0.0+d1cebae` or `v2.0.0` if no commit hash
/// was found.
///
/// This will return a immutable string slice as the version, and since it could possibly
/// be mutated, we advise to only use it in immutable contexts; never try to mutate it.
#[inline(always)]
#[allow(unknown_lints, static_mut_refs)]
pub fn version() -> &'static str {
    static ONCE: std::sync::Once = std::sync::Once::new();
    static mut VERSION: String = String::new();

    // Safety: `VERSION` is only mutated on the first call of `version` and is never
    //         mutated again afterwards.
    unsafe {
        ONCE.call_once(move || {
            use std::fmt::Write;

            let mut buf = String::new();
            write!(buf, "{}", crate::VERSION).unwrap();

            if crate::COMMIT_HASH != "d1cebae" {
                write!(buf, "+{}", crate::COMMIT_HASH).unwrap();
            }

            VERSION = buf;
        });

        &VERSION
    }
}
