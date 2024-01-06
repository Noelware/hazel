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

FROM rust:1.75-alpine3.18 AS build

RUN apk update && apk add --no-cache git ca-certificates curl musl-dev libc6-compat gcompat pkgconfig openssl-dev
WORKDIR /build

ENV RUSTFLAGS=-Ctarget-feature=-crt-static
ENV CARGO_INCREMENTAL=1

COPY . .
RUN cargo build --release

FROM alpine:3.19

RUN apk update && apk add --no-cache bash tini
WORKDIR /app/noelware/hazel

COPY --from=build /build/target/release/hazel /app/noelware/hazel/bin/hazel
COPY              distribution/docker/scripts /app/noelware/hazel/scripts
COPY              distribution/docker/config  /app/noelware/hazel/config

RUN mkdir -p /var/lib/noelware/hazel/data
RUN addgroup -g 1001 noelware && \
    adduser -DSH -u 1001 -G noelware noelware && \
    chown -R noelware:noelware /app/noelware/hazel && \
    chown -R noelware:noelware /var/lib/noelware/hazel/data && \
    chmod +x /app/noelware/hazel/scripts/docker-entrypoint.sh

ENV HAZEL_DISTRIBUTION_KIND=docker
EXPOSE 3939
VOLUME /var/lib/noelware/hazel/data

USER noelware
ENTRYPOINT ["/app/noelware/hazel/scripts/docker-entrypoint.sh"]
CMD ["/app/noelware/hazel/bin/hazel"]
