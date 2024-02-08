package de.fruiture.cor.ccs.git

import de.fruiture.cor.ccs.cc.Body
import de.fruiture.cor.ccs.cc.ConventionalCommitMessage
import de.fruiture.cor.ccs.cc.Description
import de.fruiture.cor.ccs.cc.Type
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

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
