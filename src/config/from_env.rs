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

use eyre::Result;

/// Trait to implement a "from_env() -> T" function to load
/// the object from the system environment variables.
pub trait FromEnv<T> {
    fn from_env() -> T;
}

/// Trait that implements a "try_from_env() -> Result<T>" to load up
/// a object from the system environment variables which return a Result
/// variant.
pub trait TryFromEnv<T> {
    fn try_from_env() -> Result<T>;
}
