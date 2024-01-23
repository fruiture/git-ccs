package de.fruiture.cor.ccs

import de.fruiture.cor.ccs.cc.Type
import de.fruiture.cor.ccs.git.Git
import de.fruiture.cor.ccs.git.ProcessCaller
import de.fruiture.cor.ccs.git.System
import de.fruiture.cor.ccs.semver.ChangeType
import kotlin.system.exitProcess

class App(sys: System) {
    val git = Git(sys)

    val types = mapOf(
        Type("feat") to ChangeType.MINOR,
        Type("fix") to ChangeType.PATCH
    )

    fun getNextRelease(): String {
        val latestVersion = git.getLatestVersion()!!
        val changeType = git.getLog(latestVersion)
            .mapNotNull { it.conventionalCommit }
            .maxOfOrNull { types.getOrDefault(it.type, ChangeType.PATCH) }
            ?: ChangeType.PATCH
        return latestVersion.next(changeType).toString()
    }
}

fun main(args: Array<String>) {
    val app = App(ProcessCaller())
    val cmd = args.toList()

    if (cmd == listOf("next", "release")) {
        println(app.getNextRelease())
    } else {
        java.lang.System.err.println("unknown command $cmd")
        exitProcess(1)
    }
}