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

use std::io::{stdout, Result, Write};

use ansi_term::Colour::RGB;
use tracing::field::Visit;

pub struct DefaultVisitor {
    writer: Box<dyn Write + Send>,
    result: Result<()>,
}

impl Default for DefaultVisitor {
    fn default() -> DefaultVisitor {
        let stdout = stdout();

        DefaultVisitor {
            writer: Box::new(stdout) as Box<dyn Write + Send>,
            result: Ok(()),
        }
    }
}

impl Visit for DefaultVisitor {
    fn record_debug(&mut self, field: &tracing::field::Field, value: &dyn std::fmt::Debug) {
        if self.result.is_err() {
            return;
        }

        if field.name().starts_with("log.") {
            return;
        }

        let gray = RGB(134, 134, 134);
        if field.name() == "message" {
            self.result = write!(self.writer, "{:?} ", value);
        } else {
            self.result = write!(
                self.writer,
                "{}{}{} ",
                gray.paint(field.name()),
                gray.paint("="),
                gray.paint(format!("{value:?}")),
            );
        }
    }
}
