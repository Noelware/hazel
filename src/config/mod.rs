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

mod from_env;
mod server;
mod storage;

use std::path::{Path, PathBuf};

use crate::logging::LoggingConfig;
use eyre::Result;
use once_cell::sync::OnceCell;

pub use from_env::*;
pub use server::*;
pub use storage::*;

/// Creates a configuration struct with a given `$name` and given properties.
/// if needed.
///
/// ## Example
/// ```no_run
/// # use authkit::config::config;
/// #
/// config!(SomeConfig {
///     pub key: String => {
///         default_value: "".into();
///         to_env: std::env::var!("SOME_ENV_KEY").unwrap();
///     };
/// });
/// ```
#[macro_export]
macro_rules! config {
    ($name:ident {
        $(
            $(#[$meta:meta])*
            $vis:vis $key:ident: $ty:ty => {
                default_value: $default:expr;
                to_env: $env:expr;
            };
        )* $(;)?
    }) => {
        #[derive(Debug, Clone, ::serde::Serialize, ::serde::Deserialize)]
        pub struct $name {
            $(
                $(#[$meta])*
                $vis $key: $ty,
            )*
        }

        impl Default for $name {
            fn default() -> Self {
                Self {
                    $(
                        $key: $default,
                    )*
                }
            }
        }

        impl $crate::config::FromEnv<$name> for $name {
            fn from_env() -> Self {
                Self {
                    $(
                        $key: $env,
                    )*
                }
            }
        }
    };
}

config!(Config {
    /// Service name to identify this Hazel instance, since there
    /// can be many Hazel deployments.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub service_name: Option<String> => {
        default_value: None;
        to_env: ::std::env::var("HAZEL_SERVICE_NAME").ok();
    };

    /// The DSN for configuring Sentry. This will enable tracing for Sentry as well,
    /// and error handling.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sentry_dsn: Option<String> => {
        default_value: None;
        to_env: ::std::env::var("HAZEL_SENTRY_DSN").ok();
    };

    #[serde(default)]
    pub logging: LoggingConfig => {
        default_value: LoggingConfig::default();
        to_env: LoggingConfig::from_env();
    };

    /// Storage configuration. Cannot be null.
    #[serde(default, with = "serde_yaml::with::singleton_map")]
    pub storage: StorageConfig => {
        default_value: StorageConfig::default();
        to_env: StorageConfig::from_env();
    };

    #[serde(default)]
    pub server: ServerConfig => {
        default_value: ServerConfig::default();
        to_env: ServerConfig::from_env();
    };
});

static CONFIG: OnceCell<Config> = OnceCell::new();

impl Config {
    fn find_config_file() -> Option<PathBuf> {
        let mut config_dir = Path::new("./config").to_path_buf();
        if config_dir.is_dir() {
            config_dir.push("hazel.yaml");

            if config_dir.exists() && config_dir.is_file() {
                return Some(config_dir.clone());
            }
        }

        let config_path = Path::new("./config.yml").to_path_buf();
        if config_path.exists() && config_path.is_file() {
            return Some(config_path);
        }

        match std::env::var("HAZEL_CONFIG_FILE") {
            Ok(path) => {
                let path = Path::new(&path);
                if path.exists() && path.is_file() {
                    return Some(path.to_path_buf());
                }

                None
            }

            Err(_) => None,
        }
    }

    /// Returns a reference to the loaded-up configuration that
    /// was previously loaded.
    ///
    /// ## Panics
    /// This will panic if the inner [`OnceCell`] was not initialized. To fix this,
    /// run the [`Config::load`] method.
    pub fn get<'a>() -> &'a Config {
        CONFIG.get().unwrap()
    }

    /// Loads up the configuration object from a path, or from the default
    /// locations if not found.
    pub fn load() -> Result<()> {
        if CONFIG.get().is_some() {
            warn!("tried to load previously-loaded configuration file");
            return Ok(());
        }

        match Config::find_config_file() {
            Some(path) => {
                let serialized: Config = serde_yaml::from_reader(std::fs::File::open(path)?)?;
                CONFIG.set(serialized).unwrap();

                Ok(())
            }

            None => {
                CONFIG.set(Config::from_env()).unwrap();
                Ok(())
            }
        }
    }
}
