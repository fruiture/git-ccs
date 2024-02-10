package de.fruiture.cor.ccs

import de.fruiture.cor.ccs.git.Git
import de.fruiture.cor.ccs.git.any
import de.fruiture.cor.ccs.git.before
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
    private fun getChangeType(latestVersion: Version, mapping: ChangeMapping) =
        mapping.of(git.getLog(latestVersion))

    fun getNextRelease(changeMapping: ChangeMapping = ChangeMapping()): Release {
        val latestVersion = git.getLatestVersionTag()?.version ?: return Version.initial

        val changeType = getChangeType(latestVersion, changeMapping)
        return latestVersion.next(changeType)
    }

    fun getNextPreRelease(strategy: Strategy, changeMapping: ChangeMapping = ChangeMapping()): PreRelease {
        val latestVersion = git.getLatestVersionTag()?.version ?: return initialPreRelease(strategy)
        val changeType = getChangeType(latestVersion, changeMapping)

        return latestVersion.nextPreRelease(changeType, strategy)
    }

    private fun initialPreRelease(strategy: Strategy) =
        Version.initial + strategy.start()

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json { explicitNulls = false }

    fun getChangeLogJson(release: Boolean = false, before: Version? = null): String {
        val commits = getChanges(release, before)

        return json.encodeToString(commits)
    }

    private fun getChanges(release: Boolean, before: Version? = null) =
        getLatest(release, before)?.let { git.getLog(from = it, to = before) } ?: git.getLog(to = before)

    fun getLatestVersion(release: Boolean = false, before: Version? = null): String? =
        getLatest(release, before)?.toString()

    private fun getLatest(release: Boolean, before: Version? = null): Version? {
        val filter = if (before == null) any else before(before)
        return if (release) git.getLatestReleaseTag(filter)?.version
        else git.getLatestVersionTag(filter)?.version
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
