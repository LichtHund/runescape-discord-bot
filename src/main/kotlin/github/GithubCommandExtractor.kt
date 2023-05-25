package dev.triumphteam.pvm.github

import dev.kord.core.behavior.reply
import dev.triumphteam.nebula.ModularApplication
import dev.triumphteam.nebula.container.Container
import dev.triumphteam.nebula.container.inject
import dev.triumphteam.nebula.module.BaseModule
import dev.triumphteam.nebula.module.ModuleFactory
import dev.triumphteam.pvm.Config
import dev.triumphteam.pvm.command.PrefixCommandManager
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

public class GithubCommandExtractor(container: Container) : BaseModule(container) {

    private val application: ModularApplication by inject()
    private val commandManager: PrefixCommandManager by inject()
    private val config: Config by inject()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
    }
    private val downloadClient = HttpClient(CIO)
    private val commandsFolder: File by lazy {
        File(application.applicationFolder, "commands").also {
            it.mkdirs()
        }
    }

    init {
        onRegister {
            coroutineScope.launch {
                downloadCommands()
                loadCommands()
            }
        }
    }

    private suspend fun downloadCommands() {

        val commit = client.get("$GITHUB_API/repos/$REPO/commits/master") {
            headers {
                parameter("per_page", 1)
            }
        }.body<Commits>().sha

        if (config.data.pvmeCommitId == commit) return

        // Save if it's more recent
        config.save(config.data.copy(pvmeCommitId = commit))

        // Download the template zip
        val files = client.get("$GITHUB_API/repos/$REPO/contents/commands").body<List<CommandFiles>>()

        files.forEach { file ->
            val folder = File(commandsFolder, file.name)
            println("Downloading '${file.name}' command.")
            downloadClient.get(file.download) {
                headers {
                    append(HttpHeaders.Accept, "application/vnd.github+json")
                    append(HttpHeaders.ContentType, ContentType.Application.Any)
                }
            }.body<ByteReadChannel>().copyAndClose(folder.writeChannel())
        }

        println("All downloads complete")
    }

    private fun loadCommands() {
        val files = commandsFolder.listFiles() ?: return
        files.forEach { file ->
            commandManager.register(file.nameWithoutExtension) { event ->
                event.message.reply {
                    content = file.readText().lines().dropLast(2).joinToString("\n")
                }
            }
        }
    }

    @Serializable
    public data class Commits(
        public val sha: String,
    )

    @Serializable
    public data class CommandFiles(
        public val name: String,
        @SerialName("download_url")
        public val download: String,
    )

    public companion object Factory : ModuleFactory<GithubCommandExtractor> {

        private const val GITHUB_API = "https://api.github.com"
        private const val REPO = "pvme/pvme-guides"

        override fun install(container: Container): GithubCommandExtractor {
            return GithubCommandExtractor(container)
        }
    }
}
