package dev.floofy.hazel.server

import dev.floofy.hazel.server.config.Config
import dev.floofy.hazel.server.keystore.KeystoreWrapper
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val hazelModule = module {
    single {
        Json {
            allowSpecialFloatingPointValues = true
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    single {
        Hazel(get())
    }
}
