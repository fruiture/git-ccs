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

    fun getChangeLog(): String {
        val commits = git.getLatestVersion()?.let { git.getLog(it) } ?: git.getLog()

        return json.encodeToString(commits)
    }
}
