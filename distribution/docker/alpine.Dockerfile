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

###### BINARY BUILD
FROM rustlang/rust:nightly-alpine3.20 AS build

RUN apk upgrade && apk add --no-cache git \
    mold                                  \
    ca-certificates                       \
    musl-dev                              \
    pkgconfig                             \
    openssl-dev                           \
    build-base

WORKDIR /build
COPY . .

ENV RUSTFLAGS="-C link-arg=-fuse-ld=mold -C target-cpu=native -C target-feature=-crt-static"
RUN cargo build --release --locked

##### FINAL STAGE
FROM alpine:3.20

RUN apk upgrade && apk add --no-cache bash tini curl libgcc

WORKDIR /app/noelware/hazel

COPY --from=build /build/target/release/hazel /app/noelware/hazel/bin/hazel
COPY distribution/docker/scripts              /app/noelware/hazel/scripts
COPY distribution/docker/config               /app/noelware/hazel/config

EXPOSE 8989
VOLUME /var/lib/noelware/hazel/data

RUN mkdir -p /var/lib/noelware/hazel/data
RUN addgroup -g 1001 noelware && \
    adduser -DSH -u 1001 -G noelware noelware && \
    chown -R noelware:noelware /app/noelware/hazel && \
    chown -R noelware:noelware /var/lib/noelware/hazel/data && \
    chmod +x /app/noelware/hazel/bin/hazel /app/noelware/hazel/scripts/docker-entrypoint.sh

USER noelware
ENTRYPOINT ["/app/noelware/hazel/scripts/docker-entrypoint.sh"]
CMD ["/app/noelware/hazel/bin/hazel"]
