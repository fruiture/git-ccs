package de.fruiture.cor.ccs

import com.github.ajalt.clikt.testing.test
import de.fruiture.cor.ccs.semver.AlphaNumericIdentifier.Companion.alphanumeric
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
        ccs.test("next").output shouldBe "1.2.3"
        verify { app.getNextRelease() }
    }

    @Test
    fun `next pre-release`() {
        ccs.test("next -p").output shouldBe "1.2.3-SNAPSHOT.5"
        verify { app.getNextPreRelease(counter(DEFAULT_PRERELEASE)) }
    }

    @Test
    fun `next pre-release RC`() {
        ccs.test("next -pi RC").output shouldBe "1.2.3-RC.1"
        verify { app.getNextPreRelease(counter("RC".alphanumeric)) }
    }

    @Test
    fun `show help`() {
        ccs.test("next --help").output shouldStartWith  """
            Usage: ccs next [<options>]

              compute the next version based on changes since the last tagged version
            
            Options:
              -p, --pre-release        create a pre-release version
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
}