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

pub mod logging;
pub mod opentelemetry;
pub mod server;
pub mod storage;
pub(in crate::config) mod util;

use azalia::config::{
    env::{self, TryFromEnv},
    merge::Merge,
};
use eyre::Report;
use sentry::types::Dsn;
use serde::{Deserialize, Serialize};
use std::{fs, path::PathBuf};

#[derive(Debug, Clone, Serialize, Deserialize, Merge)]
pub struct Config {
    /// Name of this Hazel instance that the content is being directed from.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub server_name: Option<String>,

    /// Data Source Name (DSN) to connect to a [Sentry](https://sentry.io) service, either
    /// from the SaaS product or from [`getsentry/self-hosted`].
    ///
    /// [`getsentry/self-hosted`]: https://github.com/getsentry/self-hosted
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub sentry_dsn: Option<Dsn>,

    /// Configuration for configuring the default logging mechanism.
    #[serde(default)]
    pub logging: logging::Config,

    /// Represents the configuration for configuring the data storage where Hazel
    /// grabs all the objects to be exposed.
    #[serde(default)]
    pub storage: storage::Config,

    /// Configures the HTTP server's host and port bindings and SSL.
    #[serde(default)]
    pub server: server::Config,
}

pub const SERVER_NAME: &str = "HAZEL_SERVER_NAME";
pub const SENTRY_DSN: &str = "HAZEL_SENTRY_DSN";

impl TryFromEnv for Config {
    type Error = Report;

    fn try_from_env() -> Result<Self, Self::Error> {
        Ok(Config {
            server_name: env::try_parse_optional(SERVER_NAME)?,
            sentry_dsn: env::try_parse_optional::<_, sentry::types::Dsn>(SENTRY_DSN)?,
            logging: logging::Config::try_from_env()?,
            storage: storage::Config::try_from_env()?,
            server: server::Config::try_from_env()?,
        })
    }
}

impl Config {
    fn find_default_location() -> Option<PathBuf> {
        if PathBuf::from("./config/hazel.toml").exists() {
            return Some(PathBuf::from("./config/hazel.toml"));
        }

        if PathBuf::from("./config.toml").exists() {
            return Some(PathBuf::from("./config.toml"));
        }

        std::env::var("HAZEL_CONFIG_FILE")
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
        let file = toml::from_str(fs::read_to_string(path)?.as_str())?;
        config.merge(file);

        Ok(config)
    }
}

macro_rules! impl_enum_based_env_value {
    ($env:expr, {
        on match fail: |$input:ident| $error:literal$( [$($arg:expr),*])?;

        $($field:pat => $value:expr;)*
    }) => {
        match ::azalia::config::env::try_parse_or_else::<_, ::std::string::String>($env, ::core::default::Default::default()) {
            Ok($input) => match &*$input.to_ascii_lowercase() {
                $($field => $value,)*

                _ => ::eyre::bail!($error$(, $($arg),*)?)
            }

            Err(::azalia::config::env::TryParseError::System(::std::env::VarError::NotUnicode(_))) => {
                ::eyre::bail!("environment variable `${}` couldn't be loaded due to invalid unicode data", $env)
            }

            Err(e) => Err(::core::convert::Into::into(e)),
        }
    };
}

pub(crate) use impl_enum_based_env_value;
