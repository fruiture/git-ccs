package de.fruiture.cor.ccs

import de.fruiture.cor.ccs.cmd.Command
import de.fruiture.cor.ccs.cmd.IO
import de.fruiture.cor.ccs.cmd.JvmIO
import de.fruiture.cor.ccs.git.JvmProcessCaller

internal data object CLI : Command<App>("Conventional Commits & Semantic Versioning",
    "next" to object : Command<App>("compute next version",
        "release" to object : Command<App>("compute next release version (no pre-release) -> 1.2.3") {
            override fun execute(app: App, io: IO) {
                io.out.print(app.getNextRelease())
            }
        },
        "pre-release" to object : Command<App>("compute next pre-release version") {
            override fun getHelp() = listOf(
                "• <pre-release identifier> – alphanumeric string (default: 'SNAPSHOT') -> '1.2.3-SNAPSHOT.4')"
            )

            override fun getSubcommand(key: String) = object : Command<App>(
                "compute pre-release with identifier '$key' -> '1.2.3-$key.4'"
            ) {
                override fun execute(app: App, io: IO) {
                    io.out.print(app.getNextPreRelease(key))
                }
            }

            override fun execute(app: App, io: IO) {
                io.out.print(app.getNextPreRelease())
            }
        }
    ) {},
    "log" to object : Command<App>("get change log since last version (as JSON)") {
        override fun execute(app: App, io: IO) {
            io.out.println(app.getChangeLog())
        }
    }
) {
    @JvmStatic
    fun main(args: Array<String>) {
        val app = App(JvmProcessCaller())
        val io = JvmIO

        try {
            CLI.getCommand(args.toList()).execute(app, io)
        } catch (e: Exception) {
            e.message?.let(io.err::println)
            e::class.qualifiedName?.let(io.err::println)
            e.stackTrace.first().toString().let(io.err::println)
        }
    }
}

