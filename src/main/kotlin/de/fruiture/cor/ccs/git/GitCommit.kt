package de.fruiture.cor.ccs.git

import de.fruiture.cor.ccs.cc.ConventionalCommitMessage
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.ZonedDateTime

object ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("zoned-date-time", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ZonedDateTime {
        return ZonedDateTime.parse(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: ZonedDateTime) {
        encoder.encodeString(value.toString())
    }
}

@Serializable
data class GitCommit(
    val hash: String,
    @Serializable(ZonedDateTimeSerializer::class)
    val date: ZonedDateTime,
    val message: String
) {
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault
    val conventionalCommit =
        runCatching { ConventionalCommitMessage.message(message) }.getOrNull()
}
