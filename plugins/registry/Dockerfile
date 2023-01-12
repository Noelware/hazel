# 🪶 Hazel: Minimal, and fast HTTP proxy to host files from any cloud storage provider.
# Copyright 2022-2023 Noelware <team@noelware.org>
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

FROM --platform=$BUILDPLATFORM eclipse-temurin:17.0.5_8-jdk-alpine AS jdk-runtime

RUN apk update && \
    apk add --no-cache binutils && \
    jlink --add-modules ALL-MODULE-PATH \
            --strip-debug \
            --no-man-pages \
            --no-header-files \
            --compress=2 \
            --output /runtime

FROM --platform=$BUILDPLATFORM eclipse-temurin:17.0.5_8-jdk-alpine AS gradle-build

ENV DEBIAN_FRONTEND=noninteractive
RUN apt update && apt install -y git ca-certificates curl
WORKDIR /build/registry

COPY . .
RUN chmod +x ./gradlew && ./gradlew :plugins:registry:installDist --no-daemon --stacktrace

FROM --platform=$BUILDPLATFORM alpine:3.17

RUN apk update && apk add --no-cache bash tini libc6-compat gcompat
WORKDIR /app/noelware/hazel/plugins/registry

ENV JAVA_HOME=/opt/openjdk/java
COPY --from=gradle-build /build/registry/cli/build/install/hazel/config /app/noelware/hazel/plugins/registry/config
COPY --from=gradle-build /build/registry/cli/build/install/hazel/lib    /app/noelware/hazel/plugins/registry/lib
COPY --from=gradle-build /build/registry/cli/build/install/hazel/bin    /app/noelware/hazel/plugins/registry/bin
COPY                     distribution/docker/scripts/linux              /app/noelware/hazel/plugins/registry/scripts
COPY --from=jdk-runtime  /runtime                                       /opt/openjdk/java

ENV HAZEL_DISTRIBUTION_TYPE=docker
EXPOSE 18921
VOLUME /var/lib/noelware/hazel/data

RUN mkdir -p /var/lib/noelware/hazel/data
RUN addgroup -g 1001 noelware && \
    adduser -DSH -u 1001 -G noelware noelware && \
    chown -R noelware:noelware /app/noelware/hazel/plugins/registry && \
    chmod +x /app/noelware/hazel/bin/hazel /app/noelware/hazel/plugins/registry/scripts/docker-entrypoint.sh

USER noelware
ENTRYPOINT ["/app/noelware/hazel/scripts/docker-entrypoint.sh"]
CMD ["/app/noelware/hazel/plugins/registry/bin/plugins-registry"]
