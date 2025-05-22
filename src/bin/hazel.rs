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

use azalia::{
    log::{WriteLayer, writers},
    remi::StorageService,
};
use hazel::{
    config::{Config, storage},
    server,
};
use mimalloc::MiMalloc;
use sentry::ClientOptions;
use std::{
    borrow::Cow,
    cmp, io,
    sync::atomic::{AtomicUsize, Ordering},
};
use tracing::{info, level_filters::LevelFilter};
use tracing_subscriber::prelude::*;

#[global_allocator]
static GLOBAL: MiMalloc = MiMalloc;

fn main() -> eyre::Result<()> {
    dotenvy::dotenv().unwrap_or_default();

    let workers = cmp::max(
        num_cpus::get(),
        azalia::config::env::try_parse_or("HAZEL_WORKER_THREADS", num_cpus::get)?,
    );

    let rt = tokio::runtime::Builder::new_multi_thread()
        .worker_threads(workers)
        .thread_name_fn(|| {
            static ID: AtomicUsize = AtomicUsize::new(0);
            let id = ID.fetch_add(1, Ordering::SeqCst);

            format!("hazel-worker-pool[#{id}]")
        })
        .enable_all()
        .build()?;

    rt.block_on(real_main())
}

async fn real_main() -> eyre::Result<()> {
    let config = Config::new()?;
    color_eyre::install()?;

    let _guard = sentry::init(ClientOptions {
        traces_sample_rate: 0.5,
        attach_stacktrace: true,
        server_name: Some(
            config
                .server_name
                .clone()
                .map(Cow::Owned)
                .unwrap_or_else(|| Cow::Borrowed("hazel")),
        ),

        dsn: config.sentry_dsn.clone(),

        ..Default::default()
    });

    tracing_subscriber::registry()
        .with(
            match config.logging.json {
                false => WriteLayer::new_with(io::stdout(), writers::default::Writer::default()),
                true => WriteLayer::new_with(io::stdout(), writers::json),
            }
            .with_filter(LevelFilter::from_level(config.logging.level))
            .with_filter(tracing_subscriber::filter::filter_fn(|meta| {
                // disallow from getting logs from `tokio` since it doesn't contain anything
                // useful to us
                !meta.target().starts_with("tokio::")
            })),
        )
        .with(sentry_tracing::layer())
        .with(tracing_error::ErrorLayer::default())
        .init();

    info!(
        "init Hazel :: {} (built @ {}) with Rust {}",
        hazel::version(),
        hazel::BUILD_TIMESTAMP,
        hazel::RUSTC
    );

    let storage = match config.storage.clone() {
        storage::Config::Filesystem(fs) => {
            StorageService::Filesystem(azalia::remi::fs::StorageService::with_config(fs))
        }

        storage::Config::Azure(azure) => StorageService::Azure(azalia::remi::azure::StorageService::new(azure)?),
        storage::Config::S3(s3) => StorageService::S3(azalia::remi::s3::StorageService::new(s3)),
    };

    <StorageService as azalia::remi::core::StorageService>::init(&storage).await?;
    info!("data storage has been initialized");

    server::start(storage, config).await
}
