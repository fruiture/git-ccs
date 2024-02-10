package de.fruiture.cor.ccs

import de.fruiture.cor.ccs.cc.Type
import de.fruiture.cor.ccs.git.*
import de.fruiture.cor.ccs.git.VersionTag.Companion.versionTag
import de.fruiture.cor.ccs.semver.AlphaNumericIdentifier.Companion.alphanumeric
import de.fruiture.cor.ccs.semver.PreReleaseIndicator.Strategy.Companion.counter
import de.fruiture.cor.ccs.semver.Version.Companion.version
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test

class CCSApplicationTest {

    private val oneFeatureAfterMajorRelease = mockk<Git>().apply {
        every { getLatestVersionTag() } returns versionTag("1.0.0")
        every { getLatestReleaseTag() } returns versionTag("1.0.0")
        every { getLogX(from = TagName("1.0.0")) } returns listOf(
            GitCommit("cafebabe", ZonedDateTime("2001-01-01T13:00Z"), "feat: a feature is born")
        )
    }

    private val noReleaseYet = mockk<Git>().apply {
        every { getLatestVersionTag() } returns null
        every { getLatestReleaseTag() } returns null
        every { getLogX(from = null) } returns listOf(
            GitCommit("cafebabe", ZonedDateTime("2001-01-01T13:00Z"), "feat: a feature is born")
        )
    }

    private val hadABreakingChangeAfterSnapshot = mockk<Git>().apply {
        every { getLatestVersionTag() } returns versionTag("1.2.3-SNAPSHOT.5")
        every { getLogX(from = TagName("1.2.3-SNAPSHOT.5")) } returns listOf(
            GitCommit("cafebabe", ZonedDateTime("2001-01-01T13:00Z"), "feat!: a feature with a breaking change")
        )
    }

    private val afterMultipleReleases = mockk<Git>().apply {
        every { getLatestVersionTag(before(version("1.0.0"))) } returns versionTag("1.0.0-RC.3")
        every { getLatestReleaseTag(before(version("1.0.0"))) } returns versionTag("vers0.3.7")

        every { getLatestVersionTag(until(version("1.0.0"))) } returns versionTag("v1.0.0")

        every { getLogX(from = TagName("vers0.3.7"), to = TagName("v1.0.0")) } returns listOf(
            GitCommit("cafebabe", ZonedDateTime("2001-01-01T13:00Z"), "feat: range change")
        )
    }

    private val mixedBagOfCommits = mockk<Git>().apply {
        every { getLatestVersionTag() } returns versionTag("1.0.0")
        every { getLogX(from = TagName("1.0.0")) } returns listOf(
            GitCommit("0001", ZonedDateTime("2001-01-01T13:01Z"), "feat: feature1"),
            GitCommit("002", ZonedDateTime("2001-01-01T13:02Z"), "fix: fix2"),
            GitCommit("003", ZonedDateTime("2001-01-01T13:03Z"), "perf: perf3"),
            GitCommit("004", ZonedDateTime("2001-01-01T13:04Z"), "none4")
        )
    }

    @Test
    fun `get next release version`() {
        CCSApplication(oneFeatureAfterMajorRelease).getNextRelease() shouldBe version("1.1.0")
    }

    @Test
    fun `get next pre-release version`() {
        CCSApplication(oneFeatureAfterMajorRelease).getNextPreRelease(counter()) shouldBe version("1.1.0-SNAPSHOT.1")
        CCSApplication(oneFeatureAfterMajorRelease).getNextPreRelease(counter("alpha".alphanumeric)) shouldBe version("1.1.0-alpha.1")
    }


    @Test
    fun `get initial release or snapshot`() {
        CCSApplication(noReleaseYet).getNextRelease() shouldBe version("0.0.1")
        CCSApplication(noReleaseYet).getNextPreRelease(counter("RC".alphanumeric)) shouldBe version("0.0.1-RC.1")
        CCSApplication(noReleaseYet).getNextPreRelease(counter()) shouldBe version("0.0.1-SNAPSHOT.1")
    }

    @Test
    fun `get change log`() {
        CCSApplication(oneFeatureAfterMajorRelease).getChangeLogJson() shouldBe
                """[{"hash":"cafebabe","date":"2001-01-01T13:00Z","message":"feat: a feature is born",""" +
                """"conventional":{"type":"feat","description":"a feature is born"}}]"""

        CCSApplication(noReleaseYet).getChangeLogJson() shouldBe
                """[{"hash":"cafebabe","date":"2001-01-01T13:00Z","message":"feat: a feature is born",""" +
                """"conventional":{"type":"feat","description":"a feature is born"}}]"""
    }

    @Test
    fun `get change log before a certain version`() {
        CCSApplication(afterMultipleReleases).getChangeLogJson(release = true, before = version("1.0.0")) shouldBe
                """[{"hash":"cafebabe","date":"2001-01-01T13:00Z","message":"feat: range change",""" +
                """"conventional":{"type":"feat","description":"range change"}}]"""
    }

    @Test
    fun `breaking change is recognized`() {
        CCSApplication(hadABreakingChangeAfterSnapshot).getNextRelease() shouldBe version("2.0.0")
        CCSApplication(hadABreakingChangeAfterSnapshot).getNextPreRelease(counter()) shouldBe version("2.0.0-SNAPSHOT.1")
    }

    @Test
    fun `get latest release`() {
        CCSApplication(oneFeatureAfterMajorRelease).getLatestVersion(true) shouldBe "1.0.0"
        CCSApplication(noReleaseYet).getLatestVersion(true) shouldBe null
    }

    @Test
    fun `get latest version`() {
        CCSApplication(hadABreakingChangeAfterSnapshot).getLatestVersion() shouldBe "1.2.3-SNAPSHOT.5"
    }

    @Test
    fun `get latest version before another version`() {
        CCSApplication(afterMultipleReleases).getLatestVersion(before = version("1.0.0")) shouldBe "1.0.0-RC.3"
        CCSApplication(afterMultipleReleases).getLatestVersion(
            release = true,
            before = version("1.0.0")
        ) shouldBe "0.3.7"
    }

    @Test
    fun `get markdown`() {
        CCSApplication(oneFeatureAfterMajorRelease).getChangeLogMarkdown(
            release = false,
            sections = Sections(mapOf("Neue Funktionen" to setOf(Type("feat"))))
        ) shouldBe """
            ## Neue Funktionen
            
            * a feature is born
            
        """.trimIndent()
    }

    @Test
    fun `get markdown with breaking changes`() {
        CCSApplication(hadABreakingChangeAfterSnapshot).getChangeLogMarkdown(
            release = false,
            sections = Sections().setBreakingChanges("API broken")
        ) shouldBe """
            ## API broken
            
            * a feature with a breaking change
            
        """.trimIndent()
    }

    @Test
    fun `summarize various types`() {
        CCSApplication(mixedBagOfCommits).getChangeLogMarkdown() shouldBe """
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