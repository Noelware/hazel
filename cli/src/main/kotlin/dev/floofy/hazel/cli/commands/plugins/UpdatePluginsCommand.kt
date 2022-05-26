package dev.floofy.hazel.cli.commands.plugins

import org.noelware.ai.AiCommand
import org.noelware.ai.AiPhase

object UpdatePluginsCommand: AiCommand("update-plugins", "Updates all the plugins that it can find.") {
    override fun run(args: List<String>): AiPhase {
        return AiPhase.FINISHED
    }
}
