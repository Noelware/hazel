# 🪶 Hazel: Easy to use read-only proxy to map objects to URLs
# Copyright 2022-2025 Noelware, LLC. <team@noelware.org>
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
{
  pkg-config,
  openssl,
  stdenv,
  lib,
  makeRustPlatform,
  darwin,
  rust-bin,
}: let
  toolchain = rust-bin.fromRustupToolchainFile ../rust-toolchain.toml;
  rustPlatform = makeRustPlatform {
    rustc = toolchain;
    cargo = toolchain;
  };
in
  rustPlatform.buildRustPackage rec {
    version = "2.0.0";
    pname = "hazel";

    src = ../.;

    nativeBuildInputs = [pkg-config];
    buildInputs =
      [openssl]
      ++ (lib.optional stdenv.isDarwin (with darwin.apple_sdk.frameworks; [
        CoreFoundation
        SystemConfiguration
      ]));

    cargoLock.lockFile = ../Cargo.lock;

    meta = with lib; {
      description = "Easy to use read-only proxy to map storage objects as URLs";
      homepage = "https://noelware.org/oss/hazel";
      license = with licenses; [asl20];
      maintainers = with maintainers; [auguwu];
      mainProgram = "hazel";
      changelog = "https://github.com/Noelware/hazel/releases/v${version}";
    };
  }
