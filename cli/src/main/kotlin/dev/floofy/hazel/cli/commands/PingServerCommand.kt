package dev.floofy.hazel.cli.commands

import dev.floofy.hazel.cli.HazelTerminal
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import org.noelware.ai.AiCommand
import org.noelware.ai.AiPhase
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.function.Supplier

object PingServerCommand: AiCommand("ping", "Pings the server with the [HOST] specified.") {
    override fun run(args: List<String>): AiPhase {
        val host = args.firstOrNull()
        if (host == null) {
            HazelTerminal.logError("You must specify a [HOST] to use.")
            return AiPhase.FINISHED
        }

        HazelTerminal.logInfo("Pinging $host...")
        return AiPhase.FINISHED
    }
}
