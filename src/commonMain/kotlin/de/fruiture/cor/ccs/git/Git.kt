package de.fruiture.cor.ccs.git

import de.fruiture.cor.ccs.semver.Release
import de.fruiture.cor.ccs.semver.Version
import kotlin.jvm.JvmInline

const val RECORD_SEPARATOR = '\u001e'

typealias VersionFilter = (Version) -> Boolean

val any: VersionFilter = { true }

@JvmInline
private value class BeforeFilter(val maxExclusive: Version) : VersionFilter {
    override fun invoke(it: Version) = it < maxExclusive
}

fun before(maxExclusive: Version): VersionFilter = BeforeFilter(maxExclusive)

@JvmInline
private value class UntilFilter(val maxInclusive: Version) : VersionFilter {
    override fun invoke(it: Version) = it <= maxInclusive
}

fun until(maxInclusive: Version): VersionFilter = UntilFilter(maxInclusive)

class Git(private val sys: SystemCaller) {

    fun getLatestVersionTag(filter: VersionFilter = any): VersionTag? {
        return getAllVersionTags(filter).maxOrNull()
    }

    fun getLatestReleaseTag(filter: VersionFilter = any): VersionTag? {
        return getAllVersionTags(filter).filter { it.version is Release }.maxOrNull()
    }

    private fun getAllVersionTags(filter: VersionFilter): List<VersionTag> {
        val tagFilter = { tag: VersionTag -> filter(tag.version) }
        return git(
            listOf(
                "for-each-ref",
                "--merged", "HEAD",
                "--sort=-committerdate",
                "--format=%(refname:short)",
                "refs/tags/*.*.*"
            )
        ).mapNotNull { VersionTag.versionTag(it) }.filter(tagFilter)
    }

    fun getLogX(from: TagName? = null, to: TagName? = null): List<GitCommit> {
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
