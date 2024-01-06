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

use crate::config;

config!(ServerConfig {
    #[serde(default = "host")]
    pub host: String => {
        default_value: host();
        to_env: ::std::env::var("HAZEL_SERVER_HOST").unwrap_or(host());
    };

    #[serde(default = "port")]
    pub port: i16 => {
        default_value: port();
        to_env: ::std::env::var("HAZEL_SERVER_PORT")
            .map(|f| f.parse::<i16>().unwrap_or_else(|_| panic!("unable to parse {f} as i16.")))
            .unwrap_or(port());
    };
});

fn host() -> String {
    "0.0.0.0".into()
}

fn port() -> i16 {
    3939
}
