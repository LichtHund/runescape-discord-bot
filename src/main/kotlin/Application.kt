package dev.triumphteam.pvm

import dev.triumphteam.nebula.module.install
import dev.triumphteam.pvm.command.PrefixCommandManager
import dev.triumphteam.pvm.core.KordApplication
import dev.triumphteam.pvm.github.GithubCommandExtractor

public fun KordApplication.module() {

    install(Config)
    install(PrefixCommandManager)
    install(GithubCommandExtractor)
}
