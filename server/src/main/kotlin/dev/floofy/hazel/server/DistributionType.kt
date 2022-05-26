package dev.floofy.hazel.server

enum class DistributionType(val key: String, val id: String) {
    UNKNOWN("?", "?"),
    DOCKER("docker", "docker"),
    TAR("tar", "tar (local)"),
    ZIP("tar", "zip (local)"),
    RPM("rpm", "rpm"),
    DEB("deb", "deb");

    companion object {
        fun fromString(key: String): DistributionType = values().find { it.key == key } ?: UNKNOWN
    }
}
