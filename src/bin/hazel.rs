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

use std::{
    cmp,
    sync::atomic::{AtomicUsize, Ordering},
};

use noelware_config::env;
use owo_colors::{OwoColorize, Stream};
use tokio::runtime::Builder;

#[global_allocator]
static GLOBAL: mimalloc::MiMalloc = mimalloc::MiMalloc;

fn main() -> eyre::Result<()> {
    let _ = dotenvy::dotenv();
    color_eyre::install()?;

    let workers = cmp::max(
        num_cpus::get(),
        env!("HAZEL_WORKER_THREADS")
            .map(|x| x.parse())?
            .unwrap_or(num_cpus::get()),
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

    Ok(())
}
