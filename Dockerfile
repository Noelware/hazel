# 🪶 hazel: Minimal, simple, and open source content delivery network made in Kotlin
# Copyright 2022 Noel <cutie@floofy.dev>
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

# Create a builder stage, where hazel is built.
FROM eclipse-temurin:18-jdk-alpine AS builder

# Install git, which is required for metadata purposes
RUN apk update && apk add --no-cache git ca-certificates

# Set the working directory to /build/hazel
WORKDIR /build/hazel

# Copy everything to the image
COPY . .

# Assuming that the Docker context is in the root directory (where `gradlew` is at)
RUN chmod +x ./gradlew

# Build the CLI and server!
RUN ./gradlew installDist --stacktrace --no-daemon

# Create the last stage, which is the runtime image where
# hazel is ran as a container!
FROM eclipse-temurin:18-jdk-alpine

# Install bash, which is required to execute the Docker scripts
# We will also install `tini`, which is a valid "init" for containers.
RUN apk update && apk add --no-cache tini bash libc6-compat gcompat

# Set the working directory to /app/noel/hazel
WORKDIR /app/noel/hazel

# Copy the Docker scripts in the `scripts/` directory
COPY docker /app/noel/hazel/scripts

# Copy all the assets here
COPY assets /app/noel/hazel/assets

# Copy the built distribution from the `builder` stage.
COPY --from=builder /build/hazel/build/install/hazel .

# Make sure the Docker scripts are executable
RUN chmod +x /app/noel/hazel/scripts/docker-entrypoint.sh /app/noel/hazel/scripts/run.sh

# Create a symbolic link so you can execute `hazel` outside
RUN ln -s /app/noel/hazel/bin/hazel /usr/local/bin/hazel

# Do not run the container as the root user for security reasons.
USER 1001

# Set the entrypoint and the runner.
ENTRYPOINT ["/app/noel/hazel/scripts/docker-entrypoint.sh"]
CMD ["/app/noel/hazel/scripts/run.sh"]
