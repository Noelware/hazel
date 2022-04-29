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

package dev.floofy.hazel.cli.commands.keystore

import dev.floofy.hazel.cli.abstractions.BaseKeystoreCommand

object ListKeysCommand: BaseKeystoreCommand(
    "list",
    "List all the keys in the keystore."
) {
    override fun run() {
        val keystore = wrapper()
        val keys = keystore.keys()

        for (key in keys) {
            val isUser = key.startsWith("users.")
            if (isUser) {
                val name = key.split(".").last()
                println("===> User $name | Use `hazel keystore update-pwd $name` to update their password,")
                println("                | or use `hazel keystore delete users.$name` to delete their account")
                println("                | in the keystore.")
            } else {
                println("===> Key $key")
            }
        }
    }
}
