package de.fruiture.cor.ccs

import VERSION
import com.github.ajalt.clikt.testing.test
import de.fruiture.cor.ccs.cc.Type
import de.fruiture.cor.ccs.semver.AlphaNumericIdentifier.Companion.alphanumeric
import de.fruiture.cor.ccs.semver.ChangeType
import de.fruiture.cor.ccs.semver.PreRelease
import de.fruiture.cor.ccs.semver.PreReleaseIndicator.Strategy.Companion.counter
import de.fruiture.cor.ccs.semver.PreReleaseIndicator.Strategy.Companion.plus
import de.fruiture.cor.ccs.semver.PreReleaseIndicator.Strategy.Companion.static
import de.fruiture.cor.ccs.semver.Release
import de.fruiture.cor.ccs.semver.Version.Companion.version
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test

class CLITest {

    private val app = mockk<CCSApplication>(relaxed = true)
    private val cli = CLI(app)

    init {
        every { app.getNextRelease() } returns version("1.2.3") as Release
        every { app.getNextPreRelease(counter()) } returns version("1.2.3-RC.5") as PreRelease
    }

    @Test
    fun `next release`() {
        cli.test("next release").output shouldBe "1.2.3"
        verify { app.getNextRelease() }
    }

    @Test
    fun `next pre-release`() {
        cli.test("next pre-release").output shouldBe "1.2.3-RC.5"
        verify { app.getNextPreRelease(counter()) }
    }

    @Test
    fun `next pre-release custom`() {
        every { app.getNextPreRelease(counter("alpha".alphanumeric)) } returns version("1.2.3-alpha.5") as PreRelease
        cli.test("next pre-release -f alpha.1").output shouldBe "1.2.3-alpha.5"
    }

    @Test
    fun `combined counter and static strategy`() {
        every {
            app.getNextPreRelease(
                counter("alpha".alphanumeric) + static("snap".alphanumeric)
            )
        } returns version("1.2.3-alpha.1.snap") as PreRelease

        cli.test("next pre-release -f alpha.1.snap").output shouldBe "1.2.3-alpha.1.snap"
    }

    @Test
    fun `show help`() {
        cli.test("next --help").output shouldStartWith """
            Usage: git-ccs next [<options>] <command> [<args>]...

              compute the next semantic version based on changes since the last version tag
        """.trimIndent()
        verify { app wasNot called }
    }

    @Test
    fun `show help when nothing`() {
        cli.test("").output shouldStartWith "Usage: git-ccs"
    }

    @Test
    fun `illegal command`() {
        cli.test("nope").stderr shouldBe """
            Usage: git-ccs [<options>] <command> [<args>]...

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
        } returns version("1.1.1") as Release

        cli.test("next release -n default").output shouldBe "1.1.1"
    }

    @Test
    fun `get latest release tag`() {
        every {
            app.getLatestVersion(true)
        } returns "0.2.3"
        cli.test("latest -r").output shouldBe "0.2.3"
    }

    @Test
    fun `get latest release fails if no release yet`() {
        every {
            app.getLatestVersion(true)
        } returns null
        val result = cli.test("latest -r")
        result.statusCode shouldBe 1
        result.stderr shouldBe "no version found\n"
    }

    @Test
    fun `get latest version tag`() {
        every { app.getLatestVersion() } returns "0.2.3-SNAP"
        cli.test("latest").output shouldBe "0.2.3-SNAP"
    }

    @Test
    fun `get latest -t`() {
        every { app.getLatestVersion(before = version("1.0.0")) } returns "1.0.0-RC.5"
        cli.test("latest -t 1.0.0").output shouldBe "1.0.0-RC.5"
    }

    @Test
    fun `get log since release`() {
        every { app.getChangeLogJson(true) } returns "[{json}]"
        cli.test("log --release").output shouldBe "[{json}]"
    }

    @Test
    fun `log -t`() {
        every { app.getChangeLogJson(true, before = version("1.0.0")) } returns "[{json}]"
        cli.test("log --release --target 1.0.0").output shouldBe "[{json}]"
    }

    @Test
    fun `get markdown`() {
        every { app.getChangeLogMarkdown(false) } returns "*markdown*"
        cli.test("changes").output shouldBe "*markdown*"
    }

    @Test
    fun `changes -t`() {
        every { app.getChangeLogMarkdown(true, target = version("1.0.0")) } returns
                "*markdown of changes leading to 1.0.0"

        cli.test("changes -rt 1.0.0").output shouldBe "*markdown of changes leading to 1.0.0"
    }

    @Test
    fun `markdown with custom headings`() {
        every {
            app.getChangeLogMarkdown(
                release = false,
                sections = Sections(mapOf("Fun" to setOf(Type("feat")))),
                level = 1
            )
        } returns "# Fun"
        cli.test("changes -s 'Fun=feat' -l 1").output shouldBe "# Fun"
    }

    @Test
    fun `show version`() {
        cli.test("--version").output shouldBe "git-ccs version $VERSION\n"
    }


}