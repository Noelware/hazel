package dev.floofy.hazel.core

import dev.floofy.hazel.data.StorageClass
import dev.floofy.hazel.data.StorageConfig
import gay.floof.utils.slf4j.logging
import kotlinx.coroutines.runBlocking
import org.noelware.remi.core.StorageTrailer
import org.noelware.remi.filesystem.FilesystemStorageTrailer
import org.noelware.remi.s3.S3StorageTrailer
import java.io.InputStream

/**
 * Wrapper for configuring the storage trailer that **hazel** will use.
 */
class StorageWrapper(config: StorageConfig) {
    private val trailer: StorageTrailer<*>
    private val log by logging<StorageWrapper>()

    init {
        log.info("Figuring out what storage trailer to use...")

        trailer = when (config.storageClass) {
            StorageClass.FS -> {
                assert(config.fs != null) { "Configuration for filesystem configuration is missing." }

                FilesystemStorageTrailer(config.fs!!.directory)
            }

            StorageClass.FILESYSTEM -> {
                assert(config.filesystem != null) { "Configuration for filesystem configuration is missing." }

                FilesystemStorageTrailer(config.filesystem!!.directory)
            }

            StorageClass.S3 -> {
                assert(config.s3 != null) { "" }

                S3StorageTrailer(config.s3!!)
            }
        }

        log.info("Using storage trailer ${config.storageClass}!")

        // block the main thread so the trailer can be
        // loaded successfully.
        runBlocking {
            try {
                trailer.init()
            } catch (e: Exception) {
                // skip
            }
        }
    }

    /**
     * Opens a file under the [path] and returns the [InputStream] of the file.
     */
    suspend fun open(path: String): InputStream? = trailer.open(path)

    /**
     * Deletes the file under the [path] and returns a [Boolean] if the
     * operation was a success or not.
     */
    suspend fun delete(path: String): Boolean = trailer.delete(path)

    /**
     * Checks if the file exists under this storage trailer.
     * @param path The path to find the file.
     */
    suspend fun exists(path: String): Boolean = trailer.exists(path)

    /**
     * Uploads file to this storage trailer and returns a [Boolean] result
     * if the operation was a success or not.
     *
     * @param path The path to upload the file to
     * @param stream The [InputStream] that represents the raw data.
     * @param contentType The content type of the file (useful for S3 and GCS support)!
     */
    suspend fun upload(
        path: String,
        stream: InputStream,
        contentType: String = "application/octet-stream"
    ): Boolean = trailer.upload(path, stream, contentType)
}
