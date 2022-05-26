package dev.floofy.hazel.cli.commands.plugins

import org.noelware.ai.AiCommand
import org.noelware.ai.AiPhase

object RemovePluginCommand: AiCommand("remove-plugin", "Removes a [PLUGIN]") {
    override fun run(args: List<String>): AiPhase {
        return AiPhase.FINISHED
    }
}
