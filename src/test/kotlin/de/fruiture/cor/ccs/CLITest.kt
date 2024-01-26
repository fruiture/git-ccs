package de.fruiture.cor.ccs

import com.github.ajalt.clikt.testing.test
import de.fruiture.cor.ccs.cc.Type
import de.fruiture.cor.ccs.semver.AlphaNumericIdentifier.Companion.alphanumeric
import de.fruiture.cor.ccs.semver.ChangeType
import de.fruiture.cor.ccs.semver.PreReleaseIndicator.Strategy.Companion.DEFAULT_PRERELEASE
import de.fruiture.cor.ccs.semver.PreReleaseIndicator.Strategy.Companion.counter
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class CLITest {

    private val app = mockk<App>(relaxed = true)
    private val ccs = CCS(app)

    init {
        every { app.getNextRelease() } returns "1.2.3"
        every { app.getNextPreRelease(counter(DEFAULT_PRERELEASE)) } returns "1.2.3-SNAPSHOT.5"
        every { app.getNextPreRelease(counter("RC".alphanumeric)) } returns "1.2.3-RC.1"
    }

    @Test
    fun `next release`() {
        ccs.test("next release").output shouldBe "1.2.3"
        verify { app.getNextRelease() }
    }

    @Test
    fun `next pre-release`() {
        ccs.test("next pre-release").output shouldBe "1.2.3-SNAPSHOT.5"
        verify { app.getNextPreRelease(counter(DEFAULT_PRERELEASE)) }
    }

    @Test
    fun `next pre-release RC`() {
        ccs.test("next pre-release -i RC").output shouldBe "1.2.3-RC.1"
        verify { app.getNextPreRelease(counter("RC".alphanumeric)) }
    }

    @Test
    fun `show help`() {
        ccs.test("next --help").output shouldStartWith """
            Usage: ccs next [<options>] <command> [<args>]...

              compute the next semantic version based on changes since the last version tag
        """.trimIndent()
        verify { app wasNot called }
    }

    @Test
    fun `show help when nothing`() {
        ccs.test("").output shouldStartWith "Usage: ccs"
    }

    @Test
    fun `illegal command`() {
        ccs.test("nope").stderr shouldBe """
            Usage: ccs [<options>] <command> [<args>]...

            Error: no such subcommand nope
            
        """.trimIndent()
        verify { app wasNot called }
    }

    @Test
    fun `allow non-bumps`() {
        every {
            app.getNextRelease(
                ChangeMapping() + (Type("default") to ChangeType.NONE)
            )
        } returns "1.1.1"

        ccs.test("next release -n default").output shouldBe "1.1.1"
    }

    @Test
    fun `get latest release tag`() {
        every {
            app.getLatestVersion(true)
        } returns "0.2.3"
        ccs.test("latest -r").output shouldBe "0.2.3"
    }

    @Test
    fun `get latest release fails if no release yet`() {
        every {
            app.getLatestVersion(true)
        } returns null
        val result = ccs.test("latest -r")
        result.statusCode shouldBe 1
        result.stderr shouldBe "no release found\n"
    }

    @Test
    fun `get latest version tag`() {
        every { app.getLatestVersion() } returns "0.2.3-SNAP"
        ccs.test("latest").output shouldBe "0.2.3-SNAP"
    }

    @Test
    fun `get log since release`() {
        every { app.getChangeLogJson(true) } returns "[{foo}]"
        ccs.test("log --release").output shouldBe "[{foo}]"
    }
}