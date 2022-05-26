package dev.floofy.hazel.gradle

import dev.floofy.utils.gradle.ReleaseType
import dev.floofy.utils.gradle.Version
import org.gradle.api.JavaVersion
import java.io.File
import java.util.concurrent.TimeUnit

val VERSION = Version(2, 0, 0, 0, ReleaseType.Snapshot)

val JAVA_VERSION = JavaVersion.VERSION_17

val COMMIT_HASH by lazy {
    val cmd = "git rev-parse --short HEAD".split("\\s".toRegex())
    val proc = ProcessBuilder(cmd)
        .directory(File("."))
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    proc.waitFor(1, TimeUnit.MINUTES)
    proc.inputStream.bufferedReader().readText().trim()
}
