#!/usr/bin/env bash
#
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

set -euo pipefail

function hazel::os {
    case "$(uname -s)" in
        Darwin)
            echo "darwin"
            ;;
        Linux)
            echo "linux"
            ;;
        *)
            echo "Unsupported operating system: \`$(uname -s)\`" >&2
            exit 1
            ;;
    esac
}

function hazel::arch {
    case "$(uname -m)" in
        x86_64|amd64)
            echo "x86_64"
            ;;

        aarch64|arm64)
            echo "arm64"
            ;;

        *)
            echo "Unsupported architecture: \`$(uname -m)\`" >&2
            exit 1
            ;;
    esac
}

function hazel::binary_url {
    local version="$1"

    arch=$(hazel::arch)
    os=$(hazel::os)

    echo "https://artifacts.noelware.org/hazel/$version/hazel-$os-$arch"
}

function hazel::checksum_url {
    local version="$1"

    arch=$(hazel::arch)
    os=$(hazel::os)

    echo "https://artifacts.noelware.org/hazel/$version/hazel-$os-$arch.sha256"
}

blue=''
green=''
pink=''
reset=''
bold=''
underline=''
red=''
yellow=''

if [[ -t 1 ]]; then
    blue='\033[38;2;81;81;140m'
    green='\033[38;2;165;204;165m'
    pink='\033[38;2;241;204;209m'
    reset='\033[0m'
    bold='\033[1m'
    underline='\033[4m'
    red='\033[38;166;76;76m'
    yellow='\033[38;227;227;172m'
fi

hazel::error() {
    echo -e "${red}error${reset}:" "$@" >&2
}

hazel::fatal() {
    echo -e "${bold}${red}fatal${reset}${reset}:" "$@" >&2
    exit 1
}

hazel::info() {
    echo -e "${green}info${reset}:" "$@"
}

hazel::warn() {
    echo -e "${yellow}warn${reset}:" "$@"
}

hazel::command-exists() {
    command -v "$1" >/dev/null
}

function hazel::download {
    local url="$1"
    local loc="$2"

    if hazel::command-exists curl >/dev/null; then
        curl -sSL "$url" -o "$loc" || {
            hazel::fatal "failed to download $url into $loc"
        }
    elif hazel::command-exists wget >/dev/null; then
        wget -q "$1" -O "$2" || {
            hazel::fatal "failed to download $url into $loc"
        }
    else
        hazel::fatal "Failed to download Hazel as \`curl\` or \`wget\` was not found on the system"
    fi
}

function hazel::fetch {
    local url="$1"

    if hazel::command-exists curl >/dev/null; then
        curl -fsSL "$url" || {
            hazel::fatal "failed to fetch $url"
        }
    elif hazel::command-exists >/dev/null; then
        wget -q -S -O - "$url" 2>&1 || {
            hazel::fatal "failed to fetch $url"
        }
    else
        hazel::fatal "missing either \`wget\` or \`curl\`" >/dev/stderr
    fi
}

function hazel::install {
    local version="latest"
    local installdir="/usr/local"
    local allowpathoverwrite="true"

    while [ "$#" -gt 0 ]; do
        case "$1" in
            --version|-v)
                if [ $# -lt 2 ]; then
                    hazel::error "\`--version\` flag requires a value (i.e., --version 2.0.0)" >&2
                    exit 1
                fi

                version="$2"
                shift 2
                ;;

            --install-dir)
                if [ $# -lt 2 ]; then
                    hazel::error "\`--install-dir\` flag requires a value (i.e., --install-dir /usr/local)" >&2
                    exit 1
                fi

                installdir="$2"
                shift 2
                ;;

            --no-path-overwrite)
                allowpathoverwrite="false"
                shift 1
                ;;

            --)
                shift
                break
                ;;

            -*)
                hazel::error "unknown option: $1" >&2
                exit 1
                ;;

            *)
                break
                ;;
        esac
    done

    if [ "$version" == "latest" ]; then
        hazel::info "resolving latest version..."
        version=$(hazel::fetch "https://artifacts.noelware.org/hazel/versions.json" | jq '.latest.version' | tr -d '"')

        hazel::info "resolved: $version"
    fi

    binary_url=$(hazel::binary_url "$version")
    checksums_url=$(hazel::checksum_url "$version")

    hazel::info "resolved options:"
    hazel::info "Distribution URL => $binary_url"
    hazel::info "   Checksums URL => $checksums_url"

    tmpdir=$(mktemp -d)
    tmploc="$tmpdir/hazel-$(hazel::os)-$(hazel::arch)"

    hazel::download "$binary_url" "$tmploc"

    if hazel::command-exists sha256sum; then
        binary=$(sha256sum "$tmploc" | awk '{print $1}')
        server=$(hazel::fetch "$checksums_url")

        if ! grep -q "$binary" "$server"; then
            hazel::error "failed to compute checksum for \`hazel\` binary"
            hazel::error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
            hazel::error "generated by system:"
            hazel::error "$binary"
            hazel::error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
            hazel::error "generated by artifacts server:"
            hazel::error "$server"

            rm -rf "$tmpdir" || {
                hazel::warn "failed to cleanup [$tmpdir]; manual intervention is required"
                true
            }

            hazel::fatal "SHA256 checksums do not compute correctly; possibly artifacts server is down?"
        fi

        mv "$tmploc" "$installdir/bin/hazel"
        hazel::info "binary is now installed in \`$installdir/bin/hazel\`!"
    fi

    if hazel::command-exists shasum; then
        binary=$(shasum -a 256 "$tmploc" | awk '{print $1}')
        server=$(hazel::fetch "$checksums_url")

        if ! grep -q "$binary" "$server"; then
            hazel::error "failed to compute checksum for \`hazel\` binary"
            hazel::error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
            hazel::error "generated by system:"
            hazel::error "$binary"
            hazel::error "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
            hazel::error "generated by artifacts server:"
            hazel::error "$server"

            rm -rf "$tmpdir" || {
                hazel::warn "failed to cleanup [$tmpdir]; manual intervention is required"
                true
            }

            hazel::fatal "SHA256 checksums do not compute correctly; possibly artifacts server is down?"
        fi

        mv "$tmploc" "$installdir/bin/hazel"
        hazel::info "binary is now installed in \`$installdir/bin/hazel\`!"
    fi

    rm -rf "$tmpdir" || {
        hazel::warn "failed to cleanup [$tmpdir]; manual intervention is required"
        true
    }

    hazel::fatal "missing either \`sha256sum\` or \`shasum\` binaries to compute checksum verification"
}

hazel::install "$@"
