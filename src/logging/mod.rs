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

mod config;
mod default_visitor;
mod json_visitor;

pub use config::*;

use std::{
    collections::BTreeMap,
    io::{self, Write},
    process, thread,
};

use self::{default_visitor::DefaultVisitor, json_visitor::JsonVisitor};
use crate::config::Config;
use ansi_term::Colour::RGB;
use chrono::Local;
use serde::{Deserialize, Serialize};
use serde_json::{json, to_string, Value};
use tracing::{span, Event, Level};
use tracing_log::NormalizeEvent;
use tracing_subscriber::{layer::Context, registry::LookupSpan, Layer};

/// For the default visitor, we will need to figure out which writer
/// we will need to write to. We will only write to stdout or
/// stderr.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename = "snake_case")]
pub enum WriteType {
    /// Uses the standard error for writing logs to.
    Stderr,

    /// Uses the standard output for writing logs to.
    #[default]
    Stdout,
}

#[derive(Debug, Clone)]
pub struct HazelLayer {
    config: &'static Config,
}

impl Default for HazelLayer {
    fn default() -> Self {
        Self { config: Config::get() }
    }
}

impl<S> Layer<S> for HazelLayer
where
    S: tracing::Subscriber,
    S: for<'l> LookupSpan<'l>,
{
    fn on_new_span(&self, attrs: &span::Attributes<'_>, id: &span::Id, ctx: Context<'_, S>) {
        if self.config.logging.json {
            let span = ctx.span(id).unwrap();
            let mut data = BTreeMap::new();
            let mut visitor = JsonVisitor(&mut data);
            attrs.record(&mut visitor);

            let storage = JsonStorage(data);
            let mut exts = span.extensions_mut();
            exts.insert(storage);
        }
    }

    fn on_record(&self, span: &span::Id, values: &span::Record<'_>, ctx: Context<'_, S>) {
        if self.config.logging.json {
            let span = ctx.span(span).unwrap();
            let mut exts = span.extensions_mut();

            let storage: &mut JsonStorage = exts.get_mut::<JsonStorage>().unwrap();
            let mut visitor = JsonVisitor(&mut storage.0);

            values.record(&mut visitor);
        }
    }

    fn on_event(&self, event: &Event<'_>, ctx: Context<'_, S>) {
        let pid = process::id();
        let thread = thread::current();
        let thread_name = thread.name().unwrap_or("main");
        let metadata = event.normalized_metadata();
        let metadata = metadata.as_ref().unwrap_or_else(|| event.metadata());

        if self.config.logging.json {
            let mut spans = vec![];
            if let Some(scope) = ctx.event_scope(event) {
                for span in scope.from_root() {
                    let ext = span.extensions();
                    let storage = ext.get::<JsonStorage>().unwrap();
                    let data = &storage.0;

                    spans.push(json!({
                        "target": span.metadata().target(),
                        "level": span.metadata().level().as_str().to_lowercase(),
                        "name": span.metadata().name(),
                        "fields": data,
                        "meta": json!({
                            "module": span.metadata().module_path(),
                            "file": span.metadata().file(),
                            "line": span.metadata().line()
                        })
                    }));
                }
            }

            let mut data = BTreeMap::new();
            let mut visitor = JsonVisitor(&mut data);
            event.record(&mut visitor);

            let default_message = &Value::String("none provided".to_owned());
            let message = data.get("message").unwrap_or(default_message);
            let fields = {
                let mut d = data.clone();
                d.remove_entry("message");

                d
            };

            println!(
                "{}",
                to_string(&json!({
                    "target": metadata.target(),
                    "level": metadata.level().as_str().to_lowercase(),
                    "message": message,
                    "fields": fields,
                    "spans": spans,
                    "meta": json!({
                        "module": metadata.module_path(),
                        "file": metadata.file(),
                        "line": metadata.line(),
                    }),
                    "process": json!({
                        "pid": pid
                    }),
                    "thread": json!({
                        "name": thread_name
                    })
                }))
                .unwrap()
            );
        } else {
            let mut stdout = io::stdout();
            let time = RGB(134, 134, 134).paint(format!("{}", Local::now().format("[%B %d, %G - %H:%M:%S %p]")));
            let target = RGB(120, 231, 255).paint(format!("{:<25}", metadata.module_path().unwrap_or("unknown")));

            let thread_name_color = RGB(255, 105, 189).paint(thread_name);
            let (b1, b2) = (RGB(134, 134, 134).paint("["), RGB(134, 134, 134).paint("]"));
            let (c1, c2) = (RGB(134, 134, 134).paint("<"), RGB(134, 134, 134).paint(">"));
            let level_color = match *metadata.level() {
                Level::DEBUG => RGB(163, 182, 138).bold(),
                Level::TRACE => RGB(163, 182, 138).bold(),
                Level::ERROR => RGB(153, 75, 104).bold(),
                Level::WARN => RGB(243, 243, 134).bold(),
                Level::INFO => RGB(178, 157, 243).bold(),
            };

            let _ = write!(
                stdout,
                "{time} {level} {b1}{target} {c1}{thread_name_color}{c2}{b2} :: ",
                level = level_color.paint(format!("{:<5}", metadata.level().as_str()))
            );

            let mut visitor = DefaultVisitor::default();
            event.record(&mut visitor);

            let _ = writeln!(stdout);
        }
    }
}

pub(crate) struct JsonStorage(pub BTreeMap<String, Value>);
