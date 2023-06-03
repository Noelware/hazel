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

use std::{borrow::Cow, str::FromStr};

use eyre::Result;
use sentry::types::Dsn;

use super::BootstrapPhase;
use crate::config::Config;

#[derive(Debug, Clone)]
pub struct SentryPhase;

#[async_trait]
impl BootstrapPhase for SentryPhase {
    async fn bootstrap(&self, config: &Config) -> Result<()> {
        if let Some(dsn) = &config.sentry_dsn {
            debug!(%dsn, phase = "bootstrap.sentry", "initializing Sentry with DSN");

            let service_name = match &config.service_name {
                Some(ref name) => Cow::Owned(name.to_string()),
                None => Cow::Borrowed("hazel"),
            };

            let _ = sentry::init(sentry::ClientOptions {
                dsn: Some(Dsn::from_str(dsn.as_str())?),
                release: Some(Cow::Borrowed(
                    "hazel v0.0.0+ff0babe (+https://github.com/Noelware/hazel)",
                )),
                traces_sample_rate: 1.0,
                enable_profiling: true,
                attach_stacktrace: true,
                server_name: Some(service_name),
                integrations: vec![],

                ..Default::default()
            });
        }

        Ok(())
    }

    fn try_clone(&self) -> Result<Box<dyn BootstrapPhase>> {
        Ok(Box::new(self.clone()))
    }
}
