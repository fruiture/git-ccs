package de.fruiture.cor.ccs

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import de.fruiture.cor.ccs.git.JvmProcessCaller
import de.fruiture.cor.ccs.semver.AlphaNumericIdentifier.Companion.alphanumeric
import de.fruiture.cor.ccs.semver.PreReleaseIndicator.Strategy.Companion.DEFAULT_PRERELEASE
import de.fruiture.cor.ccs.semver.PreReleaseIndicator.Strategy.Companion.counter
import de.fruiture.cor.ccs.semver.PreReleaseIndicator.Strategy.Companion.static


class CCS(app: App) : CliktCommand() {
    override fun run() = Unit

    init {
        subcommands(object : CliktCommand(
            name = "next",
            help = "compute the next version based on changes since the last tagged version"
        ) {
            val preRelease by option(
                "-p", "--pre-release",
                help = "create a pre-release version instead of a release version"
            ).flag()

            val identifier by option(
                "-i", "--identifier",
                help = "set the pre-release identifier (default: 'SNAPSHOT') -> '1.2.3-SNAPSHOT.4'"
            ).default(DEFAULT_PRERELEASE.toString())

            val strategy by option().switch(
                "--counter" to { counter(identifier.alphanumeric) },
                "--static" to { static(identifier.alphanumeric) }
            ).default({ counter(identifier.alphanumeric) })

            override fun run() {
                if (preRelease)
                    echo(app.getNextPreRelease(strategy()), trailingNewline = false)
                else
                    echo(app.getNextRelease(), trailingNewline = false)
            }
        }, object : CliktCommand(name = "log") {
            override fun run() {
                echo(app.getChangeLog())
            }
        })
    }
}

fun main(args: Array<String>) {
    CCS(App(JvmProcessCaller())).main(args)
}

