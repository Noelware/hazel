package dev.floofy.hazel.server

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlConfig
import dev.floofy.hazel.server.config.Config
import dev.floofy.hazel.server.keystore.KeystoreWrapper
import dev.floofy.utils.slf4j.logging
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.io.File
import java.io.IOError
import kotlin.concurrent.thread
import kotlin.system.exitProcess

object Bootstrap {
    private val log by logging<Bootstrap>()

    // credit: https://github.com/elastic/logstash/blob/main/logstash-core/src/main/java/org/logstash/Logstash.java#L98-L133
    private fun installDefaultThreadExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            if (e is Error) {
                log.error("Uncaught fatal error in thread ${t.name} (#${t.id}):", e)
                log.error("If this keeps occurring, please report it to Noel: https://github.com/auguwu/hazel/issues")

                var success = false

                if (e is InternalError) {
                    success = true
                    halt(128)
                }

                if (e is OutOfMemoryError) {
                    success = true
                    halt(127)
                }

                if (e is StackOverflowError) {
                    success = true
                    halt(126)
                }

                if (e is UnknownError) {
                    success = true
                    halt(125)
                }

                if (e is IOError) {
                    success = true
                    halt(124)
                }

                if (e is LinkageError) {
                    success = true
                    halt(123)
                }

                if (!success) halt(120)

                exitProcess(1)
            } else {
                log.error("Uncaught exception in thread ${t.name} (#${t.id}):", e)
            }
        }
    }

    private fun halt(code: Int) {
        Runtime.getRuntime().halt(code)
    }

    private fun installShutdownHook() {
        Runtime.getRuntime().addShutdownHook(thread(start = false, name = "Hazel-ShutdownThread") {

        })
    }

    fun launch(configFile: File) {
        Thread.currentThread().name = "Hazel-BootstrapThread"
        log.info("Starting server...")

        // Install hooks
        installDefaultThreadExceptionHandler()
        installShutdownHook()

        // Configure the hazel config
        if (configFile.extension != "toml")
            throw IllegalStateException("Configuration file $configFile must be a TOML file (must be `.toml` extension, not ${configFile.extension})")

        val toml = Toml(
            TomlConfig(
                ignoreUnknownNames = true,
                allowEmptyToml = false,
                allowEmptyValues = false,
                allowEscapedQuotesInLiteralStrings = true
            )
        )

        val config = toml.decodeFromString(Config.serializer(), configFile.readText())
        val keystore = KeystoreWrapper(config.keystore.path, config.keystore.password)
        val storage = StorageWrapper(config.storage)

        val koin = startKoin {
            modules(
                module {
                    single { config }
                    single { keystore }
                    single { storage }
                },

                hazelModule
            )
        }

        runBlocking {
            val hazel = koin.koin.get<Hazel>()
            try {
                hazel.start()
            } catch (e: Exception) {
                log.error("Unable to bootstrap Hazel:", e)
                exitProcess(1)
            }
        }
    }
}
