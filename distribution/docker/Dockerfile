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

FROM --platform=$BUILDPLATFORM eclipse-temurin:17.0.6_10-jdk-alpine AS jdk-runtime

RUN apk update && \
    apk add --no-cache binutils && \
    jlink --add-modules ALL-MODULE-PATH \
            --strip-debug \
            --no-man-pages \
            --no-header-files \
            --compress=2 \
            --output /runtime

FROM --platform=$BUILDPLATFORM eclipse-temurin:17.0.6_10-jdk-alpine AS gradle-build

RUN apk update && apk add --no-cache git ca-certificates gcompat libc6-compat curl
WORKDIR /build/server

COPY . .
RUN chmod +x ./gradlew && ./gradlew :cli:installDist --no-daemon --stacktrace

FROM --platform=$BUILDPLATFORM alpine:3.17

RUN apk update && apk add --no-cache bash tini libc6-compat gcompat
WORKDIR /app/noelware/hazel

ENV JAVA_HOME=/opt/openjdk/java
COPY --from=gradle-build /build/server/cli/build/install/hazel/config /app/noelware/hazel/config
COPY --from=gradle-build /build/server/cli/build/install/hazel/lib    /app/noelware/hazel/lib
COPY --from=gradle-build /build/server/cli/build/install/hazel/bin    /app/noelware/hazel/bin
COPY                     distribution/docker/scripts/linux            /app/noelware/hazel/scripts
COPY --from=jdk-runtime  /runtime                                     /opt/openjdk/java

# Remove the PowerShell script (since it's useless on *UNIX)
RUN rm /app/noelware/hazel/bin/hazel.ps1

ENV HAZEL_DISTRIBUTION_TYPE=docker
EXPOSE 8901
VOLUME /var/lib/noelware/hazel/data

RUN mkdir -p /var/lib/noelware/hazel/data
RUN addgroup -g 1001 noelware && \
    adduser -DSH -u 1001 -G noelware noelware && \
    chown -R noelware:noelware /app/noelware/hazel && \
    chown -R noelware:noelware /var/lib/noelware/hazel/data && \
    chmod +x /app/noelware/hazel/bin/hazel /app/noelware/hazel/scripts/docker-entrypoint.sh

# Create a symbolic link so you can just run `hazel` without specifying
# the full path.
RUN ln -s /app/noelware/hazel/bin/hazel /usr/bin/hazel

USER noelware
ENTRYPOINT ["/app/noelware/hazel/scripts/docker-entrypoint.sh"]
CMD ["/app/noelware/hazel/bin/hazel", "server"]
