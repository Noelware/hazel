/*
 * 🪶 hazel: Minimal, simple, and open source content delivery network made in Kotlin
 * Copyright 2022 Noel <cutie@floofy.dev>
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

@file:Suppress("UNUSED")

package dev.floofy.hazel.tests

import de.mkammerer.argon2.Argon2Factory
import dev.floofy.hazel.core.KeystoreWrapper
import dev.floofy.hazel.data.KeystoreConfig
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class KeystoreWrapperTests: DescribeSpec({
    val keystore = KeystoreWrapper(
        KeystoreConfig(
            file = "./owo.jks",
            password = "owo da uwu"
        ),
        Argon2Factory.create(), true
    )

    // The test order is so for each test that is executed, it will run
    // based off the order from the `describe` spec. If this wasn't here,
    // then this would happen: https://haste.red-panda.red/omipoxehid.bash
    testOrder = TestCaseOrder.Sequential

    beforeSpec {
        keystore.init()
    }

    afterSpec {
        keystore.close()
    }

    describe("dev.floofy.hazel.tests.KeystoreWrapper") {
        it("should not contain the 'noel' user in keystore") {
            keystore.closed shouldNotBe true
            keystore.checkIfValid("noel", "owo da uwu") shouldBe false
        }

        it("should add the noel user to the keystore") {
            keystore.closed shouldNotBe true

            shouldNotThrow<Exception> {
                keystore.addUser("noel", "owo da uwu")
            }

            keystore.checkIfValid("noel", "owo da uwu") shouldBe true
        }
    }
})
