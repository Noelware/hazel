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

pub mod logging;
pub mod storage;
pub mod tracing;

use std::{fs::File, path::PathBuf};

use eyre::Report;
use noelware_config::{env, merge::Merge, TryFromEnv};
use serde::{Deserialize, Serialize};

use crate::server;

#[derive(Debug, Clone, Serialize, Deserialize, Merge)]
pub struct Config {
    /// Server name that helps identify which Hazel instance this is.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub server_name: Option<String>,

    /// Data Source Name (DSN) to connect to a [Sentry](https://sentry.io) service, either
    /// from the SaaS product or from [`getsentry/self-hosted`].
    ///
    /// [`getsentry/self-hosted`]: https://github.com/getsentry/self-hosted
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub sentry_dsn: Option<String>,

    /// Configuration for configuring the default logging mechanism.
    #[serde(default, serialize_with = "hcl::ser::block")]
    pub logging: logging::Config,

    /// Represents the configuration for configuring the data storage where Hazel
    /// grabs all the objects to be exposed.
    #[serde(default, serialize_with = "hcl::ser::labeled_block")]
    pub storage: storage::Config,

    /// Configures the HTTP server's host and port bindings and SSL.
    #[serde(default, serialize_with = "hcl::ser::block")]
    pub server: server::config::Config,
}

impl TryFromEnv for Config {
    type Output = Config;
    type Error = Report;

    fn try_from_env() -> Result<Self::Output, Self::Error> {
        Ok(Config {
            server_name: env!("HAZEL_SERVER_NAME", optional),
            sentry_dsn: env!("HAZEL_SENTRY_DSN", optional),
            logging: logging::Config::try_from_env()?,
            storage: storage::Config::try_from_env()?,
            server: server::config::Config::try_from_env()?,
        })
    }
}

impl Config {
    fn find_default_location() -> Option<PathBuf> {
        if PathBuf::from("./config/hazel.hzl").exists() {
            return Some(PathBuf::from("./config/hazel.hzl"));
        }

        env!("HAZEL_CONFIG_FILE")
            .map(|path| path.parse::<PathBuf>().unwrap())
            .map(|path| {
                if path.exists() && path.is_file() {
                    return Some(path);
                }

                None
            })
            .ok()?
    }

    pub fn new() -> eyre::Result<Config> {
        let Some(path) = Config::find_default_location() else {
            return Config::try_from_env();
        };

        if !path.try_exists()? {
            eprintln!(
                "[hazel WARN] given file [{}] doesn't exist, using system env variables as configuration source",
                path.display()
            );

            return Config::try_from_env();
        }

        let mut config = Config::try_from_env()?;
        let file = hcl::from_reader::<Config, _>(File::open(path)?)?;
        config.merge(file);

        Ok(config)
    }
}
