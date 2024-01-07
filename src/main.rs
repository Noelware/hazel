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

use eyre::{Context, Result};
use hazel::{
    app::Hazel, config::Config, logging::HazelLayer, remi::StorageServiceDelegate, server, COMMIT_HASH, VERSION,
};
use remi_core::StorageService;
use sentry::types::Dsn;
use std::{borrow::Cow, str::FromStr};
use tokio::{net::TcpListener, select, signal};
use tracing::{metadata::LevelFilter, *};
use tracing_subscriber::prelude::*;

#[tokio::main]
async fn main() -> Result<()> {
    color_eyre::install()?;
    dotenv::dotenv().unwrap_or_default();
    Config::load()?;

    let config = Config::get();
    tracing_subscriber::registry()
        .with(HazelLayer::default().with_filter(LevelFilter::from_level(config.logging.level)))
        .with(config.sentry_dsn.as_ref().map(|_| sentry_tracing::layer()))
        .init();

    let service_name = match config.service_name {
        Some(ref name) => Cow::Owned(name.to_owned()),
        None => Cow::Borrowed("hazel"),
    };

    let _sentry_guard = sentry::init(sentry::ClientOptions {
        traces_sample_rate: 1.0,
        attach_stacktrace: true,
        server_name: Some(service_name),
        release: Some(Cow::Owned(format!("v{VERSION}+{COMMIT_HASH}"))),
        dsn: config
            .sentry_dsn
            .as_ref()
            .map(|x| Dsn::from_str(x).unwrap_or_else(|e| panic!("unable to parse dsn [{x}]: {e}"))),

        ..Default::default()
    });

    info!(
        version = VERSION,
        commit.hash = COMMIT_HASH,
        "starting Hazel application..."
    );

    let state = Hazel {
        storage: StorageServiceDelegate::default(),
        config,
    };

    state.storage.init().await?;
    let router = server::router(state);
    let listener = TcpListener::bind(format!("{}:{}", config.server.host, config.server.port))
        .await
        .context(format!(
            "unable to create TCP listener with params [{}:{}]",
            config.server.host, config.server.port
        ))?;

    info!(
        address = format!("{}:{}", config.server.host, config.server.port),
        "bind HTTP service to"
    );

    axum::serve(listener, router)
        .with_graceful_shutdown(shutdown_signal())
        .await
        .context("unable to run Hazel HTTP service")
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
