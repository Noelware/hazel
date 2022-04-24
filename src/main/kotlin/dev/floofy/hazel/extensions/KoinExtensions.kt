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

package dev.floofy.hazel.extensions

import org.koin.core.context.GlobalContext
import kotlin.properties.ReadOnlyProperty

/**
 * Injects a singleton into a property.
 * ```kt
 * class Owo {
 *    val kord: Kord by inject()
 * }
 * ```
 */
inline fun <reified T> inject(): ReadOnlyProperty<Any?, T> =
    ReadOnlyProperty<Any?, T> { _, _ ->
        val koin = GlobalContext.get()
        koin.get()
    }

/**
 * Retrieve a singleton from the Koin application without chaining `.get()` methods twice.
 * ```kt
 * val kord: Kord = GlobalContext.retrieve()
 * ```
 */
inline fun <reified T> GlobalContext.retrieve(): T = get().get()

/**
 * Returns a list of singletons that match with type [T].
 * ```kt
 * val commands: List<AbstractCommand> = GlobalContext.retrieveAll()
 * // => List<Command> [ ... ]
 * ```
 */
inline fun <reified T> GlobalContext.retrieveAll(): List<T> = get().getAll()
