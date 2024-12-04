# 🪶 Hazel: Easy to use read-only proxy to map objects to URLs
# Copyright 2022-2024 Noelware, LLC. <team@noelware.org>
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
{pkgs}:
with pkgs; let
  rust-toolchain = rust-bin.fromRustupToolchainFile ../rust-toolchain.toml;
in
  mkShell {
    nativeBuildInputs =
      [pkg-config]
      ++ (lib.optional stdenv.isLinux [mold lldb])
      ++ (lib.optional stdenv.isDarwin (with darwin.apple_sdk.frameworks; [
        CoreFoundation
        Security
        SystemConfiguration
      ]));

    buildInputs =
      [
        cargo-machete
        cargo-expand
        cargo-deny

        rust-toolchain
        hadolint
        openssl
        curl
        git
      ]
      ++ (lib.optional stdenv.isLinux [glibc]);

    shellHook = ''
      export RUSTFLAGS="${
        if stdenv.isLinux
        then "-C link-arg=-fuse-ld=mold -C target-cpu=native"
        else ""
      } $RUSTFLAGS"

      export LD_LIBRARY_PATH="${lib.makeLibraryPath [openssl]}"
    '';
  }
