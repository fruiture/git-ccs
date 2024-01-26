package de.fruiture.cor.ccs

import de.fruiture.cor.ccs.git.Git
import de.fruiture.cor.ccs.semver.PreReleaseIndicator.Strategy
import de.fruiture.cor.ccs.semver.Version
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class App(
    private val git: Git
) {
    private fun getChangeType(latestVersion: Version, mapping: ChangeMapping) =
        mapping.of(git.getLog(latestVersion))

    fun getNextRelease(changeMapping: ChangeMapping = ChangeMapping()): String {
        val latestVersion = git.getLatestVersion() ?: return Version.initial.toString()

        val changeType = getChangeType(latestVersion, changeMapping)
        return latestVersion.next(changeType).toString()
    }

    fun getNextPreRelease(strategy: Strategy, changeMapping: ChangeMapping = ChangeMapping()): String {
        val latestVersion = git.getLatestVersion() ?: return initialPreRelease(strategy).toString()
        val changeType = getChangeType(latestVersion, changeMapping)

        return latestVersion.nextPreRelease(changeType, strategy).toString()
    }

    private fun initialPreRelease(strategy: Strategy) =
        Version.initial + strategy.start()

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json { explicitNulls = false }

    fun getChangeLogJson(release: Boolean = false): String {
        val commits = getChanges(release)

        return json.encodeToString(commits)
    }

    private fun getChanges(release: Boolean) = getLatest(release)?.let { git.getLog(it) } ?: git.getLog()

    fun getLatestVersion(release: Boolean = false): String? =
        getLatest(release)?.toString()

    private fun getLatest(release: Boolean) = if (release) git.getLatestRelease() else git.getLatestVersion()
    fun getChangeLogMarkdown(release: Boolean = false, sections: Sections = Sections.default(), level: Int = 2) =
        sequence {
            val hl = "#".repeat(level)

            val commits = getChanges(release).mapNotNull { it.conventionalCommit }

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
