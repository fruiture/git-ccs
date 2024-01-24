package de.fruiture.cor.ccs

import de.fruiture.cor.ccs.cc.Type
import de.fruiture.cor.ccs.git.Git
import de.fruiture.cor.ccs.git.ProcessCaller
import de.fruiture.cor.ccs.git.System
import de.fruiture.cor.ccs.semver.AlphaNumericIdentifier.Companion.alphanumeric
import de.fruiture.cor.ccs.semver.ChangeType
import de.fruiture.cor.ccs.semver.PreReleaseIdentifier.Companion.identifier
import de.fruiture.cor.ccs.semver.PreReleaseIndicator
import de.fruiture.cor.ccs.semver.Version
import kotlin.system.exitProcess

class App(sys: System) {
    val git = Git(sys)

    val types = mapOf(
        Type("feat") to ChangeType.MINOR,
        Type("fix") to ChangeType.PATCH
    )

    fun getNextRelease(): String {
        val latestVersion = git.getLatestVersion() ?: return Version.initial.toString()

        val changeType = getChangeType(latestVersion)
        return latestVersion.next(changeType).toString()
    }

    private fun getChangeType(latestVersion: Version) = (git.getLog(latestVersion)
        .mapNotNull { it.conventionalCommit }
        .maxOfOrNull { types.getOrDefault(it.type, ChangeType.PATCH) }
        ?: ChangeType.PATCH)

    fun getNextPreRelease(label: String? = null): String {
        val identifier = label?.let { identifier(it.alphanumeric) }
        val latestVersion = git.getLatestVersion() ?: return (
                Version.initial + PreReleaseIndicator.start(identifier)
                ).toString()
        val changeType = getChangeType(latestVersion)

        return latestVersion.nextPreRelease(
            changeType,
            identifier
        ).toString()
    }
}

fun main(args: Array<String>) {
    val app = App(ProcessCaller())
    val cmd = args.toList()

    if (cmd == listOf("next", "release")) {
        println(app.getNextRelease())
    } else if (cmd.take(2) == listOf("next", "pre-release")) {
        println(app.getNextPreRelease(cmd.getOrNull(2)))
    } else {
        java.lang.System.err.println("unknown command $cmd")
        exitProcess(1)
    }
}