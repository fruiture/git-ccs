package de.fruiture.cor.ccs.git

import de.fruiture.cor.ccs.semver.Release
import de.fruiture.cor.ccs.semver.Version

class Git(private val sys: System) {
    fun getLatestVersion(): Version? {
        return getLatestTagAsVersion(false)
    }

    fun getLatestRelease(): Release? {
        return getLatestTagAsVersion(true) as Release?
    }

    private fun getLatestTagAsVersion(excludePreReleases: Boolean): Version? {
        val result = sys.call(
            "git",
            listOf("describe", "--tags", "--match=\"*.*.*\"")
                    + (if (excludePreReleases) listOf("--exclude \"*-*\"") else emptyList())
                    + listOf("--abbrev=0", "HEAD")
        )
        return if (result.code == 0) return result.stdout.first().let(Version::version)
        else {
            if (result.stderr.first().contains("No tags can describe")) null
            else throw RuntimeException("unexpected result from system call: $result")
        }
    }
}
