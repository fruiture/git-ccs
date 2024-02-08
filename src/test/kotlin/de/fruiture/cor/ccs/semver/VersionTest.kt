package de.fruiture.cor.ccs.semver

import de.fruiture.cor.ccs.semver.AlphaNumericIdentifier.Companion.alphanumeric
import de.fruiture.cor.ccs.semver.DigitIdentifier.Companion.digits
import de.fruiture.cor.ccs.semver.NumericIdentifier.Companion.numeric
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class VersionTest {
    @Test
    fun `represent version cores`() {
        VersionCore.of(
            major = 1, minor = 0, patch = 0
        ).toString() shouldBe "1.0.0"
    }

    @Test
    fun `represent pre-release`() {
        PreReleaseIndicator(
            listOf(
                PreReleaseIdentifier.identifier("alpha".alphanumeric), PreReleaseIdentifier.identifier(26.numeric)
            )
        ).toString() shouldBe "alpha.26"
    }

    @Test
    fun `compare release versions`() {
        val v100 = Release(VersionCore.of(1, 0, 0))
        val v101 = Release(VersionCore.of(1, 0, 1))

        v100.compareTo(v101) shouldBe -1
        v101.compareTo(v100) shouldBe 1
    }

    @Test
    fun `compare pre-release versions with releases`() {
        val v100Alpha = PreRelease(
            VersionCore.of(1, 0, 0),
            PreReleaseIndicator.of(
                PreReleaseIdentifier.identifier("alpha".alphanumeric)
            )
        )
        val v100 = Release(VersionCore.of(1, 0, 0))
        val v001 = Release(VersionCore.of(0, 0, 1))

        v100Alpha.compareTo(v100) shouldBe -1
        v100.compareTo(v100Alpha) shouldBe 1

        v100Alpha.compareTo(v001) shouldBe 1
        v001.compareTo(v100Alpha) shouldBe -1
    }

    @Test
    fun `compare pre-releases`() {
        val alpha = PreRelease(
            VersionCore.of(1, 0, 0),
            PreReleaseIndicator.of(
                PreReleaseIdentifier.identifier("alpha".alphanumeric)
            )
        )
        val beta = PreRelease(
            VersionCore.of(1, 0, 0),
            PreReleaseIndicator.of(
                PreReleaseIdentifier.identifier("beta".alphanumeric)
            )
        )
        val betaPlus = PreRelease(
            VersionCore.of(1, 0, 0),
            PreReleaseIndicator.of(
                PreReleaseIdentifier.identifier("beta".alphanumeric),
                PreReleaseIdentifier.identifier("plus".alphanumeric)
            )
        )

        alpha.compareTo(beta) shouldBe -1
        beta.compareTo(alpha) shouldBe 1
        beta.compareTo(betaPlus) shouldBe -1
    }

    @Test
    fun `allow build identifiers`() {
        Release(
            VersionCore.of(2, 1, 3),
            Build(
                listOf(
                    BuildIdentifier.identifier("build".alphanumeric),
                    BuildIdentifier.identifier("000".digits)
                )
            )
        ).toString() shouldBe "2.1.3+build.000"

        PreRelease(
            VersionCore.of(2, 1, 3),
            PreReleaseIndicator.of(
                PreReleaseIdentifier.identifier("alpha".alphanumeric)
            ),
            Build(
                listOf(
                    BuildIdentifier.identifier("build".alphanumeric),
                    BuildIdentifier.identifier("000".digits)
                )
            )
        ).toString() shouldBe "2.1.3-alpha+build.000"
    }

    @Test
    fun `version strings`() {
        Release(VersionCore.of(1, 2, 3)).toString() shouldBe "1.2.3"
        PreRelease(
            VersionCore.of(1, 2, 3),
            PreReleaseIndicator.of(
                PreReleaseIdentifier.identifier("alpha".alphanumeric)
            )
        ).toString() shouldBe "1.2.3-alpha"
    }

    @Test
    fun `valid alphanumeric identifiers`() {
        AlphaNumericIdentifier("foo")
        AlphaNumericIdentifier("foo-bar")
        shouldThrow<IllegalArgumentException> {
            AlphaNumericIdentifier("0000")
        }
        AlphaNumericIdentifier("0000a")
    }

    @Test
    fun `parsing versions`() {
        Version.version("1.2.3") shouldBe Release(VersionCore.of(1, 2, 3))
        Version.version("1.2.3+x23.005") shouldBe Release(
            VersionCore.of(1, 2, 3),
            Build(listOf(BuildIdentifier.identifier("x23".alphanumeric), BuildIdentifier.identifier("005".digits)))
        )
        Version.version("1.2.3-alpha.7.go-go+x23.005") shouldBe PreRelease(
            VersionCore.of(1, 2, 3),
            PreReleaseIndicator(
                listOf(
                    PreReleaseIdentifier.identifier("alpha".alphanumeric),
                    PreReleaseIdentifier.identifier(7.numeric),
                    PreReleaseIdentifier.identifier("go-go".alphanumeric)
                )
            ),
            Build(listOf(BuildIdentifier.identifier("x23".alphanumeric), BuildIdentifier.identifier("005".digits)))
        )

        Version.version("1.2.3-alpha.7.go-go") shouldBe PreRelease(
            VersionCore.of(1, 2, 3),
            PreReleaseIndicator(
                listOf(
                    PreReleaseIdentifier.identifier("alpha".alphanumeric),
                    PreReleaseIdentifier.identifier(7.numeric),
                    PreReleaseIdentifier.identifier("go-go".alphanumeric)
                )
            )
        )
    }
}


