package de.fruiture.cor.ccs.git

import de.fruiture.cor.ccs.cc.Body
import de.fruiture.cor.ccs.cc.ConventionalCommitMessage
import de.fruiture.cor.ccs.cc.Description
import de.fruiture.cor.ccs.cc.Type
import de.fruiture.cor.ccs.semver.Version
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class ZonedDateTime(private val iso8601: String) {
    override fun toString() = iso8601
}

val NON_CONVENTIONAL_COMMIT_TYPE = Type("none")

@Serializable
data class GitCommit(
    val hash: String,
    val date: ZonedDateTime,
    val message: String
) {
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault
    val conventional =
        runCatching { ConventionalCommitMessage.message(message) }.getOrElse {
            val lines = message.lines()
            val bodyText = lines.dropLast(1).joinToString("\n").trim()

            ConventionalCommitMessage(
                type = NON_CONVENTIONAL_COMMIT_TYPE,
                description = Description(lines.first()),
                body = if (bodyText.isNotBlank()) Body(bodyText) else null
            )
        }

    val type = conventional.type
    val hasBreakingChange = conventional.hasBreakingChange
}

@JvmInline
value class TagName(val value: String) {
    override fun toString() = value
}

data class VersionTag(val tag: TagName, val version: Version) : Comparable<VersionTag> {
    init {
        require(tag.value.contains(version.toString())) { "tag '$tag' does not contain version number '$version'" }
    }

    override fun compareTo(other: VersionTag) = version.compareTo(other.version)

    override fun toString() = tag.toString()

    companion object {
        fun versionTag(tagName: String) = Version.extractVersion(tagName)?.let { VersionTag(TagName(tagName), it) }
    }
}