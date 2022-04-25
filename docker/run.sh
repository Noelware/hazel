#!/bin/bash

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

set -o errexit
set -o nounset
set -o pipefail

. /app/noel/hazel/scripts/liblog.sh

info "*** starting hazel! ***"
debug "   ===> Logback Configuration: ${HAZEL_LOGBACK_CONFIG_PATH:-unknown}"
debug "   ===> Dedicated Node:        ${WINTERFOX_DEDI_NODE:-unknown}"
debug "   ===> JVM Arguments:         ${HAZEL_JAVA_OPTS:-unknown}"

JAVA_OPTS=("-XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8")

if [[ -n "${HAZEL_LOGBACK_CONFIG_PATH:-}" && -f "${HAZEL_LOGBACK_CONFIG_PATH}" ]]; then
  JAVA_OPTS+=("-Ddev.floofy.hazel.logback.config=${HAZEL_LOGBACK_CONFIG_PATH}")
fi

if [[ -n "${WINTERFOX_DEDI_NODE:-}" ]]; then
  JAVA_OPTS+=("-Dwinterfox.dediNode=${WINTERFOX_DEDI_NODE}")
fi

if [[ -n "${HAZEL_JAVA_OPTS:-}" ]]; then
  JAVA_OPTS+=("$HAZEL_JAVA_OPTS")
fi

debug "Resolved JVM arguments: $JAVA_OPTS"

/app/noel/hazel/bin/hazel $@
