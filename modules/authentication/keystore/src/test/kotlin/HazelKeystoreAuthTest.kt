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

package org.noelware.hazel.testing.modules.authentication.keystore

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.noelware.hazel.configuration.kotlin.dsl.auth.HazelAuthenticationStrategy
import org.noelware.hazel.modules.authentication.keystore.HazelKeystoreAuthentication
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestMethodOrder(MethodOrderer.MethodName::class)
class HazelKeystoreAuthTest {
    @DisplayName("Can we create the noel user or not")
    @Test
    fun test0() {
        assertDoesNotThrow {
            val user = keystore.create("noel", "noelowouwu", listOf())
            assertEquals(user.username, "noel")
        }
    }

    @DisplayName("Can we get the noel in the keystore now")
    @Test
    fun test1() {
        assertDoesNotThrow {
            val user = keystore.get("noel")
            assertNotNull(user)
            assertEquals("noel", user.username)
        }
    }

    @DisplayName("Check if noel has the admin role or not")
    @Test
    fun test2() {
        assertDoesNotThrow {
            assertFalse(keystore.hasRole("noel", "admin"))
        }
    }

    @DisplayName("Can we authenticate the noel user and not the hazel user")
    @Test
    fun test3() {
        assertDoesNotThrow {
            assertTrue(keystore.doAuthenticate("noel", "noelowouwu"))
            assertFalse(keystore.doAuthenticate("hazel", "hazel"))
        }
    }

    @DisplayName("Can we close down the keystore, flush changes, and throw exceptions on close?")
    @Test
    fun test4() {
        keystore.close()
        assertThrows<IllegalStateException>("Keystore is currently closed, cannot do operation [GET_USER noel]") {
            keystore.get("noel")
        }

        assertThrows<IllegalStateException>("Keystore is currently closed, cannot do operation [CREATE_USER hazel]") {
            keystore.create("hazel", "hazel", listOf())
        }

        assertThrows<IllegalStateException>("Keystore is currently closed, cannot do operation [DO_AUTHENTICATE hazel *****]") {
            keystore.doAuthenticate("hazel", "hazel")
        }

        assertThrows<IllegalStateException>("Keystore is currently closed, cannot do operation [HAS_ROLE hazel admin]") {
            keystore.hasRole("hazel", "admin")
        }
    }

    companion object {
        private val keystore: HazelKeystoreAuthentication = HazelKeystoreAuthentication(
            HazelAuthenticationStrategy.Keystore(),
            Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8(),
            Json
        )

        @JvmStatic
        @BeforeAll
        fun init() {
            keystore.init()
        }

        @AfterAll
        @JvmStatic
        fun deleteKeystorePath() {
            File("./hazel.keystore.jks").delete()
        }
    }
}
