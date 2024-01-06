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

use axum::Server;
use eyre::Result;
use hazel::{
    app::Hazel, config::Config, logging::HazelLayer, remi::StorageServiceDelegate, server, COMMIT_HASH, VERSION,
};
use remi_core::StorageService;
use std::{borrow::Cow, net::SocketAddr, str::FromStr};
use tokio::{select, signal};
use tracing::{metadata::LevelFilter, *};
use tracing_subscriber::prelude::*;

#[tokio::main]
async fn main() -> Result<()> {
    // Load up environment variables from external .env file!
    dotenv::dotenv().unwrap_or_default();

    // Load up the Hazel configuration
    Config::load()?;

    let config = Config::get();
    tracing_subscriber::registry()
        .with(HazelLayer::default().with_filter(LevelFilter::from_level(config.logging.level)))
        .with(config.sentry_dsn.as_ref().map(|_| sentry_tracing::layer()))
        .init();

    let sentry_guard = if let Some(dsn) = config.sentry_dsn.clone() {
        debug!("initializing Sentry");

        let service_name = match &config.service_name {
            Some(ref name) => Cow::Owned(name.to_string()),
            None => Cow::Borrowed("hazel"),
        };

        Some(sentry::init(sentry::ClientOptions {
            dsn: Some(sentry::types::Dsn::from_str(dsn.as_str())?),
            release: Some(Cow::Owned(format!("v{VERSION}+{COMMIT_HASH}"))),
            traces_sample_rate: 1.0,
            attach_stacktrace: true,
            server_name: Some(service_name),

            ..Default::default()
        }))
    } else {
        None
    };

    info!(version = VERSION, commit = COMMIT_HASH, "starting hazel...");
    let state = Hazel {
        storage: StorageServiceDelegate::default(),
        config,
    };

    state.storage.init().await?;

    let router = server::router(state);
    let addr_string = format!("{}:{}", config.server.host, config.server.port);
    let addr = addr_string.parse::<SocketAddr>()?;

    info!(%addr, "hazel is now listening on");
    Server::bind(&addr)
        .serve(router.into_make_service())
        .with_graceful_shutdown(shutdown_signal())
        .await?;

    if let Some(guard) = sentry_guard {
        drop(guard);
    }

    Ok(())
}

async fn shutdown_signal() {
    let ctrl_c = async {
        signal::ctrl_c().await.expect("unable to install Ctrl+C handler");
    };

    #[cfg(unix)]
    let terminate = async {
        signal::unix::signal(signal::unix::SignalKind::terminate())
            .expect("unable to install signal handler")
            .recv()
            .await;
    };

    #[cfg(not(unix))]
    let terminate = std::future::pending::<()>();

    select! {
        _ = ctrl_c => {}
        _ = terminate => {}
    }

    warn!("received Ctrl+C or termination signal!");
}
