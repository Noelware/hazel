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

use std::{ffi::OsStr, process::Command, time::SystemTime};

use chrono::{DateTime, Utc};

/// execute returns the standard output of the command specified.
fn execute<T: Into<String> + AsRef<OsStr>>(
    command: T,
    args: &[&str],
) -> Result<String, Box<dyn std::error::Error + 'static>> {
    let res = Command::new(command).args(args).output().unwrap();
    Ok(String::from_utf8(res.stdout)?)
}

fn main() {
    println!("cargo:rerun-if-changed=build.rs");

    // Generate the metadata variables
    let commit_hash = execute("git", &["rev-parse", "--short=8", "HEAD"]).unwrap();
    let now = SystemTime::now();
    let now_in_utc: DateTime<Utc> = now.into();

    println!("cargo:rustc-env=HAZEL_COMMIT_HASH={}", commit_hash);
    println!("cargo:rustc-env=HAZEL_BUILD_DATE={}", now_in_utc.to_rfc3339());
}
