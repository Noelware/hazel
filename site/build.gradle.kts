/*
 * 🪶 hazel: Minimal, simple, and open source content delivery network made in Kotlin
 * Copyright 2022 Noel <cutie@floofy.dev>
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

import java.io.ByteArrayOutputStream

val buildImage = tasks.register<Exec>("buildDockerImage") {
    doFirst {
        logger.lifecycle("Checking if Docker client is available...")

        val output = ByteArrayOutputStream()

        standardOutput = output
        commandLine("docker", "version", "--format", "\"v{{.Client.Version}} ({{.Client.Os}}/{{.Client.Arch}})\"")

        doLast {
            val data = String(output.toByteArray())
            logger.lifecycle("Found Docker $data!")
        }
    }

    // $ docker build . -f ./site/Dockerfile --platform linux/amd64 -t registry.floofy.dev/noel/hazel-site:master
    commandLine(
        "docker",
        "build",
        ".",
        "-f",
        "./site/Dockerfile",
        "--platform",
        "linux/amd64",
        "-t",
        "registry.floofy.dev/noel/hazel-site:master"
    )
}

tasks.register<Exec>("buildAndPush") {
    dependsOn(buildImage)

    // $ docker push registry.floofy.dev/noel/hazel-site:master
    commandLine(
        "docker",
        "push",
        "registry.floofy.dev/noel/hazel-site:master"
    )

    doLast {
        logger.lifecycle("Finished! :3")
    }
}

tasks.register<Exec>("build") {
    doFirst {
        logger.lifecycle("Checking if Node.js exists on system...")

        val output = ByteArrayOutputStream()
        standardOutput = output

        // $ node version
        commandLine("node", "version")

        doLast {
            logger.lifecycle("Node.js exists on system!")
        }
    }

    commandLine("yarn", "build")
}
