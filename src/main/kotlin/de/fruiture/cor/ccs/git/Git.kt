package de.fruiture.cor.ccs.git

import de.fruiture.cor.ccs.semver.Release
import de.fruiture.cor.ccs.semver.Version
import java.time.ZonedDateTime

class Git(private val sys: System) {
    fun getLatestVersion(): Version? {
        return getLatestTagAsVersion(false)
    }

    fun getLatestRelease(): Release? {
        return getLatestTagAsVersion(true) as Release?
    }

    private fun getLatestTagAsVersion(excludePreReleases: Boolean): Version? {
        val arguments = (listOf("describe", "--tags", "--match=*.*.*")
                + (if (excludePreReleases) listOf("--exclude=*-*") else emptyList())
                + listOf("--abbrev=0", "HEAD"))
        val result = sys.call("git", arguments)

        return if (result.code == 0) return result.stdout.first().let(Version::version)
        else {
            if (result.stderr.first().contains("No tags can describe")) null
            else throw RuntimeException("unexpected result from system call (git $arguments): $result")
        }
    }

    fun getLog(from: Version): List<GitCommit> {
        val arguments = listOf("log", "--format=format:'%H %aI%n%B%n'", "-z", "${from}..HEAD")
        val result = sys.call("git", arguments)

        if (result.code != 0) {
            throw RuntimeException("unexpected result from system call (gt $arguments): $result")
        }

        return result.stdout.joinToString("\n").split(Char.MIN_VALUE).mapNotNull {
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
