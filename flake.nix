# 🪶 hazel: Minimal, and easy HTTP proxy to map storage provider items into HTTP endpoints
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
{
  description = "🪶 Minimal, and easy HTTP proxy to map storage provider items into HTTP endpoints";
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
    flake-utils.url = "github:numtide/flake-utils";
    rust-overlay = {
      url = "github:oxalica/rust-overlay";
      inputs = {
        nixpkgs.follows = "nixpkgs";
        flake-utils.follows = "flake-utils";
      };
    };

    crane = {
      url = "github:ipetkov/crane";
      inputs = {
        nixpkgs.follows = "nixpkgs";
      };
    };
  };

  outputs = {
    self,
    nixpkgs,
    flake-utils,
    rust-overlay,
    crane,
  }:
    flake-utils.lib.eachDefaultSystem (system: let
      pkgs = import nixpkgs {
        inherit system;
        overlays = [(import rust-overlay)];
      };

      stdenv =
        if pkgs.stdenv.isLinux
        then pkgs.stdenv
        else pkgs.clangStdenv;

      rust = pkgs.rust-bin.fromRustupToolchainFile ./rust-toolchain.toml;
      craneLib = crane.lib.${system};
      commonArgs = {
        src = craneLib.cleanCargoSource (craneLib.path ./.);
        buildInputs = with pkgs; [
          openssl
        ];

        nativeBuildInputs = with pkgs; [
          pkg-config
        ];
      };

      rustflags =
        if stdenv.isLinux
        then ''-C link-arg=-fuse-ld=mold -C target-cpu=native $RUSTFLAGS''
        else "$RUSTFLAGS";

      # builds only the dependencies
      artifacts = craneLib.buildDepsOnly (commonArgs
        // {
          pname = "hazel-deps";
        });

      # runs `cargo clippy`
      clippy = craneLib.cargoClippy (commonArgs
        // {
          inherit artifacts;

          pname = "hazel-clippy";
        });

      # build the ume cli and server
      hazel = craneLib.buildPackage (commonArgs
        // {
          inherit artifacts;
        });
    in {
      packages.default = hazel;
      checks = {
        # checks for `nix flake check`
        inherit hazel clippy;
      };

      devShells.default = pkgs.mkShell {
        nativeBuildInputs = with pkgs;
          [pkg-config]
          ++ (lib.optional stdenv.isLinux [mold lldb])
          ++ (lib.optional stdenv.isDarwin [darwin.apple_sdk.frameworks.CoreFoundation]);

        buildInputs = with pkgs; [
          cargo-expand
          openssl
          cargo
          rust
        ];

        shellHook = ''
          export RUSTFLAGS="--cfg tokio_unstable ${rustflags}"
        '';
      };
    });
}
