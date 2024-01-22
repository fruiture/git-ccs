package de.fruiture.cor.ccs.semver

import de.fruiture.cor.ccs.semver.AlphaNumericIdentifier.Companion.alphanumeric
import de.fruiture.cor.ccs.semver.Build.Companion.build
import de.fruiture.cor.ccs.semver.PreReleaseIdentifier.Companion.identifier
import de.fruiture.cor.ccs.semver.PreReleaseIndicator.Companion.preRelease
import de.fruiture.cor.ccs.semver.Version.Companion.version
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class VersionUsageTest {

    @Test
    fun `special releases`() {
        Version.initial shouldBe version("0.0.0")
        Version.stable shouldBe version("1.0.0")
    }

    @Test
    fun `bumping releases`() {
        version("1.0.0").bump(ChangeType.MINOR) shouldBe version("1.1.0")
        version("1.0.0").bump(ChangeType.PATCH) shouldBe version("1.0.1")
        version("1.0.0").bump(ChangeType.MAJOR) shouldBe version("2.0.0")

        version("2.3.25").bump(ChangeType.PATCH) shouldBe version("2.3.26")
        version("2.3.25").bump(ChangeType.MINOR) shouldBe version("2.4.0")
        version("2.3.25").bump(ChangeType.MAJOR) shouldBe version("3.0.0")
    }

    @Test
    fun `bumping releases drops build metadata and pre-releases`() {
        version("1.0.7-rc.1").bump(ChangeType.MAJOR) shouldBe version("2.0.0")
        version("1.0.7-rc.1").bump(ChangeType.MINOR) shouldBe version("1.1.0")
        version("1.0.7-rc.1").bump(ChangeType.PATCH) shouldBe version("1.0.8")

        version("1.0.7-rc.1+commit").bump(ChangeType.MAJOR) shouldBe version("2.0.0")
    }

    @Test
    fun `attach pre-release and build metadata`() {
        version("1.2.3-snapshot").release shouldBe version("1.2.3")
        version("3.2.0").release.preRelease("rc.1") shouldBe version("3.2.0-rc.1")

        version("3.2.0").build("job.267") shouldBe version("3.2.0+job.267")
        version("3.2.0+job.123").build("job.267") shouldBe version("3.2.0+job.267")
        version("4.0.99-RC.1").build("job.008") shouldBe version("4.0.99-RC.1+job.008")
    }

    @Test
    fun `more sugar - append arbitrary pre-release and build metadata`() {
        version("1.2.3") + preRelease("RC.5") shouldBe version("1.2.3-RC.5")
        version("1.2.3-RC.3") + preRelease("RC.5") shouldBe version("1.2.3-RC.3.RC.5")
        version("1.2.3+cafe") + preRelease("RC.5") shouldBe version("1.2.3-RC.5+cafe")
        version("1.2.3-alpha+cafe") + preRelease("RC.5") shouldBe version("1.2.3-alpha.RC.5+cafe")

        version("1.2.3") + build("c1f2e3") shouldBe version("1.2.3+c1f2e3")
        version("1.2.3-pre") + build("c1f2e3") shouldBe version("1.2.3-pre+c1f2e3")
        version("1.2.3-pre+cafe") + build("c1f2e3") shouldBe version("1.2.3-pre+cafe.c1f2e3")
    }

    @Test
    fun `bump pre-release`() {
        version("1.2.3-rc.1").bumpPreRelease() shouldBe version("1.2.3-rc.2")
        version("1.2.3-rc").bumpPreRelease() shouldBe version("1.2.3-rc.2")

        version("1.2.3").bumpPreRelease() shouldBe version("1.2.3-SNAPSHOT.1")
        version("1.2.3").bumpPreRelease(identifier("RC".alphanumeric)) shouldBe version("1.2.3-RC.1")

        val alpha = identifier("alpha")

        version("1.2.3-beta").bumpPreRelease(alpha) shouldBe version("1.2.3-beta.alpha.1")
        version("1.2.3-alpha").bumpPreRelease(alpha) shouldBe version("1.2.3-alpha.2")
        version("1.2.3-alpha.7").bumpPreRelease(alpha) shouldBe version("1.2.3-alpha.8")
        version("1.2.3").bumpPreRelease(alpha) shouldBe version("1.2.3-alpha.1")

        version("1.2.3-alpha.7.junk").bumpPreRelease(alpha) shouldBe version("1.2.3-alpha.8.junk")
    }

    @Test
    fun `detect release diff`() {
        (version("1.2.3") - version("1.2.2")) shouldBe ChangeType.PATCH
        (version("1.2.3") - version("1.1.7")) shouldBe ChangeType.MINOR
        (version("1.2.3") - version("0.0.3")) shouldBe ChangeType.MAJOR
        (version("1.2.3") - version("1.2.2")) shouldBe ChangeType.PATCH
        (version("1.0.0") - version("1.0.0")) shouldBe null
    }

    @Test
    fun `pre release strategy`() {
        val lastRelease = version("1.2.3") as Release
        val lastPreRelease = version("1.2.4-SNAPSHOT.1") as PreRelease

        lastRelease.bump(ChangeType.PATCH).bumpPreRelease() shouldBe lastPreRelease

        lastPreRelease.bumpPreRelease(lastRelease, ChangeType.PATCH) shouldBe version("1.2.4-SNAPSHOT.2")
        lastPreRelease.bumpPreRelease(lastRelease, ChangeType.MINOR) shouldBe version("1.3.0-SNAPSHOT.1")
        lastPreRelease.bumpPreRelease(lastRelease, ChangeType.MAJOR) shouldBe version("2.0.0-SNAPSHOT.1")

        (version("2.0.0-SNAPSHOT.1") as PreRelease).bumpPreRelease(
            lastRelease,
            ChangeType.PATCH
        ) shouldBe version("2.0.0-SNAPSHOT.2")
    }
}