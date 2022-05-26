package dev.floofy.hazel.server.config

import kotlinx.serialization.SerialName
import org.noelware.remi.filesystem.FilesystemStorageConfig
import org.noelware.remi.minio.MinIOStorageConfig
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
    S3,

    /**
     * Initializes the MinIO storage class
     */
    @SerialName("minio")
    MINIO;
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
     * Configures a MinIO server as a storage source if [StorageClass] is [MINIO][StorageClass.MINIO].
     */
    val minio: MinIOStorageConfig? = null,

    /**
     * Alias for [filesystem].
     */
    val fs: FilesystemStorageConfig? = null,

    /**
     * Configures Amazon S3 or a S3-compatible server if [StorageClass] is [S3][StorageClass.S3]
     */
    val s3: S3StorageConfig? = null
)
