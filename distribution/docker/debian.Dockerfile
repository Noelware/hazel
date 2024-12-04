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

FROM rustlang/rust:nightly-bookworm-slim AS build

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get upgrade &&   \
    apt-get update -y && \
    apt-get install -y git \
        curl               \
        ca-certificates    \
        pkg-config         \
        libssl-dev         \
        mold

WORKDIR /build
COPY . .

ENV RUSTFLAGS="-C link-arg=-fuse-ld=mold -C target-cpu=native"
RUN cargo build --release --locked

FROM debian:bookworm-slim

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get upgrade &&   \
    apt-get update -y && \
    apt-get install -y curl bash tini

WORKDIR /app/noelware/hazel

COPY --from=build /build/target/release/hazel /app/noelware/hazel/bin/hazel
COPY distribution/docker/scripts              /app/noelware/hazel/scripts
COPY distribution/docker/config               /app/noelware/hazel/config

RUN mkdir -p /var/lib/noelware/hazel/data
RUN groupadd -g 1001 noelware && \
    useradd -rm -s /bin/bash -g noelware -u 1001 noelware && \
    chown 1001:1001 /app/noelware/hazel && \
    chown 1001:1001 /var/lib/noelware/hazel/data && \
    chmod +x /app/noelware/hazel/bin/hazel /app/noelware/hazel/scripts/docker-entrypoint.sh

EXPOSE 8989
VOLUME /var/lib/noelware/hazel/data

USER noelware
ENTRYPOINT ["/app/noelware/hazel/scripts/docker-entrypoint.sh"]
CMD ["/app/noelware/hazel/bin/hazel"]
