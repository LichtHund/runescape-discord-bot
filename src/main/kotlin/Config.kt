package dev.triumphteam.pvm

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import dev.triumphteam.nebula.ModularApplication
import dev.triumphteam.nebula.container.Container
import dev.triumphteam.nebula.container.inject
import dev.triumphteam.nebula.module.BaseModule
import dev.triumphteam.nebula.module.ModuleFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.decodeFromConfig
import kotlinx.serialization.hocon.encodeToConfig
import java.io.File

public class Config(container: Container) : BaseModule(container) {

    private val application: ModularApplication by inject()

    private val hocon: Hocon = Hocon {
        encodeDefaults = true
    }
    private val renderOptions: ConfigRenderOptions = ConfigRenderOptions.defaults().setOriginComments(false)

    public var data: Data = Data()
        private set
    private val dataFile: File = File(application.applicationFolder, "data.conf")

    init {
        runBlocking {
            loadData()
        }
    }

    private suspend fun loadData() {
        if (!dataFile.exists()) {
            save(data)
        }

        this.data = hocon.decodeFromConfig<Data>(ConfigFactory.parseFile(dataFile))
    }

    public suspend fun save(data: Data): Unit = withContext(Dispatchers.IO) {
        dataFile.writeText(hocon.encodeToConfig(data).root().render(renderOptions))
    }

    @Serializable
    public data class Data(
        public val pvmeCommitId: String = "test"
    )

    public companion object Factory : ModuleFactory<Config> {

        override fun install(container: Container): Config {
            return Config(container)
        }
    }
}
