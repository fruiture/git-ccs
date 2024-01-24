package de.fruiture.cor.ccs

import de.fruiture.cor.ccs.cc.ConventionalCommitMessage
import de.fruiture.cor.ccs.cc.Type
import de.fruiture.cor.ccs.git.Git
import de.fruiture.cor.ccs.git.GitCommit
import de.fruiture.cor.ccs.git.ProcessCaller
import de.fruiture.cor.ccs.git.System
import de.fruiture.cor.ccs.semver.AlphaNumericIdentifier.Companion.alphanumeric
import de.fruiture.cor.ccs.semver.ChangeType
import de.fruiture.cor.ccs.semver.PreReleaseIdentifier
import de.fruiture.cor.ccs.semver.PreReleaseIdentifier.Companion.identifier
import de.fruiture.cor.ccs.semver.PreReleaseIndicator
import de.fruiture.cor.ccs.semver.Version
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class App(
    sys: System,
    private val types: Map<Type, ChangeType> = mapOf(
        Type("feat") to ChangeType.MINOR,
        Type("fix") to ChangeType.PATCH
    )
) {
    private val git = Git(sys)

    private fun getChangeType(latestVersion: Version) = git.getLog(latestVersion).change()

    private fun List<GitCommit>.change() = mapNotNull(GitCommit::conventionalCommit)
        .maxOfOrNull(::changeType) ?: ChangeType.PATCH

    private fun changeType(message: ConventionalCommitMessage) =
        if (message.hasBreakingChange) ChangeType.MAJOR else types.getOrDefault(message.type, ChangeType.PATCH)

    fun getNextRelease(): String {
        val latestVersion = git.getLatestVersion() ?: return Version.initial.toString()

        val changeType = getChangeType(latestVersion)
        return latestVersion.next(changeType).toString()
    }

    fun getNextPreRelease(label: String? = null): String {
        val identifier = label?.let { identifier(it.alphanumeric) }
        val latestVersion = git.getLatestVersion() ?: return initialPreRelease(identifier).toString()
        val changeType = getChangeType(latestVersion)

        return latestVersion.nextPreRelease(changeType, identifier).toString()
    }

    private fun initialPreRelease(identifier: PreReleaseIdentifier?) =
        Version.initial + PreReleaseIndicator.start(identifier)

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json { explicitNulls = false }

    fun getChangeLog(): String {
        val commits = git.getLatestVersion()?.let { git.getLog(it) } ?: git.getLog()

        return json.encodeToString(commits)
    }
}

internal abstract class Command(
    val description: String,
    private val subcommands: Map<String, Command>
) {
    constructor(
        description: String,
        vararg subcommands: Pair<String, Command>
    ) : this(description, subcommands.toMap())

    private fun help() = object : Command("show help for $description") {
        override fun execute(app: App) {
            println(this@Command.description)
            this@Command.getHelp().forEach { println(it) }
            println("use --help at any point to get help")
        }
    }

    private fun unexpected(key: String) = object : Command("unexpected '$key'") {
        override fun execute(app: App) {
            println(description)
            this@Command.help().execute(app)
        }
    }

    protected open fun getHelp(): List<String> = subcommands.entries.mapIndexed { i, (key, command) ->
        val marker = if (i == 0) "•" else "·"
        listOf("$marker $key — ${command.description}") + command.getHelp().map { "  $it" }
    }.flatten()

    fun subcommand(key: String) = if (key == "--help") help() else getSubcommand(key)

    protected open fun getSubcommand(key: String) =
        subcommands[key] ?: unexpected(key)


    open fun execute(app: App) {
        subcommands.values.first().execute(app)
    }
}

internal data object Root : Command("Conventional Commits & Semantic Versioning",
    "next" to object : Command("compute next version",
        "release" to object : Command("compute next release version (no pre-release) -> 1.2.3") {
            override fun execute(app: App) {
                print(app.getNextRelease())
            }
        },
        "pre-release" to object : Command("compute next pre-release version") {
            override fun getHelp() = listOf(
                "• <pre-release identifier> – alphanumeric string (default: 'SNAPSHOT') -> '1.2.3-SNAPSHOT.4')"
            )

            override fun getSubcommand(key: String) = object : Command(
                "compute pre-release with identifier '$key' -> '1.2.3-$key.4'"
            ) {
                override fun execute(app: App) {
                    print(app.getNextPreRelease(key))
                }
            }

            override fun execute(app: App) {
                print(app.getNextPreRelease())
            }
        }
    ) {},
    "log" to object : Command("get change log since last version (as JSON)") {
        override fun execute(app: App) {
            println(app.getChangeLog())
        }
    }
)

fun main(args: Array<String>) {
    val app = App(ProcessCaller())

    args.toList().fold(Root, Command::subcommand).execute(app)
}