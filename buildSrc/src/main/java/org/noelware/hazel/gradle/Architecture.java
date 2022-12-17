/*
 * 🪶 Hazel: Minimal, and fast HTTP proxy to host files from any cloud storage provider.
 * Copyright 2022-2023 Noelware <team@noelware.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.noelware.hazel.gradle;

/**
 * Represents the host's CPU architecture.
 */
public enum Architecture {
    X64,
    AARCH64;

    public static Architecture current() {
        final String arch = System.getProperty("os.arch");
        return switch (arch) {
            case "x86_64", "amd64" -> X64;
            case "aarch64", "arm64" -> AARCH64;
            default -> throw new RuntimeException("Architecture [%s] is not supported.".formatted(arch));
        };
    }

    public boolean isX64() {
        return this == X64;
    }

    public boolean isArm64() {
        return this == AARCH64;
    }

    public String getName() {
        return switch (this) {
            case AARCH64 -> "aarch64";
            case X64 -> "x86_64";
        };
    }

    @Override
    public String toString() {
        return "Architecture(%s)".formatted(getName());
    }
}