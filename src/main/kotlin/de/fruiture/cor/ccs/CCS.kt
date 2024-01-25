package de.fruiture.cor.ccs

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import de.fruiture.cor.ccs.git.JvmProcessCaller


class CCS(app: App) : CliktCommand() {
    override fun run() {
        shortHelp(currentContext)
    }

    init {
        subcommands(object : CliktCommand(name = "next") {
            val preRelease by option(
                "-p", "--pre-release",
                help = "create a pre-release version instead of a full release"
            ).flag()

            val identifier by option(
                "-i", "--identifier",
                help = "set the pre-release identifier (default: 'SNAPSHOT') -> '1.2.3-SNAPSHOT.4'"
            ).default("SNAPSHOT")

            override fun run() {
                if (preRelease) {
                    echo(app.getNextPreRelease(identifier), trailingNewline = false)
                } else {
                    echo(app.getNextRelease(), trailingNewline = false)
                }
            }
        },
            object : CliktCommand(name = "log") {
                override fun run() {
                    echo(app.getChangeLog())
                }
            }
        )
    }
}

fun main(args: Array<String>) {
    CCS(App(JvmProcessCaller())).main(args)
}

