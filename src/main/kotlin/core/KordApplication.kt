package dev.triumphteam.pvm.core

import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import dev.triumphteam.nebula.ModularApplication
import dev.triumphteam.nebula.container.Container
import dev.triumphteam.nebula.container.registry.GlobalInjectionRegistry
import dev.triumphteam.nebula.container.registry.InjectionRegistry
import dev.triumphteam.nebula.registerable.Registerable
import dev.triumphteam.pvm.module
import kotlinx.coroutines.runBlocking
import java.io.File

public class KordApplication(
    private val kord: Kord,
    private val module: KordApplication.() -> Unit
) : ModularApplication {

    override val applicationFolder: File = File("data")
    override val key: String = "pvm"

    override val parent: Container? = null
    override val registry: InjectionRegistry = GlobalInjectionRegistry

    init {
        applicationFolder.mkdirs()
    }

    @OptIn(PrivilegedIntent::class)
    override fun onStart() {
        registry.put(ModularApplication::class.java, this)
        registry.put(Kord::class.java, kord)
        module()
        registry.instances.values.filterIsInstance<Registerable>().forEach(Registerable::register)
        runBlocking {
            kord.login {
                intents = Intents(Intent.Guilds, Intent.DirectMessages, Intent.GuildMessages, Intent.MessageContent)
            }
        }
    }

    override fun onStop() {}
}

public fun main(args: Array<String>): Unit = runBlocking {
    KordApplication(Kord(args[0]), module = KordApplication::module).onStart()
}
