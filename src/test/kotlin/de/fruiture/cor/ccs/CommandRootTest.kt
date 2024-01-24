package de.fruiture.cor.ccs

import de.fruiture.cor.ccs.cmd.IO
import de.fruiture.cor.ccs.cmd.Output
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class TestIO : IO {
    val captureOut = StringBuilder()
    val captureErr = StringBuilder()

    val stdout get() = captureOut.toString()
    val stderr get() = captureErr.toString()

    override val out = object : Output {
        override fun print(text: String) {
            captureOut.append(text)
        }

        override fun println(text: String) {
            captureOut.appendLine(text)
        }

    }
    override val err = object : Output {
        override fun print(text: String) {
            captureErr.append(text)
        }

        override fun println(text: String) {
            captureErr.appendLine(text)
        }
    }
}

class CommandRootTest {

    private val app = mockk<App>(relaxed = true)
    private val io = TestIO()

    init {
        every { app.getNextRelease() } returns "1.2.3"
        every { app.getNextPreRelease() } returns "1.2.3-SNAPSHOT.5"
        every { app.getNextPreRelease("RC") } returns "1.2.3-RC.1"
    }

    @Test
    fun `explicit next release`() {
        Root.getCommand(listOf("next", "release")).execute(app, io)
        verify { app.getNextRelease() }
        io.stdout shouldBe "1.2.3"
    }

    @Test
    fun `implicit next`() {
        Root.getCommand(listOf("next")).execute(app, io)
        verify { app.getNextRelease() }
        io.stdout shouldBe "1.2.3"
    }

    @Test
    fun `default command`() {
        Root.getCommand(emptyList()).execute(app, io)
        verify { app.getNextRelease() }
        io.stdout shouldBe "1.2.3"
    }

    @Test
    fun `next pre-release`() {
        Root.getCommand(listOf("next", "pre-release")).execute(app, io)
        verify { app.getNextPreRelease() }
        io.stdout shouldBe "1.2.3-SNAPSHOT.5"
    }

    @Test
    fun `next pre-release RC`() {
        Root.getCommand(listOf("next", "pre-release", "RC")).execute(app, io)
        verify { app.getNextPreRelease("RC") }
        io.stdout shouldBe "1.2.3-RC.1"
    }

    @Test
    fun `show help`() {
        Root.getCommand(listOf("next", "--help")).execute(app, io)
        verify { app wasNot called }
        io.stdout shouldBe """
            compute next version
            • release — compute next release version (no pre-release) -> 1.2.3
            · pre-release — compute next pre-release version
              • <pre-release identifier> – alphanumeric string (default: 'SNAPSHOT') -> '1.2.3-SNAPSHOT.4')
            use --help at any point to get help
            
        """.trimIndent()
    }

    @Test
    fun `dynamic help`() {
        Root.getCommand(listOf("next","pre-release","beta","--help")).execute(app,io)
        verify { app wasNot called }
        io.stdout shouldBe """
            compute pre-release with identifier 'beta' -> '1.2.3-beta.4'
            use --help at any point to get help
            
        """.trimIndent()
    }

    @Test
    fun `illegal command`() {
        Root.getCommand(listOf("NOPE")).execute(app,io)
        verify { app wasNot called }
        io.stderr shouldStartWith """
            unexpected 'NOPE'
            Conventional Commits & Semantic Versioning
            • next — compute next version
        """.trimIndent()
    }
}