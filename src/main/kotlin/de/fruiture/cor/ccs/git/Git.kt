package de.fruiture.cor.ccs.git

import de.fruiture.cor.ccs.semver.Release
import de.fruiture.cor.ccs.semver.Version
import de.fruiture.cor.ccs.semver.Version.Companion.extractVersion
import java.time.ZonedDateTime

const val RECORD_SEPARATOR = '\u001e'

class Git(private val sys: SystemCaller) {
    fun getLatestVersion(before: Version? = null): Version? {
        return getAllVersionTags(before).maxOrNull()
    }

    fun getLatestRelease(before: Version? = null): Release? {
        return getAllVersionTags(before).filterIsInstance<Release>().maxOrNull()
    }

    private fun getAllVersionTags(before: Version?): List<Version> {
        val filter = before?.let { { v: Version -> v < it } } ?: { true }
        return git(
            listOf(
                "for-each-ref",
                "--merged", "HEAD",
                "--sort=-committerdate",
                "--format=%(refname:short)",
                "refs/tags/*.*.*"
            )
        ).mapNotNull { extractVersion(it) }.filter(filter)
    }

    fun getLog(from: Version? = null, to: Version? = null): List<GitCommit> {
        val end = to?.toString() ?: "HEAD"
        val arguments = listOf("log", "--format=format:%H %aI%n%B%n%x1E",
            from?.let { "$it..$end" } ?: end
        )
        val result = git(arguments)

        return result.joinToString("\n").split(RECORD_SEPARATOR).map(String::trim).mapNotNull {
            Regex("^(\\S+) (\\S+)\\n").matchAt(it, 0)?.let { match ->
                val (hash, date) = match.destructured
                GitCommit(
                    hash = hash,
                    date = ZonedDateTime.parse(date),
                    message = it.substring(match.range.last + 1)
                )
            }
        }
    }

    private fun git(arguments: List<String>, success: (SystemCallResult) -> Boolean = { it.code == 0 }): List<String> {
        val result = sys.call("git", arguments)
        if (success(result)) {
            return result.stdout
        } else {
            throw RuntimeException("unexpected result from system call (git $arguments): $result")
        }
    }
}
