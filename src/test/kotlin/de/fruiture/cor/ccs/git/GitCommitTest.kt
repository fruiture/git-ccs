package de.fruiture.cor.ccs.git

import de.fruiture.cor.ccs.cc.ConventionalCommitMessage.Companion.message
import de.fruiture.cor.ccs.cc.Type
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class GitCommitTest {

    @Test
    fun `parse conventional commit`() {
        val commit = GitCommit(
            hash = "cafe",
            date = ZonedDateTime.now(),
            message = "feat: a feature"
        )
        commit.conventionalCommit shouldBe message("feat: a feature")
        commit.type shouldBe Type("feat")
        commit.hasBreakingChange shouldBe false
    }

    @Test
    fun `ignore invalid commit message`() {
        val commit = GitCommit(
            hash = "cafe",
            date = ZonedDateTime.now(),
            message = "non-conventional commit"
        )
        commit.conventionalCommit shouldBe null
        commit.type shouldBe Type("none")
        commit.hasBreakingChange shouldBe false
    }
}