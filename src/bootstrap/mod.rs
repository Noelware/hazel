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

use eyre::Result;
use once_cell::sync::Lazy;
use std::fmt::Debug;

use self::{logging::SetupLoggingPhase, sentry::SentryPhase};
use crate::config::Config;

mod logging;
mod sentry;

static PHASES: Lazy<Vec<Box<dyn BootstrapPhase + 'static>>> =
    Lazy::new(|| vec![Box::new(SetupLoggingPhase), Box::new(SentryPhase)]);

/// Represents a phase that the AuthKit server uses to bootstrap logging
/// and Sentry, or other things if wished.
///
/// ## Order
/// SetupLoggingPhase -> SentryPhase
#[async_trait]
pub trait BootstrapPhase: Debug + Send + Sync {
    /// Bootstraps this given phase, based off the chronological order
    /// on how Hazel will use this bootstrap phase.
    async fn bootstrap(&self, config: &Config) -> Result<()>;

    // We can't implement Clone into BootstrapPhase, so we will have to do this
    // "try_clone" method to do so.
    fn try_clone(&self) -> Result<Box<dyn BootstrapPhase>>;
}

impl Clone for Box<dyn BootstrapPhase> {
    fn clone(&self) -> Box<dyn BootstrapPhase> {
        self.try_clone().expect("Unable to clone this bootstrap phase.")
    }
}

pub async fn bootstrap(config: &Config) -> Result<()> {
    for phase in PHASES.clone().into_iter() {
        debug!("bootstrapping {:?}", phase);
        phase.bootstrap(config).await?;
    }

    Ok(())
}
