package de.fruiture.cor.ccs.git

import de.fruiture.cor.ccs.semver.Release
import de.fruiture.cor.ccs.semver.Version
import de.fruiture.cor.ccs.semver.Version.Companion.version
import java.time.ZonedDateTime

class Git(private val sys: SystemCaller) {
    fun getLatestVersion(): Version? {
        return getLatestTagAsVersion(false)
    }

    fun getLatestRelease(): Release? {
        return getLatestTagAsVersion(true) as Release?
    }

    private fun getLatestTagAsVersion(excludePreReleases: Boolean): Version? =
        getOneLatestTag(excludePreReleases)?.let {
            findHighestVersionTag(it, excludePreReleases)
        }

    private fun findHighestVersionTag(candidate: Version, excludePreReleases: Boolean): Version? {
        val tags = git(listOf("tag", "--points-at", candidate.toString()))

        return tags.map(::version).filter {
            if (excludePreReleases) it is Release else true
        }.maxOrNull()
    }

    private fun git(arguments: List<String>, success: (SystemCallResult) -> Boolean = { it.code == 0 }): List<String> {
        val result = sys.call("git", arguments)
        if (success(result)) {
            return result.stdout
        } else {
            throw RuntimeException("unexpected result from system call (git $arguments): $result")
        }
    }

    private fun getOneLatestTag(excludePreReleases: Boolean): Version? {

        val arguments = (
                listOf("describe", "--tags", "--match=*.*.*")
                        + (if (excludePreReleases) listOf("--exclude=*-*") else emptyList())
                        + listOf("--abbrev=0", "HEAD")
                )

        val result = git(arguments) {
            it.code == 0 || it.stderr.first().contains("No tags can describe")
        }

        return result.firstOrNull()?.let(::version)
    }

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
}
