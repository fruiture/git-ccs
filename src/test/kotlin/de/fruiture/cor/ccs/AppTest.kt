package de.fruiture.cor.ccs

import de.fruiture.cor.ccs.cc.Type
import de.fruiture.cor.ccs.git.Git
import de.fruiture.cor.ccs.git.GitCommit
import de.fruiture.cor.ccs.semver.AlphaNumericIdentifier.Companion.alphanumeric
import de.fruiture.cor.ccs.semver.PreReleaseIndicator.Strategy.Companion.counter
import de.fruiture.cor.ccs.semver.Release
import de.fruiture.cor.ccs.semver.Version.Companion.version
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class AppTest {

    private val oneFeatureAfterMajorRelease = mockk<Git>().apply {
        every { getLatestVersion() } returns version("1.0.0")
        every { getLatestRelease() } returns version("1.0.0") as Release
        every { getLog(from = version("1.0.0")) } returns listOf(
            GitCommit("cafebabe", ZonedDateTime.parse("2001-01-01T13:00Z"), "feat: a feature is born")
        )
    }

    private val noReleaseYet = mockk<Git>().apply {
        every { getLatestVersion() } returns null
        every { getLatestRelease() } returns null
        every { getLog(from = null) } returns listOf(
            GitCommit("cafebabe", ZonedDateTime.parse("2001-01-01T13:00Z"), "feat: a feature is born")
        )
    }

    private val hadABreakingChangeAfterSnapshot = mockk<Git>().apply {
        every { getLatestVersion() } returns version("1.2.3-SNAPSHOT.5")
        every { getLog(from = version("1.2.3-SNAPSHOT.5")) } returns listOf(
            GitCommit("cafebabe", ZonedDateTime.parse("2001-01-01T13:00Z"), "feat!: a feature with a breaking change")
        )
    }

    private val afterMultipleReleases = mockk<Git>().apply {
        every { getLatestVersion(before = version("1.0.0")) } returns version("1.0.0-RC.3")
        every { getLatestRelease(before = version("1.0.0")) } returns version("0.3.7") as Release

        every { getLog(from = version("0.3.7"),  to = version("1.0.0")) } returns listOf(
            GitCommit("cafebabe", ZonedDateTime.parse("2001-01-01T13:00Z"), "feat: range change")
        )
    }

    private val mixedBagOfCommits = mockk<Git>().apply {
        every { getLatestVersion() } returns version("1.0.0")
        every { getLog(from = version("1.0.0")) } returns listOf(
            GitCommit("0001", ZonedDateTime.parse("2001-01-01T13:01Z"), "feat: feature1"),
            GitCommit("002", ZonedDateTime.parse("2001-01-01T13:02Z"), "fix: fix2"),
            GitCommit("003", ZonedDateTime.parse("2001-01-01T13:03Z"), "perf: perf3"),
            GitCommit("004", ZonedDateTime.parse("2001-01-01T13:04Z"), "none4")
        )
    }

    @Test
    fun `get next release version`() {
        App(oneFeatureAfterMajorRelease).getNextRelease() shouldBe version("1.1.0")
    }

    @Test
    fun `get next pre-release version`() {
        App(oneFeatureAfterMajorRelease).getNextPreRelease(counter()) shouldBe version("1.1.0-SNAPSHOT.1")
        App(oneFeatureAfterMajorRelease).getNextPreRelease(counter("alpha".alphanumeric)) shouldBe version("1.1.0-alpha.1")
    }


    @Test
    fun `get initial release or snapshot`() {
        App(noReleaseYet).getNextRelease() shouldBe version("0.0.1")
        App(noReleaseYet).getNextPreRelease(counter("RC".alphanumeric)) shouldBe version("0.0.1-RC.1")
        App(noReleaseYet).getNextPreRelease(counter()) shouldBe version("0.0.1-SNAPSHOT.1")
    }

    @Test
    fun `get change log`() {
        App(oneFeatureAfterMajorRelease).getChangeLogJson() shouldBe
                """[{"hash":"cafebabe","date":"2001-01-01T13:00Z","message":"feat: a feature is born",""" +
                """"conventional":{"type":"feat","description":"a feature is born"}}]"""

        App(noReleaseYet).getChangeLogJson() shouldBe
                """[{"hash":"cafebabe","date":"2001-01-01T13:00Z","message":"feat: a feature is born",""" +
                """"conventional":{"type":"feat","description":"a feature is born"}}]"""
    }

    @Test
    fun `get change log before a certain version`() {
        App(afterMultipleReleases).getChangeLogJson(release = true, before = version("1.0.0")) shouldBe
                """[{"hash":"cafebabe","date":"2001-01-01T13:00Z","message":"feat: range change",""" +
                """"conventional":{"type":"feat","description":"range change"}}]"""
    }

    @Test
    fun `breaking change is recognized`() {
        App(hadABreakingChangeAfterSnapshot).getNextRelease() shouldBe version("2.0.0")
        App(hadABreakingChangeAfterSnapshot).getNextPreRelease(counter()) shouldBe version("2.0.0-SNAPSHOT.1")
    }

    @Test
    fun `get latest release`() {
        App(oneFeatureAfterMajorRelease).getLatestVersion(true) shouldBe "1.0.0"
        App(noReleaseYet).getLatestVersion(true) shouldBe null
    }

    @Test
    fun `get latest version`() {
        App(hadABreakingChangeAfterSnapshot).getLatestVersion() shouldBe "1.2.3-SNAPSHOT.5"
    }

    @Test
    fun `get latest version before another version`() {
        App(afterMultipleReleases).getLatestVersion(before = version("1.0.0")) shouldBe "1.0.0-RC.3"
        App(afterMultipleReleases).getLatestVersion(release = true, before = version("1.0.0")) shouldBe "0.3.7"
    }

    @Test
    fun `get markdown`() {
        App(oneFeatureAfterMajorRelease).getChangeLogMarkdown(
            release = false,
            sections = Sections(mapOf("Neue Funktionen" to setOf(Type("feat"))))
        ) shouldBe """
            ## Neue Funktionen
            
            * a feature is born
            
        """.trimIndent()
    }

    @Test
    fun `get markdown with breaking changes`() {
        App(hadABreakingChangeAfterSnapshot).getChangeLogMarkdown(
            release = false,
            sections = Sections().setBreakingChanges("API broken")
        ) shouldBe """
            ## API broken
            
            * a feature with a breaking change
            
        """.trimIndent()
    }

    @Test
    fun `summarize various types`() {
        App(mixedBagOfCommits).getChangeLogMarkdown() shouldBe """
            ## Features
            
            * feature1
            
            ## Bugfixes
            
            * fix2
            
            ## Other
            
            * perf3
            * none4
            
        """.trimIndent()
    }
}