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

package org.noelware.hazel.gradle.plugins;

import com.netflix.gradle.plugins.packaging.ProjectPackagingExtension;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;
import org.jetbrains.annotations.NotNull;
import org.noelware.hazel.gradle.Architecture;

import java.io.File;
import java.util.List;

public class HazelNebulaPlugin implements Plugin<Project> {
    public static final String DESCRIPTION = String.join("\n", List.of(
            "Hazel is a minimal, and reliable HTTP proxy microservice to do CRUD operations on your",
            "storage service with proper authentication and a cute dashboard!~ Hazel is made with",
            "Kotlin by Noelware, LLC.",
            "",
            "The software packaged is from the main repository hosted on GitHub",
            "and distributed to Noelware's APT repository hosted at",
            "https://artifacts.noelware.cloud/deb/noelware/hazel",
            "",
            "~ Noelware, LLC. <team@noelware.org> ^~^"
    ));

    @Override
    public void apply(@NotNull Project project) {
        project.getPlugins().apply("com.netflix.nebula.ospackage-base");

        final ExtensionContainer extensions = project.getExtensions();
        extensions.configure("ospackage", (Action<ProjectPackagingExtension>) (extension) -> {
            extension.setMaintainer("Noelware, LLC.");
            extension.setSummary("\uD83E\uDEB6 Minimal, and fast HTTP proxy to host files from any cloud storage provider.");
            extension.setUrl("https://noelware.org/hazel");
            extension.setPackageDescription(DESCRIPTION);
            extension.setArchStr(Architecture.current().isX64() ? "amd64" : "arm64");
            extension.requires("temurin-17-jdk");

            final String signingPassword = System.getenv("NOELWARE_SIGNING_PASSWORD");
            if (signingPassword != null) {
                extension.setSigningKeyPassphrase(signingPassword);
                extension.setSigningKeyId(System.getenv("NOELWARE_SIGNING_KEY_ID"));

                var ringPath = System.getenv("NOELWARE_SIGNING_RING_PATH");
                extension.setSigningKeyRingFile(
                        new File(ringPath != null ? ringPath : System.getProperty("user.home"), ".gnupg/secring.gpg"));
            }
        });
    }
}
