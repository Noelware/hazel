package dev.floofy.hazel.cli

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.terminal.Terminal
import java.util.TimerTask

typealias Disposable = () -> Unit

/**
 * Represents the terminal utility to handle stdin actions and more.
 */
object HazelTerminal {
    private val terminal = Terminal()

    fun readFromStdin(): String = ""

    fun logInfo(message: String) {
        val level = bold(cyan("INFO "))
        terminal.println("$level :: $message")
    }

    fun logWarn(message: String) {
        val level = bold(yellow("WARN "))
        terminal.println("$level :: $message")
    }

    fun logError(message: String) {
        val level = bold(red("ERROR"))
        terminal.println("$level :: $message")
    }
}
