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

use hazel::config::{storage, Config};
use noelware_config::env;
use noelware_log::{writers, WriteLayer};
use noelware_remi::StorageService;
use owo_colors::{OwoColorize, Stream};
use remi::StorageService as _;
use sentry::{types::Dsn, ClientOptions};
use std::{
    borrow::Cow,
    cmp, io,
    str::FromStr,
    sync::atomic::{AtomicUsize, Ordering},
};
use tokio::runtime::Builder;
use tracing::{info, level_filters::LevelFilter, trace};
use tracing_subscriber::{layer::SubscriberExt, prelude::*, Layer};

#[global_allocator]
static GLOBAL: mimalloc::MiMalloc = mimalloc::MiMalloc;

fn main() -> eyre::Result<()> {
    let _ = dotenvy::dotenv();
    color_eyre::install()?;

    let workers = cmp::max(
        num_cpus::get(),
        match env!("HAZEL_WORKER_THREADS") {
            Ok(val) => val.parse()?,
            Err(std::env::VarError::NotPresent) => num_cpus::get(),
            Err(e) => return Err(e.into()),
        },
    );

    let rt = Builder::new_multi_thread()
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
    println!(
        "» Booting up {} v{}, compiled on Rust {}",
        "hazel".if_supports_color(Stream::Stderr, |x| x.bold()),
        hazel::version().if_supports_color(Stream::Stderr, |x| x.bold()),
        hazel::RUSTC.if_supports_color(Stream::Stderr, |x| x.bold())
    );

    let config = Config::new()?;
    let _sentry_guard = sentry::init(ClientOptions {
        traces_sample_rate: 0.5,
        attach_stacktrace: true,
        server_name: Some(
            config
                .server_name
                .clone()
                .map(Cow::Owned)
                .unwrap_or_else(|| Cow::Borrowed("hazel")),
        ),

        dsn: config
            .sentry_dsn
            .as_ref()
            .map(|x| Dsn::from_str(x).expect("to have a valid Sentry DSN")),

        ..Default::default()
    });

    tracing_subscriber::registry()
        .with(
            match config.logging.json {
                false => WriteLayer::new_with(io::stdout(), writers::default),
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

    info!("Bootstrapped Hazel system successfully");
    trace!("determining data storage to use...");

    let storage = match config.storage.clone() {
        storage::Config::Filesystem(fs) => StorageService::Filesystem(remi_fs::StorageService::with_config(fs)),
        storage::Config::Azure(azure) => StorageService::Azure(remi_azure::StorageService::new(azure)),
        storage::Config::S3(s3) => StorageService::S3(remi_s3::StorageService::new(s3)),
    };

    storage.init().await?;
    info!("Initialized data storage successfully!");

    hazel::server::start(storage, config).await
}
