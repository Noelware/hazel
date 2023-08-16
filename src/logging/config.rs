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

use crate::{config, BOOL_REGEX};
use tracing::Level;

config!(LoggingConfig {
    #[serde(with = "serde")]
    pub level: Level => {
        default_value: Level::INFO;
        to_env: ::std::env::var("HAZEL_LOG_LEVEL").map(|val| match val.as_str() {
            "trace" => Level::TRACE,
            "debug" => Level::DEBUG,
            "info" => Level::INFO,
            "warn" => Level::WARN,
            _ => Level::INFO,
        }).unwrap_or(Level::INFO);
    };

    #[serde(default)]
    pub json: bool => {
        default_value: false;
        to_env: ::std::env::var("HAZEL_LOGGING_JSON").map(|val| BOOL_REGEX.is_match(val.as_ref())).unwrap_or(false);
    };
});

mod serde {
    use serde::*;
    use tracing::Level;

    pub fn serialize<S: Serializer>(filter: &Level, serializer: S) -> Result<S::Ok, S::Error> {
        serializer.serialize_str(match *filter {
            Level::TRACE => "trace",
            Level::DEBUG => "debug",
            Level::INFO => "info",
            Level::WARN => "warn",
            _ => unreachable!(), // We shouldn't be able to hit here
        })
    }

    pub fn deserialize<'de, D: Deserializer<'de>>(deserializer: D) -> Result<Level, D::Error> {
        match String::deserialize(deserializer)?.as_str() {
            "trace" => Ok(Level::TRACE),
            "debug" => Ok(Level::DEBUG),
            "info" => Ok(Level::INFO),
            "warn" => Ok(Level::WARN),
            _ => Ok(Level::INFO),
        }
    }
}
