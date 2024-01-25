package de.fruiture.cor.ccs

import com.github.ajalt.clikt.testing.test
import io.kotest.matchers.shouldBe
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
        every { app.getNextPreRelease("SNAPSHOT") } returns "1.2.3-SNAPSHOT.5"
        every { app.getNextPreRelease("RC") } returns "1.2.3-RC.1"
    }

    @Test
    fun `next release`() {
        ccs.test("next").output shouldBe "1.2.3"
        verify { app.getNextRelease() }
    }

    @Test
    fun `next pre-release`() {
        ccs.test("next -p").output shouldBe "1.2.3-SNAPSHOT.5"
        verify { app.getNextPreRelease("SNAPSHOT") }
    }

    @Test
    fun `next pre-release RC`() {
        ccs.test("next -pi RC").output shouldBe "1.2.3-RC.1"
        verify { app.getNextPreRelease("RC") }
    }

    @Test
    fun `show help`() {
        ccs.test("next --help").output shouldBe """
            Usage: ccs next [<options>]

            Options:
              -p, --pre-release        create a pre-release version instead of a full
                                       release
              -i, --identifier=<text>  set the pre-release identifier (default: 'SNAPSHOT')
                                       -> '1.2.3-SNAPSHOT.4'
              -h, --help               Show this message and exit
            
        """.trimIndent()
        verify { app wasNot called }
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