package dev.floofy.hazel

import de.mkammerer.argon2.Argon2Factory
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val globalModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    single { Hazel() }
    single { Argon2Factory.create() }
}
