package de.fruiture.cor.ccs

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.default
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
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
                help = "set the pre-release identifier (default: 'SNAPSHOT')"
            ).convert { it.alphanumeric }.default(DEFAULT_PRERELEASE)

            val strategy by mutuallyExclusiveOptions(
                option(
                    "-c", "--counter",
                    help = "(default) add a numeric counter to the pre-release identifier -> 1.2.3-SNAPSHOT.3"
                ).flag().convert { { counter(identifier) } },
                option(
                    "-s", "--static",
                    help = "always keep the pre-release identifier static -> 1.2.3-SNAPSHOT"
                ).flag().convert { { static(identifier) } }
            ).single().default { counter(identifier) }

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

