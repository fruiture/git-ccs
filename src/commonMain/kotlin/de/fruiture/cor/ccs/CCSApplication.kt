package de.fruiture.cor.ccs

import de.fruiture.cor.ccs.git.*
import de.fruiture.cor.ccs.semver.PreRelease
import de.fruiture.cor.ccs.semver.PreReleaseIndicator.Strategy
import de.fruiture.cor.ccs.semver.Release
import de.fruiture.cor.ccs.semver.Version
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CCSApplication(
    private val git: Git
) {
    private fun getChangeType(latestVersion: VersionTag, mapping: ChangeMapping) =
        mapping.of(git.getLogX(latestVersion.tag))

    fun getNextRelease(changeMapping: ChangeMapping = ChangeMapping()): Release {
        val latestVersion = git.getLatestVersionTag() ?: return Version.initial

        val changeType = getChangeType(latestVersion, changeMapping)
        return latestVersion.version.next(changeType)
    }

    fun getNextPreRelease(strategy: Strategy, changeMapping: ChangeMapping = ChangeMapping()): PreRelease {
        val latestVersion = git.getLatestVersionTag() ?: return initialPreRelease(strategy)
        val changeType = getChangeType(latestVersion, changeMapping)

        return latestVersion.version.nextPreRelease(changeType, strategy)
    }

    private fun initialPreRelease(strategy: Strategy) =
        Version.initial + strategy.start()

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json { explicitNulls = false }

    fun getChangeLogJson(release: Boolean = false, before: Version? = null): String {
        val commits = getChanges(release, before)

        return json.encodeToString(commits)
    }

    private fun getChanges(release: Boolean, before: Version? = null): List<GitCommit> {
        val from = getLatest(release, optionalFilterBefore(before))
        val to = if (before == null) null else getLatest(false, until(before))

        return git.getLogX(from = from?.tag, to = to?.tag)
    }

    fun getLatestVersion(release: Boolean = false, before: Version? = null): String? =
        getLatest(release, optionalFilterBefore(before))?.version?.toString()

    private fun optionalFilterBefore(before: Version?) = if (before == null) any else before(before)

    private fun getLatest(release: Boolean, filter: VersionFilter): VersionTag? {
        return if (release) git.getLatestReleaseTag(filter)
        else git.getLatestVersionTag(filter)
    }

    fun getChangeLogMarkdown(
        release: Boolean = false,
        target: Version? = null,
        sections: Sections = Sections.default(),
        level: Int = 2
    ) =
        sequence {
            val hl = "#".repeat(level)

            val commits = getChanges(release, target).map { it.conventional }

            val breakingChanges = commits.mapNotNull { it.breakingChange }
            if (breakingChanges.isNotEmpty()) {
                yield("$hl ${sections.breakingChanges}\n")

                breakingChanges.forEach {
                    yield("* $it")
                }
                yield("")
            }

            sections.forEach { headline, types ->
                val selectedCommits = commits.filter { it.type in types }
                if (selectedCommits.isNotEmpty()) {
                    yield("$hl $headline\n")

                    selectedCommits.forEach {
                        yield("* ${it.description}")
                    }
                    yield("")
                }
            }
        }.joinToString("\n")
}
