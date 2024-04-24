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

    flake-compat = {
      url = github:edolstra/flake-compat;
      flake = false;
    };
  };

  outputs = {
    self,
    nixpkgs,
    flake-utils,
    rust-overlay,
    ...
  }:
    flake-utils.lib.eachDefaultSystem (system: let
      pkgs = import nixpkgs {
        inherit system;

        overlays = [(import rust-overlay)];
        config.allowUnfree = true;
      };

      rust = pkgs.rust-bin.fromRustupToolchainFile ./rust-toolchain.toml;
      cargoTOML = builtins.fromTOML (builtins.readFile ./Cargo.toml);
      stdenv =
        if pkgs.stdenv.isLinux
        then pkgs.stdenv
        else pkgs.clangStdenv;

      rustPlatform = pkgs.makeRustPlatform {
        rustc = rust;
        cargo = rust;
      };

      rustflags =
        if pkgs.stdenv.isLinux
        then ''-C link-arg=-fuse-ld=mold -C target-cpu=native $RUSTFLAGS''
        else ''$RUSTFLAGS'';

      hazel = rustPlatform.buildRustPackage {
        nativeBuildInputs = with pkgs; [pkg-config];
        buildInputs = with pkgs; [openssl];
        cargoSha256 = pkgs.lib.fakeSha256;
        version = "${cargoTOML.package.version}";
        name = "hazel";
        src = ./.;

        cargoLock = {
          lockFile = ./Cargo.lock;
          outputHashes = {
            # "noelware-config-0.1.0" = "sha256-4yred15se1RB2LJJ2htB8DPMfcCo9+9ZWNRFlsmbDmQ=";
            # "arboard-3.3.2" = "sha256-H2xeFJkoeg0kN3pKsb2P4rxEeIbkoSwLVqFzBz5eb7g=";
            # "azalia-0.1.0" = "sha256-wSBYHva/VbU0F++2XBUrg1Onhatq46gjksDyv1aMaeM=";
          };
        };

        meta = with pkgs.lib; {
          description = "Minimal, and easy HTTP proxy to map storage provider items into HTTP endpoints";
          homepage = "https://noelware.org/hazel";
          license = with licenses; [asl20];
          maintainers = with maintainers; [auguwu noelware];
          mainProgram = "hazel";
        };
      };
    in {
      packages = {
        inherit hazel;
        default = hazel;
      };

      devShells.default = pkgs.mkShell {
        LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath (with pkgs; [openssl]);
        nativeBuildInputs = with pkgs;
          [pkg-config]
          ++ (lib.optional stdenv.isLinux [mold lldb gdb])
          ++ (lib.optional stdenv.isDarwin [darwin.apple_sdk.frameworks.CoreFoundation]);

        buildInputs = with pkgs; [
          cargo-machete
          cargo-expand
          cargo-deny

          openssl
          glibc
          rust
          git
        ];
      };
    });
}
