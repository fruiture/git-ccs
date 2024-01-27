package de.fruiture.cor.ccs.git

import de.fruiture.cor.ccs.semver.Release
import de.fruiture.cor.ccs.semver.Version
import de.fruiture.cor.ccs.semver.Version.Companion.extractVersion
import java.time.ZonedDateTime

class Git(private val sys: SystemCaller) {
    fun getLatestVersion(): Version? {
        return getAllVersionTags().maxOrNull()
    }

    fun getLatestRelease(): Release? {
        return getAllVersionTags().filterIsInstance<Release>().maxOrNull()
    }

    private fun getAllVersionTags() = git(
        listOf(
            "for-each-ref",
            "--merged", "HEAD",
            "--sort=-committerdate",
            "--format=%(refname:short)",
            "refs/tags/*.*.*"
        )
    ).mapNotNull { extractVersion(it) }

    fun getLog(from: Version? = null): List<GitCommit> {
        val arguments = listOf("log", "--format=format:%H %aI%n%B%n", "-z",
            from?.let { "$it..HEAD" } ?: "HEAD"
        )
        val result = git(arguments)

        return result.joinToString("\n").split(Char.MIN_VALUE).mapNotNull {
            Regex(
                "^(\\S+) (\\S+)\\n(.+?)?",
                RegexOption.DOT_MATCHES_ALL
            ).matchEntire(it)?.destructured?.let { (hash, date, msg) ->
                GitCommit(
                    hash = hash,
                    date = ZonedDateTime.parse(date),
                    message = msg.trim()
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
