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

use std::net::SocketAddr;

use axum::Server;
use eyre::Result;
use hazel::{
    app::Hazel, bootstrap::bootstrap, config::Config, remi::StorageServiceDelegate, server, COMMIT_HASH, VERSION,
};
use remi_core::StorageService;
use tracing::*;

#[tokio::main]
async fn main() -> Result<()> {
    // Load up environment variables from external .env file!
    dotenv::dotenv().unwrap_or_default();

    // Load up the Hazel configuration
    Config::load()?;

    let config = Config::get();
    bootstrap(config).await?;

    info!(version = VERSION, commit = COMMIT_HASH, "starting hazel...");
    let state = Hazel {
        storage: StorageServiceDelegate::default(),
        config,
    };

    state.storage.init().await?;

    let router = server::router(state);
    let addr_string = format!("{}:{}", config.server.host, config.server.port);
    let addr = addr_string.parse::<SocketAddr>()?;

    info!(addr = addr_string, "hazel is now listening");
    Server::bind(&addr).serve(router.into_make_service()).await?;
    Ok(())
}
