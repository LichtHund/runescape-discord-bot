package dev.triumphteam.pvm.command

import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.create.embed
import dev.triumphteam.nebula.container.Container
import dev.triumphteam.nebula.container.inject
import dev.triumphteam.nebula.module.BaseModule
import dev.triumphteam.nebula.module.ModuleFactory
import java.util.concurrent.ConcurrentHashMap

public class PrefixCommandManager(container: Container) : BaseModule(container) {

    private val kord: Kord by inject()

    private val commands: MutableMap<String, suspend (MessageCreateEvent) -> Unit> = ConcurrentHashMap()

    init {
        onRegister {
            kord.on(consumer = ::onMessageReceive)
        }
    }

    public fun register(name: String, block: suspend (event: MessageCreateEvent) -> Unit) {
        commands[name] = block
    }

    private suspend fun onMessageReceive(event: MessageCreateEvent) {
        val content = event.message.content.split(" ")
        // We don't care about commands with spaces for now
        if (content.size > 1) return

        val (commandString) = content
        val index = commandString.indexOf(PREFIX)

        if (index != PREFIX_INDEX) return

        val name = commandString.substring(SUBSTRING_INDEX)

        commands[name]?.invoke(event) ?: run {
            event.message.reply {
                embed {
                    description = "Unknown command, `!help` for more info."
                }
            }

            return
        }
    }

    public companion object Factory : ModuleFactory<PrefixCommandManager> {

        private const val PREFIX = '!'
        private const val PREFIX_INDEX = 0
        private const val SUBSTRING_INDEX = 1

        override fun install(container: Container): PrefixCommandManager {
            return PrefixCommandManager(container)
        }
    }
}
