package dev.floofy.hazel.server

import dev.floofy.hazel.server.config.StorageClass
import dev.floofy.hazel.server.config.StorageConfig
import dev.floofy.utils.slf4j.logging
import kotlinx.coroutines.runBlocking
import org.noelware.remi.core.StorageTrailer
import org.noelware.remi.core.figureContentType
import org.noelware.remi.filesystem.FilesystemStorageTrailer
import org.noelware.remi.minio.MinIOStorageTrailer
import org.noelware.remi.s3.S3StorageTrailer
import java.io.InputStream

class StorageWrapper(config: StorageConfig) {
    private val log by logging<StorageWrapper>()
    val trailer: StorageTrailer<*>

    init {
        log.info("Figuring out what trailer to use...")

        trailer = when (config.storageClass) {
            StorageClass.FS -> {
                assert(config.fs != null) { "Configuration for the local disk is missing." }

                FilesystemStorageTrailer(config.fs!!.directory)
            }

            StorageClass.FILESYSTEM -> {
                assert(config.filesystem != null) { "Configuration for the local disk is missing." }

                FilesystemStorageTrailer(config.filesystem!!.directory)
            }

            StorageClass.S3 -> {
                assert(config.s3 != null) { "Configuration for Amazon S3 is missing." }

                S3StorageTrailer(config.s3!!)
            }

            StorageClass.MINIO -> {
                assert(config.minio != null) { "Configuration for MinIO is missing." }

                MinIOStorageTrailer(config.minio!!)
            }
        }

        log.info("Figured out that we are using ${config.storageClass}!")

        runBlocking {
            try {
                log.info("Initializing storage trailer...")
                trailer.init()
            } catch (e: Exception) {
                if (e is IllegalStateException && e.message?.contains("doesn't support StorageTrailer#init/0") == true)
                    return@runBlocking

                throw e
            }
        }
    }

    /**
     * Opens a file under the [path] and returns the [InputStream] of the file.
     */
    suspend fun open(path: String): InputStream? = trailer.open(path)
    suspend fun listAll(): List<org.noelware.remi.core.Object> = trailer.listAll()

    fun <I: InputStream> findContentType(stream: I): String = trailer.figureContentType(stream)
}
