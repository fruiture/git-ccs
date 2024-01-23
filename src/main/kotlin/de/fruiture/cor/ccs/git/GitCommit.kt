package de.fruiture.cor.ccs.git

import de.fruiture.cor.ccs.cc.ConventionalCommitMessage
import java.time.ZonedDateTime

data class GitCommit(val hash: String, val date: ZonedDateTime, val message: String) {
    val conventionalCommit by lazy {
        runCatching {  ConventionalCommitMessage.message(message) }.getOrNull()
    }
}
