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

package org.noelware.hazel.modules.storage

import dev.floofy.utils.slf4j.logging
import org.noelware.hazel.configuration.kotlin.dsl.storage.StorageConfig
import org.noelware.remi.core.Blob
import org.noelware.remi.core.StorageService
import org.noelware.remi.core.UploadRequest
import org.noelware.remi.support.azure.AzureBlobStorageService
import org.noelware.remi.support.filesystem.FilesystemStorageService
import org.noelware.remi.support.gcs.GoogleCloudStorageService
import org.noelware.remi.support.s3.AmazonS3StorageService

class DefaultStorageDriver(config: StorageConfig): StorageDriver {
    override val service: StorageService<*>
    private val log by logging<DefaultStorageDriver>()

    init {
        log.info("Initializing storage driver...")
        service = when {
            config.filesystem != null -> FilesystemStorageService(config.filesystem!!.toRemiConfig())
            config.azure != null -> AzureBlobStorageService(config.azure!!.toRemiConfig())
            config.gcs != null -> GoogleCloudStorageService(config.gcs!!.toRemiConfig())
            config.s3 != null -> AmazonS3StorageService(config.s3!!.toRemiConfig())
            else -> throw IllegalStateException("Unable to determine what storage driver to use")
        }

        service.init()
    }

    override fun upload(builder: UploadRequest.Builder.() -> Unit) = service.upload(UploadRequest.builder().apply(builder).build())
    override fun delete(path: String): Boolean = service.delete(path)
    override fun exists(path: String): Boolean = service.exists(path)
    override fun blob(path: String): Blob? = service.blob(path)
}
