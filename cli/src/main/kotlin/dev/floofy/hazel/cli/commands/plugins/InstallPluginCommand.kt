package dev.floofy.hazel.cli.commands.plugins

import dev.floofy.hazel.cli.HazelTerminal
import org.noelware.ai.AiCommand
import org.noelware.ai.AiPhase

object InstallPluginCommand: AiCommand("install-plugin", "Installs a plugin and outputs it in [DEST].") {
    override fun run(args: List<String>): AiPhase {
        val pluginName = args.firstOrNull()
        if (pluginName == null) {
            HazelTerminal.logError("Missing plugin name!")
            return AiPhase.FINISHED
        }

        return AiPhase.FINISHED
    }
}
