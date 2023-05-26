package dev.triumphteam.pvm.github

import dev.kord.common.Color
import dev.kord.common.entity.DiscordEmbed
import dev.kord.common.entity.optional.value
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.create.embed
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

    private val json = Json {
        ignoreUnknownKeys = true
    }
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
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
            val fileLines = file.readText()
                .lines()
                .map { it.trim() }
                .filter { it != "." }
                .filterNot(String::isEmpty)

            val (type, value) = PARSER_REGEX.matchEntire(fileLines.last())?.destructured ?: run {
                println("Ignoring ${file.name}, cuz ????")
                return@forEach
            }

            when (type) {
                "img" -> {
                    commandManager.register(file.nameWithoutExtension, ImageCommand(value))
                    return@forEach
                }

                "embed" -> {
                    val json = fileLines.dropLast(1).joinToString("\n")
                    val test = Json.decodeFromString<EmbedHolder>(json)
                    commandManager.register(file.nameWithoutExtension, EmbedCommand(test.embed))
                }

                else -> {
                    println("No idea what this is, check -> ${file.name}")
                    return@forEach
                }
            }
        }

        println("ALL JSON STRINGS FOUND")
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

    @Serializable
    public data class EmbedHolder(public val embed: DiscordEmbed)

    public interface ParsedCommand : suspend (MessageCreateEvent) -> Unit

    public class EmbedCommand(private val embed: DiscordEmbed) : ParsedCommand {

        override suspend fun invoke(event: MessageCreateEvent) {
            event.message.reply {
                embed {
                    this.url = embed.url.value
                    this.description = embed.description.value

                    embed.author.value?.let { author ->
                        author {
                            this.name = author.name.value
                            this.url = author.url.value
                            this.icon = author.url.value
                        }
                    }

                    this.color = embed.color.value?.let { Color(it) }

                    embed.fields.value?.forEach { field ->
                        field {
                            this.value = field.value
                            this.name = field.name
                            this.inline = field.inline.value
                        }
                    }

                    embed.footer.value?.let { footer ->
                        footer {
                            this.icon = footer.iconUrl.value
                            this.text = footer.text
                        }
                    }

                    embed.image.value?.let { image -> this.image = image.url.value }

                    embed.thumbnail.value?.let { thumbnail ->
                        thumbnail {
                            this.url = thumbnail.url.value ?: ""
                        }
                    }

                    this.timestamp = embed.timestamp.value
                }
            }
        }
    }

    public class ImageCommand(private val image: String) : ParsedCommand {

        override suspend fun invoke(event: MessageCreateEvent) {
            event.message.reply {
                embed {
                    this.image = this@ImageCommand.image
                }
            }
        }
    }

    public companion object Factory : ModuleFactory<GithubCommandExtractor> {

        private val PARSER_REGEX = "\\.(?<type>\\w+):(?<value>.*)".toRegex()
        private const val GITHUB_API = "https://api.github.com"
        private const val REPO = "pvme/pvme-guides"

        override fun install(container: Container): GithubCommandExtractor {
            return GithubCommandExtractor(container)
        }
    }
}
