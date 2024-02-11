package de.fruiture.cor.ccs

import VERSION
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import de.fruiture.cor.ccs.cc.Type
import de.fruiture.cor.ccs.semver.ChangeType
import de.fruiture.cor.ccs.semver.PreReleaseIndicator
import de.fruiture.cor.ccs.semver.PreReleaseIndicator.Strategy.Companion.counter
import de.fruiture.cor.ccs.semver.PreReleaseIndicator.Strategy.Companion.static
import de.fruiture.cor.ccs.semver.Version.Companion.version

private class MappingOptions : OptionGroup(
    name = "Version Bumping Options",
    help = "adjust which conventional commit types have which semantic version effect\n\n" +
            " - each option takes a list (comma-separated) of commit types,\n" +
            " - 'default' will match all other commit types,\n" +
            " - 'none' will match commits that are not valid conventional commits"
) {
    val noChangeTypes by option(
        "-n", "--none",
        help = "commit types that should trigger no version change, empty by default"
    ).convert { Type(it) }.split(",").default(emptyList())

    val patchTypes by option(
        "-p", "--patch",
        help = "commit types that should trigger a patch level change, default: 'default', i.e." +
                "usually all commits trigger at least a patch level change"
    ).convert { Type(it) }.split(",").default(emptyList())

    val minorTypes by option(
        "-m", "--minor",
        help = "commit types that should trigger a minor level change, by default: 'feat'"
    ).convert { Type(it) }.split(",").default(emptyList())

    val majorTypes by option(
        "-M", "--major",
        help = "commit types that should trigger a major level change, empty by default"
    ).convert { Type(it) }.split(",").default(emptyList())

    fun getMapping() = ChangeMapping()
        .add(ChangeType.NONE, noChangeTypes)
        .add(ChangeType.PATCH, patchTypes)
        .add(ChangeType.MINOR, minorTypes)
        .add(ChangeType.MAJOR, majorTypes)
}


private class LogOptions : OptionGroup() {
    val release by option(
        "-r", "--release",
        help = "only consider releases (ignore pre-releases)"
    ).flag()

    val target by option(
        "-t", "--target",
        help = "look for latest version before a given version (instead of HEAD)"
    ).convert { version(it) }
}

class CLI(app: CCSApplication) : NoOpCliktCommand(
    name = "git-ccs",
    help = "Conventional Commits & Semantic Versioning Utility for Git Repositories"
) {
    init {
        versionOption(VERSION)

        subcommands(object : NoOpCliktCommand(
            name = "next",
            help = "compute the next semantic version based on changes since the last version tag",
        ) {
            init {
                subcommands(
                    object : CliktCommand(
                        name = "release",
                        help = "create a release version, e.g. 1.2.3"
                    ) {
                        val mappingOptions by MappingOptions()

                        override fun run() {
                            echo(
                                app.getNextRelease(mappingOptions.getMapping()),
                                trailingNewline = false
                            )
                        }
                    },

                    object : CliktCommand(
                        name = "pre-release",
                        help = "create a pre-release version, e.g. 1.2.3-RC.1"
                    ) {
                        val strategy by mutuallyExclusiveOptions(
                            option(
                                "-c", "--counter",
                                help = "(default) add a numeric release candidate counter, e.g. 1.2.3-RC.3"
                            ).flag().convert { counter() },

                            option(
                                "-s", "--static",
                                help = "just append a fixed pre-release identifier 'SNAPSHOT' (maven convention), e.g. 1.2.3-SNAPSHOT"
                            ).flag().convert { static() },

                            option(
                                "-f", "--format",
                                help = "specify a pattern, like: RC.1.DEV, where 'RC.1' would become a counter and 'DEV' would be static"
                            ).convert {
                                PreReleaseIndicator.Strategy.deduct(it)
                            }
                        ).single().default(counter())

                        val mappingOptions by MappingOptions()

                        override fun run() {
                            echo(
                                message = app.getNextPreRelease(
                                    strategy = strategy,
                                    changeMapping = mappingOptions.getMapping()
                                ), trailingNewline = false
                            )
                        }
                    })
            }
        }, object : CliktCommand(
            name = "log",
            help = "get commit log since last version as machine-friendly JSON representation"
        ) {
            val logOptions by LogOptions()

            override fun run() {
                echo(
                    app.getChangeLogJson(release = logOptions.release, before = logOptions.target),
                    trailingNewline = false
                )
            }
        }, object : CliktCommand(
            name = "latest",
            help = "find the latest version (release or pre-release)"
        ) {
            val logOptions by LogOptions()

            override fun run() {
                val latestVersion = app.getLatestVersion(release = logOptions.release, before = logOptions.target)
                if (latestVersion != null) {
                    echo(latestVersion, trailingNewline = false)
                } else {
                    echo("no version found", err = true)
                    throw ProgramResult(1)
                }
            }
        }, object : CliktCommand(
            name = "changes",
            help = "summarize changes (see `log`) as markdown document (changelog)"
        ) {
            val logOptions by LogOptions()

            val sections by option(
                "-s", "--section",
                help = "specify section headlines and their types, types can be comma-separated'\n\n" +
                        "e.g. (default) -s 'Features=feat' -s 'Bugfixes=fix'"
            ).splitPair().convert { (hl, types) ->
                hl to types.split(',').map { Type(it) }.toSet()
            }.multiple().toMap()

            val breakingChanges by option(
                "-b", "--breaking-changes",
                help = "adjust the headline for the breaking changes section (default 'Breaking Changes')"
            ).default("Breaking Changes")

            val level by option(
                "-l", "--level",
                help = "level of headlines in markdown, default is 2: ## Headline"
            ).int().default(2).check(message = "must be in 1 .. 7") { it in 1..7 }

            override fun run() {
                val config = if (sections.isEmpty()) Sections.default() else Sections(sections)

                echo(
                    app.getChangeLogMarkdown(
                        release = logOptions.release,
                        target = logOptions.target,
                        sections = config.setBreakingChanges(breakingChanges),
                        level = level
                    ),
                    trailingNewline = false
                )
            }
        })
    }
}
