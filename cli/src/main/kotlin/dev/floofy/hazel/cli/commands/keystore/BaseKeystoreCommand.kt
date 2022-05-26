package dev.floofy.hazel.cli.commands.keystore

import org.noelware.ai.AiCommand
import org.noelware.ai.AiPhase

abstract class BaseKeystoreCommand(name: String, help: String = "", usage: String = ""): AiCommand(name, help, usage) {
    abstract fun run(): AiPhase

    fun getKeystore(): Any = 0
}
