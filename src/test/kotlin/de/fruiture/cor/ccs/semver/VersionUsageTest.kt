package de.fruiture.cor.ccs.semver

import de.fruiture.cor.ccs.semver.AlphaNumericIdentifier.Companion.alphanumeric
import de.fruiture.cor.ccs.semver.Build.Companion.build
import de.fruiture.cor.ccs.semver.PreReleaseIndicator.Companion.preRelease
import de.fruiture.cor.ccs.semver.PreReleaseIndicator.Strategy.Companion.counter
import de.fruiture.cor.ccs.semver.PreReleaseIndicator.Strategy.Companion.static
import de.fruiture.cor.ccs.semver.Version.Companion.version
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class VersionUsageTest {

    @Test
    fun `special releases`() {
        Version.initial shouldBe version("0.0.1")
        Version.stable shouldBe version("1.0.0")
    }

    @Test
    fun `bumping releases`() {
        version("1.0.0").next(ChangeType.MINOR) shouldBe version("1.1.0")
        version("1.0.0").next(ChangeType.PATCH) shouldBe version("1.0.1")
        version("1.0.0").next(ChangeType.MAJOR) shouldBe version("2.0.0")

        version("2.3.25").next(ChangeType.PATCH) shouldBe version("2.3.26")
        version("2.3.25").next(ChangeType.MINOR) shouldBe version("2.4.0")
        version("2.3.25").next(ChangeType.MAJOR) shouldBe version("3.0.0")

        version("2.3.25").next(ChangeType.NONE) shouldBe version("2.3.25")
    }

    @Test
    fun `bumping pre-releases drops pre-release but may not bump release`() {
        version("1.0.7-rc.1").next(ChangeType.MAJOR) shouldBe version("2.0.0")
        version("1.0.7-rc.1").next(ChangeType.MINOR) shouldBe version("1.1.0")
        version("1.0.7-rc.1").next(ChangeType.PATCH) shouldBe version("1.0.7")

        version("2.3.0-rc.1").next(ChangeType.MAJOR) shouldBe version("3.0.0")
        version("2.3.0-rc.1").next(ChangeType.MINOR) shouldBe version("2.3.0")
        version("2.3.0-rc.1").next(ChangeType.PATCH) shouldBe version("2.3.0")

        version("3.0.0-rc.1").next(ChangeType.MAJOR) shouldBe version("3.0.0")
        version("3.0.0-rc.1").next(ChangeType.MINOR) shouldBe version("3.0.0")
        version("3.0.0-rc.1").next(ChangeType.PATCH) shouldBe version("3.0.0")

        version("1.0.7-rc.1").next(ChangeType.NONE) shouldBe version("1.0.7")
    }

    @Test
    fun `bumping any releases drops build metadata`() {
        version("1.0.7-rc.1+commit").next(ChangeType.PATCH) shouldBe version("1.0.7")
        version("1.0.7-rc.1+commit").next(ChangeType.MAJOR) shouldBe version("2.0.0")
        version("1.0.7+commit").next(ChangeType.MAJOR) shouldBe version("2.0.0")
        version("1.0.7+commit").next(ChangeType.PATCH) shouldBe version("1.0.8")
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
    fun `bump pre-release only`() {
        version("1.2.3-rc.1").nextPreRelease(counter("rc".alphanumeric)) shouldBe version("1.2.3-rc.2")
        version("1.2.3-rc").nextPreRelease(counter("rc".alphanumeric)) shouldBe version("1.2.3-rc.2")

        version("1.2.3").nextPreRelease() shouldBe version("1.2.3-SNAPSHOT.1")
        version("1.2.3").nextPreRelease(counter("RC".alphanumeric)) shouldBe version("1.2.3-RC.1")

        val alpha = counter("alpha".alphanumeric)

        version("1.2.3-beta").nextPreRelease(alpha) shouldBe version("1.2.3-beta.alpha.1")
        version("1.2.3-alpha").nextPreRelease(alpha) shouldBe version("1.2.3-alpha.2")
        version("1.2.3-alpha.7").nextPreRelease(alpha) shouldBe version("1.2.3-alpha.8")
        version("1.2.3").nextPreRelease(alpha) shouldBe version("1.2.3-alpha.1")

        version("1.2.3-alpha.7.junk").nextPreRelease(alpha) shouldBe version("1.2.3-alpha.8.junk")
    }

    @Test
    fun `static pre-releases`() {
        version("1.2.3").nextPreRelease(static()) shouldBe version("1.2.3-SNAPSHOT")
        version("1.2.3").nextPreRelease(ChangeType.MINOR, static()) shouldBe version("1.3.0-SNAPSHOT")

        version("1.2.3-SNAPSHOT").nextPreRelease(ChangeType.PATCH, static()) shouldBe version("1.2.3-SNAPSHOT")

        version("1.2.3").nextPreRelease(ChangeType.PATCH, static("foo".alphanumeric)) shouldBe version("1.2.4-foo")
    }

    @Test
    fun `pre release strategy`() {
        val lastRelease = version("1.2.3") as Release
        val lastPreRelease = version("1.2.4-SNAPSHOT.1") as PreRelease

        lastRelease.next(ChangeType.PATCH).nextPreRelease() shouldBe lastPreRelease
        lastRelease.nextPreRelease(ChangeType.PATCH) shouldBe lastPreRelease

        lastPreRelease.nextPreRelease(ChangeType.NONE) shouldBe version("1.2.4-SNAPSHOT.2")
        lastPreRelease.nextPreRelease(ChangeType.PATCH) shouldBe version("1.2.4-SNAPSHOT.2")
        lastPreRelease.nextPreRelease(ChangeType.MINOR) shouldBe version("1.3.0-SNAPSHOT.1")
        lastPreRelease.nextPreRelease(ChangeType.MAJOR) shouldBe version("2.0.0-SNAPSHOT.1")

        (version("2.0.0-SNAPSHOT.1") as PreRelease).nextPreRelease(ChangeType.PATCH) shouldBe
                version("2.0.0-SNAPSHOT.2")
    }
}