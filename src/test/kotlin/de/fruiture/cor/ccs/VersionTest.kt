package de.fruiture.cor.ccs

import de.fruiture.cor.ccs.AlphaNumericIdentifier.Companion.alpha
import de.fruiture.cor.ccs.Build.Companion.suffix
import de.fruiture.cor.ccs.DigitIdentifier.Companion.digits
import de.fruiture.cor.ccs.NumericIdentifier.Companion.numeric
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
                PreReleaseIdentifier.of("alpha".alpha), PreReleaseIdentifier.of(26.numeric)
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
                PreReleaseIdentifier.of("alpha".alpha)
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
                PreReleaseIdentifier.of("alpha".alpha)
            )
        )
        val beta = PreRelease(
            VersionCore.of(1, 0, 0),
            PreReleaseIndicator.of(
                PreReleaseIdentifier.of("beta".alpha)
            )
        )
        val betaPlus = PreRelease(
            VersionCore.of(1, 0, 0),
            PreReleaseIndicator.of(
                PreReleaseIdentifier.of("beta".alpha),
                PreReleaseIdentifier.of("plus".alpha)
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
                    BuildIdentifier.of("build".alpha),
                    BuildIdentifier.of("000".digits)
                )
            )
        ).toString() shouldBe "2.1.3+build.000"

        PreRelease(
            VersionCore.of(2, 1, 3),
            PreReleaseIndicator.of(
                PreReleaseIdentifier.of("alpha".alpha)
            ),
            Build(
                listOf(
                    BuildIdentifier.of("build".alpha),
                    BuildIdentifier.of("000".digits)
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
                PreReleaseIdentifier.of("alpha".alpha)
            )
        ).toString() shouldBe "1.2.3-alpha"
    }

    @Test
    fun `valid alphanumeric identifiers`() {
        AlphaNumericIdentifier("foo")
        AlphaNumericIdentifier("foo-bar")
        assertThrows<IllegalArgumentException> {
            AlphaNumericIdentifier("0000")
        }
        AlphaNumericIdentifier("0000a")
    }

    @Test
    fun `parsing versions`() {
        Version.parse("1.2.3") shouldBe Release(VersionCore.of(1, 2, 3))
        Version.parse("1.2.3+x23.005") shouldBe Release(
            VersionCore.of(1, 2, 3),
            Build(listOf(BuildIdentifier.of("x23".alpha), BuildIdentifier.of("005".digits)))
        )
        Version.parse("1.2.3-alpha.7.go-go+x23.005") shouldBe PreRelease(
            VersionCore.of(1, 2, 3),
            PreReleaseIndicator(
                listOf(
                    PreReleaseIdentifier.of("alpha".alpha),
                    PreReleaseIdentifier.of(7.numeric),
                    PreReleaseIdentifier.of("go-go".alpha)
                )
            ),
            Build(listOf(BuildIdentifier.of("x23".alpha), BuildIdentifier.of("005".digits)))
        )

        Version.parse("1.2.3-alpha.7.go-go") shouldBe PreRelease(
            VersionCore.of(1, 2, 3),
            PreReleaseIndicator(
                listOf(
                    PreReleaseIdentifier.of("alpha".alpha),
                    PreReleaseIdentifier.of(7.numeric),
                    PreReleaseIdentifier.of("go-go".alpha)
                )
            )
        )
    }
}

const val LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
const val NON_DIGITS = "-$LETTERS"
const val POSITIVE_DIGITS = "123456789"
const val DIGITS = "0$POSITIVE_DIGITS"
const val IDENTIFIER_CHARACTERS = "$NON_DIGITS$DIGITS"

private val Char.digit get() = this in DIGITS
private val Char.nonDigit get() = this in NON_DIGITS
private val Char.identifier get() = this in IDENTIFIER_CHARACTERS

@JvmInline
value class AlphaNumericIdentifier(private val value: String) : Comparable<AlphaNumericIdentifier> {
    init {
        require(value.isNotEmpty())
        require(value.all { it.identifier })
        require(value.any { it.nonDigit })
    }

    override fun compareTo(other: AlphaNumericIdentifier) = value.compareTo(other.value)
    override fun toString() = value

    companion object {
        val String.alpha get() = AlphaNumericIdentifier(this)
    }
}

@JvmInline
value class NumericIdentifier(private val number: Int) : Comparable<NumericIdentifier> {
    init {
        require(number >= 0)
    }

    override fun compareTo(other: NumericIdentifier) = number.compareTo(other.number)
    override fun toString() = number.toString()

    companion object {
        val Int.numeric get() = NumericIdentifier(this)
    }
}

@JvmInline
value class DigitIdentifier(private val value: String) {
    init {
        require(value.isNotEmpty())
        require(value.all { it.digit })
    }

    override fun toString() = value

    companion object {
        val String.digits get() = DigitIdentifier(this)
    }
}

sealed class PreReleaseIdentifier : Comparable<PreReleaseIdentifier> {
    data class AlphaNumeric(val identifier: AlphaNumericIdentifier) : PreReleaseIdentifier() {
        override fun compareTo(other: PreReleaseIdentifier) = when (other) {
            is AlphaNumeric -> identifier.compareTo(other.identifier)
            is Numeric -> 1
        }

