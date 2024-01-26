package de.fruiture.cor.ccs

import de.fruiture.cor.ccs.cc.Type
import de.fruiture.cor.ccs.git.GitCommit
import de.fruiture.cor.ccs.git.NON_CONVENTIONAL_COMMIT_TYPE
import de.fruiture.cor.ccs.semver.ChangeType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class ChangeMappingTest {
    private val mapping = ChangeMapping()

    @Test
    fun `default mapping`() {
        mapping.of(Type("feat")) shouldBe ChangeType.MINOR
        mapping.of(NON_CONVENTIONAL_COMMIT_TYPE) shouldBe ChangeType.PATCH
        mapping.of(DEFAULT_COMMIT_TYPE) shouldBe ChangeType.PATCH
    }

    @Test
    fun `commit mapping`() {
        mapping.of(
            GitCommit("cafe", ZonedDateTime.now(), "perf!: break API for speed")
        ) shouldBe ChangeType.MAJOR

        mapping.of(
            GitCommit("cafe", ZonedDateTime.now(), "perf: break API for speed")
        ) shouldBe ChangeType.PATCH
    }

    @Test
    fun `adjust mapping to none-bump`() {
        val adjusted = mapping + (DEFAULT_COMMIT_TYPE to ChangeType.NONE)

        adjusted.of(Type("doc")) shouldBe ChangeType.NONE
    }
}