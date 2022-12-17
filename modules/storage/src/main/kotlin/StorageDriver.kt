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

import org.noelware.remi.core.Blob
import org.noelware.remi.core.UploadRequest

interface StorageDriver {
    /**
     * Uploads a file to the given storage provider with the given [upload request][UploadRequest]. If the
     * contents exceed over >=50MB, then the storage provider will attempt to do a multipart request on some implementations.
     *
     * @param builder builder dsl
     */
    fun upload(builder: UploadRequest.Builder.() -> Unit = {})

    /**
     * Checks if the given [path] exists on the storage server.
     * @param path The given relative (or absolute) path.
     */
    fun exists(path: String): Boolean

    /**
     * Deletes the given [path] from the storage service.
     * @param path The given relative (or absolute) path.
     */
    fun delete(path: String): Boolean

    /**
     * Returns a [Blob] from the given [path] specified. This method can return
     * `null` if the blob was not found.
     *
     * @param path The relative (or absolute) path to get the blob from
     */
    fun blob(path: String): Blob?
}
