package de.fruiture.cor.ccs.git

import de.fruiture.cor.ccs.cc.ConventionalCommitMessage
import de.fruiture.cor.ccs.cc.ConventionalCommitMessage.Companion.message
import de.fruiture.cor.ccs.cc.Description
import de.fruiture.cor.ccs.cc.Type
import io.kotest.matchers.shouldBe
import java.time.ZonedDateTime
import kotlin.test.Test

class GitCommitTest {

    @Test
    fun `parse conventional commit`() {
        val commit = GitCommit(
            hash = "cafe",
            date = ZonedDateTime.now(),
            message = "feat: a feature"
        )
        commit.conventional shouldBe message("feat: a feature")
        commit.type shouldBe Type("feat")
        commit.hasBreakingChange shouldBe false
    }

    @Test
    fun `tolerate invalid commit message`() {
        val commit = GitCommit(
            hash = "cafe",
            date = ZonedDateTime.now(),
            message = "non-conventional commit"
        )
        commit.conventional shouldBe ConventionalCommitMessage(
            type = NON_CONVENTIONAL_COMMIT_TYPE,
            description = Description("non-conventional commit")
        )
        commit.type shouldBe Type("none")
        commit.hasBreakingChange shouldBe false
    }
}