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

package dev.floofy.hazel.data

import kotlinx.serialization.SerialName
import org.noelware.remi.filesystem.FilesystemStorageConfig
import org.noelware.remi.s3.S3StorageConfig

/**
 * Represents the `config.storage.class` option which configures
 * the storage wrapper.
 */
@kotlinx.serialization.Serializable
enum class StorageClass {
    /**
     * Initializes the filesystem storage class.
     */
    @SerialName("filesystem")
    FILESYSTEM,

    /**
     * Alias for [FILESYSTEM].
     */
    @SerialName("fs")
    FS,

    /**
     * Initializes the S3 storage class.
     */
    @SerialName("s3")
    S3;
}

/**
 * Represents the storage configuration for hazel to use.
 */
@kotlinx.serialization.Serializable
data class StorageConfig(
    /**
     * Represents the [StorageClass] to use.
     */
    @SerialName("class")
    val storageClass: StorageClass,

    /**
     * Configures the filesystem as a source if [StorageClass] is [FILESYSTEM][StorageClass.FILESYSTEM] or
     * [FS][StorageClass.FS]
     */
    val filesystem: FilesystemStorageConfig? = null,

    /**
     * Alias for [filesystem].
     */
    val fs: FilesystemStorageConfig? = null,

    /**
     * Configures Amazon S3 or a S3-compatible server if [StorageClass] is [S3][StorageClass.S3]
     */
    val s3: S3StorageConfig? = null
)
