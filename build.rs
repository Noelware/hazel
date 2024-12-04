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

use chrono::{DateTime, Utc};
use std::{process::Command, time::SystemTime};
use which::which;

fn main() {
    // Rerun the build script if the script ever changes at all.
    println!("cargo::rerun-if-changed=build.rs");

    // Collect the current `rustc` version so we can use it for diagnostics. Since,
    // other distributors can use different versions than what we use (`rust-toolchain.toml`).
    let rustver = rustc_version::version().unwrap().to_string();
    println!("cargo::rustc-env=HAZEL_RUSTC_VERSION={rustver}");

    // Collect the build timestamp
    let build_timestamp = Into::<DateTime<Utc>>::into(SystemTime::now()).to_rfc3339();
    println!("cargo::rustc-env=HAZEL_BUILD_TIMESTAMP={build_timestamp}");

    // Detect the Git commit where this seoul build is coming from. Other distributors
    // might not want to pull Seoul via Git, so this can be omitted.
    match which("git") {
        Ok(git) => {
            let mut cmd = Command::new(git);
            cmd.args(["rev-parse", "--short=8", "HEAD"]);

            let output = cmd.output().expect("to succeed");
            let stdout = String::from_utf8_lossy(&output.stdout);
            if !stdout.is_empty() {
                let hash = stdout.trim();
                println!("cargo::rustc-env=HAZEL_GIT_COMMIT={hash}");
            } else {
                println!("cargo::warning=empty commit hash, empty git dir maybe?");
            }
        }

        Err(which::Error::CannotFindBinaryPath) => {
            println!("cargo::warning=`git` binary missing, therefore cannot get commit hash")
        }

        Err(err) => panic!("unable to get `git` binary from system `$PATH`: {err}"),
    }
}
