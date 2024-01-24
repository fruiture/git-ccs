package de.fruiture.cor.ccs.cmd

internal abstract class Command<A>(
    val description: String,
    private val subcommands: Map<String, Command<A>>
) {
    constructor(
        description: String,
        vararg subcommands: Pair<String, Command<A>>
    ) : this(description, subcommands.toMap())

    private fun help(to: IO.() -> Output = IO::out): Command<A> = object : Command<A>("show help for $description") {
        override fun execute(app: A, io: IO) {
            val out = io.to()
            out.println(this@Command.description)
            this@Command.getHelp().forEach(out::println)
            out.println("use --help at any point to get help")
        }
    }

    private fun unexpected(key: String): Command<A> = object : Command<A>("unexpected '$key'") {
        override fun execute(app: A, io: IO) {
            io.err.println(description)
            this@Command.help(IO::err).execute(app, io)
        }
    }

    protected open fun getHelp(): List<String> = subcommands.entries.mapIndexed { i, (key, command) ->
        val marker = if (i == 0) "•" else "·"
        listOf("$marker $key — ${command.description}") + command.getHelp().map { "  $it" }
    }.flatten()

    private fun subcommand(key: String) = if (key == "--help") help() else getSubcommand(key)

    protected open fun getSubcommand(key: String) =
        subcommands[key] ?: unexpected(key)

    open fun execute(app: A, io: IO) {
        subcommands.values.first().execute(app, io)
    }

    fun getCommand(args: List<String>) = args.fold(this, Command<A>::subcommand)
}