        override fun toString() = identifier.toString()
    }

    data class Numeric(val identifier: NumericIdentifier) : PreReleaseIdentifier() {
        override fun compareTo(other: PreReleaseIdentifier) = when (other) {
            is Numeric -> identifier.compareTo(other.identifier)
            is AlphaNumeric -> -1
        }

        override fun toString() = identifier.toString()
    }

    companion object {
        fun of(identifier: AlphaNumericIdentifier) = AlphaNumeric(identifier)
        fun of(identifier: NumericIdentifier) = Numeric(identifier)
        fun parse(string: String) =
            if (string.all { it.digit }) of(NumericIdentifier(string.toInt()))
            else of(AlphaNumericIdentifier(string))
    }
}

data class PreReleaseIndicator(val identifiers: List<PreReleaseIdentifier>) : Comparable<PreReleaseIndicator> {
    init {
        require(identifiers.isNotEmpty())
    }

    override fun compareTo(other: PreReleaseIndicator): Int {
        return identifiers.asSequence().zip(other.identifiers.asSequence()) { a, b ->
            a.compareTo(b)
        }.firstOrNull { it != 0 } ?: identifiers.size.compareTo(other.identifiers.size)
    }

    override fun toString() = identifiers.joinToString(".")

    companion object {
        fun of(vararg identifiers: PreReleaseIdentifier) = PreReleaseIndicator(listOf(*identifiers))
        fun parse(string: String): PreReleaseIndicator =
            PreReleaseIndicator(string.split('.').map { PreReleaseIdentifier.parse(it) })
    }
}

private fun Int.then(compare: () -> Int) = if (this == 0) compare() else this

data class VersionCore(
    val major: NumericIdentifier, val minor: NumericIdentifier, val patch: NumericIdentifier
) : Comparable<VersionCore> {
    companion object {
        fun of(major: Int, minor: Int, patch: Int) = VersionCore(
            NumericIdentifier(major), NumericIdentifier(minor), NumericIdentifier(patch)
        )

        fun parse(string: String): VersionCore {
            val (ma, mi, pa) = string.split('.')
            return of(ma.toInt(), mi.toInt(), pa.toInt())
        }
    }

    override fun compareTo(other: VersionCore) = major.compareTo(other.major)
        .then { minor.compareTo(other.minor) }
        .then { patch.compareTo(other.patch) }

    override fun toString() = "$major.$minor.$patch"
}


sealed class BuildIdentifier {
    data class AlphaNumeric(val identifier: AlphaNumericIdentifier) : BuildIdentifier() {
        override fun toString() = identifier.toString()
    }

    data class Digits(val digits: DigitIdentifier) : BuildIdentifier() {
        override fun toString() = digits.toString()
    }

    companion object {
        fun of(identifier: AlphaNumericIdentifier) = AlphaNumeric(identifier)
        fun of(digits: DigitIdentifier) = Digits(digits)
        fun parse(string: String) =
            if (string.all { it.digit }) of(DigitIdentifier(string))
            else of(AlphaNumericIdentifier(string))
    }
}

data class Build(val identifiers: List<BuildIdentifier>) {
    init {
        require(identifiers.isNotEmpty())
    }

    override fun toString() = identifiers.joinToString(".")

    companion object {
        fun parse(suffix: String) = Build(suffix.split('.').map(BuildIdentifier.Companion::parse))

        val Build?.suffix get() = this?.let { "+$it" } ?: ""
    }
}


sealed class Version : Comparable<Version> {
    abstract val core: VersionCore
    abstract val build: Build?

    companion object {
        fun parse(string: String): Version {
            val (prefix, build) = string.indexOf('+').let {
                if (it == -1) string to null else {
                    val prefix = string.substring(0, it)
                    val buildSuffix = string.substring(it + 1)
                    val build = Build.parse(buildSuffix)
                    prefix to build
                }
            }

            val hyphen = prefix.indexOf('-')
            return if (hyphen == -1) Release(VersionCore.parse(prefix), build) else {
                PreRelease(
                    VersionCore.parse(prefix.substring(0, hyphen)),
                    PreReleaseIndicator.parse(prefix.substring(hyphen + 1)),
                    build
                )
            }
        }
    }
}

data class PreRelease(override val core: VersionCore, val pre: PreReleaseIndicator, override val build: Build? = null) :
    Version() {
    override fun compareTo(other: Version): Int {
        return when (other) {
            is PreRelease -> core.compareTo(other.core)
                .then { pre.compareTo(other.pre) }

            is Release -> core.compareTo(other.core).then { -1 }
        }
    }

    override fun toString() = "$core-$pre${build.suffix}"
}

data class Release(override val core: VersionCore, override val build: Build? = null) : Version() {
    override fun compareTo(other: Version): Int {
        return when (other) {
            is PreRelease -> -other.compareTo(this)
            is Release -> core.compareTo(other.core)
        }
    }

    override fun toString() = "$core${build.suffix}"
}