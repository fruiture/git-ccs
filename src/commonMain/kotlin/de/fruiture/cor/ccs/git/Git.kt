package de.fruiture.cor.ccs.git

import de.fruiture.cor.ccs.semver.Release
import de.fruiture.cor.ccs.semver.Version

const val RECORD_SEPARATOR = '\u001e'

class Git(private val sys: SystemCaller) {

    fun getLatestVersionTag(before: Version? = null): VersionTag? {
        return getAllVersionTags(before).maxOrNull()
    }

    fun getLatestReleaseTag(before: Version? = null): VersionTag? {
        return getAllVersionTags(before).filter { it.version is Release }.maxOrNull()
    }

    private fun getAllVersionTags(before: Version?): List<VersionTag> {
        val filter = before?.let { { tag: VersionTag -> tag.version < it } } ?: { true }
        return git(
            listOf(
                "for-each-ref",
                "--merged", "HEAD",
                "--sort=-committerdate",
                "--format=%(refname:short)",
                "refs/tags/*.*.*"
            )
        ).mapNotNull { VersionTag.versionTag(it) }.filter(filter)
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
                    date = ZonedDateTime(date),
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
