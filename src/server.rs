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

use crate::config::{Config, server::ssl};
use axum::Router;
use axum_server::{Address, Handle, tls_rustls::RustlsConfig};
use azalia::remi::StorageService;
use eyre::Context;
use std::{net::SocketAddr, time::Duration};

mod middlewares;
mod routes;

pub async fn start(storage: StorageService, config: Config) -> eyre::Result<()> {
    info!("starting HTTP server!");

    let router = routes::create_router(storage, config.clone());
    match config.server.ssl {
        Some(ref ssl) => start_https_server(&config, ssl, router).await,
        None => start_http_server(&config, router).await,
    }
}

async fn start_https_server(config: &Config, ssl: &ssl::Config, router: Router) -> eyre::Result<()> {
    let handle = Handle::new();
    tokio::spawn(shutdown_signal(Some(handle.clone())));

    let addr = config.server.to_socket_addr();
    let config = RustlsConfig::from_pem_file(&ssl.cert, &ssl.cert_key).await?;

    info!(address = %addr, "listening on HTTPS");
    axum_server::bind_rustls(addr, config)
        .handle(handle)
        .serve(router.into_make_service())
        .await
        .context("failed to run HTTPS server")
}

async fn start_http_server(config: &Config, router: Router) -> eyre::Result<()> {
    let addr = config.server.to_socket_addr();
    let listener = tokio::net::TcpListener::bind(addr).await?;
    info!(address = ?addr, "listening on HTTP");

    axum::serve(listener, router.into_make_service())
        .with_graceful_shutdown(shutdown_signal::<SocketAddr>(None))
        .await
        .context("failed to run HTTP server")
}

async fn shutdown_signal<A>(handle: Option<Handle<A>>)
where
    A: Address,
{
    let ctrl_c = async {
        tokio::signal::ctrl_c().await.expect("unable to install CTRL+C handler");
    };

    #[cfg(unix)]
    let terminate = async {
        tokio::signal::unix::signal(tokio::signal::unix::SignalKind::terminate())
            .expect("unable to install signal handler")
            .recv()
            .await;
    };

    #[cfg(not(unix))]
    let terminate = std::future::pending::<()>();

    tokio::select! {
        _ = ctrl_c => {}
        _ = terminate => {}
    }

    warn!("received terminal signal! shutting down");
    if let Some(handle) = handle {
        handle.graceful_shutdown(Some(Duration::from_secs(10)));
    }
}
