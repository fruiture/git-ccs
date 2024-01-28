package de.fruiture.cor.ccs.git

import de.fruiture.cor.ccs.cc.Body
import de.fruiture.cor.ccs.cc.ConventionalCommitMessage
import de.fruiture.cor.ccs.cc.Description
import de.fruiture.cor.ccs.cc.Type
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.ZonedDateTime

private object ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("zoned-date-time", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ZonedDateTime {
        return ZonedDateTime.parse(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: ZonedDateTime) {
        encoder.encodeString(value.toString())
    }
}

val NON_CONVENTIONAL_COMMIT_TYPE = Type("none")

@Serializable
data class GitCommit(
    val hash: String,
    @Serializable(ZonedDateTimeSerializer::class)
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
                body = if(bodyText.isNotBlank()) Body(bodyText) else null
            )
        }

    val type = conventional.type
    val hasBreakingChange = conventional.hasBreakingChange
}
