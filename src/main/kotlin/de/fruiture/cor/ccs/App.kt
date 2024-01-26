package de.fruiture.cor.ccs

import de.fruiture.cor.ccs.cc.Type
import de.fruiture.cor.ccs.git.Git
import de.fruiture.cor.ccs.git.GitCommit
import de.fruiture.cor.ccs.semver.AlphaNumericIdentifier.Companion.alphanumeric
import de.fruiture.cor.ccs.semver.ChangeType
import de.fruiture.cor.ccs.semver.PreReleaseIndicator.Strategy
import de.fruiture.cor.ccs.semver.PreReleaseIndicator.Strategy.Companion.counter
import de.fruiture.cor.ccs.semver.Version
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val ANY_COMMIT_TYPE = Type("any")

class App(
    private val git: Git,
    private val types: Map<Type, ChangeType> = mapOf(
        Type("feat") to ChangeType.MINOR,
        Type("fix") to ChangeType.PATCH,
        ANY_COMMIT_TYPE to ChangeType.PATCH
    )
) {
    private fun getChangeType(latestVersion: Version) = git.getLog(latestVersion).change()

    private val defaultChange = types[ANY_COMMIT_TYPE]!!

    private fun List<GitCommit>.change() = maxOfOrNull(::changeType) ?: defaultChange

    private fun changeType(commit: GitCommit) =
        if (commit.hasBreakingChange) ChangeType.MAJOR else {
            types.getOrDefault(commit.type, defaultChange)
        }

    fun getNextRelease(): String {
        val latestVersion = git.getLatestVersion() ?: return Version.initial.toString()

        val changeType = getChangeType(latestVersion)
        return latestVersion.next(changeType).toString()
    }

    fun getNextPreRelease(label: String? = null): String {
        val strategy = label?.let { counter(it.alphanumeric) } ?: counter()
        return getNextPreRelease(strategy)
    }

    fun getNextPreRelease(strategy: Strategy): String {
        val latestVersion = git.getLatestVersion() ?: return initialPreRelease(strategy).toString()
        val changeType = getChangeType(latestVersion)

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
