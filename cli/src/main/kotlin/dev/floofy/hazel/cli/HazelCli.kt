package dev.floofy.hazel.cli

import dev.floofy.hazel.cli.commands.GenerateConfigCommand
import dev.floofy.hazel.cli.commands.PingServerCommand
import org.noelware.ai.AiCommand
import org.noelware.ai.AiPhase

object HazelCli: AiCommand(
    "hazel",
    "Minimal, simple, and open source content delivery network made in Kotlin."
) {
    init {
        addSubcommands(
            GenerateConfigCommand,
            PingServerCommand
        )
    }

    override fun run(args: List<String>): AiPhase {
        // do thing here
        return AiPhase.FINISHED
    }
}
