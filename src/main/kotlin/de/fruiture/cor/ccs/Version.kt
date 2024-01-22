package de.fruiture.cor.ccs

import de.fruiture.cor.ccs.AlphaNumericIdentifier.Companion.alphanumeric
import de.fruiture.cor.ccs.Build.Companion.add
import de.fruiture.cor.ccs.Build.Companion.suffix
import de.fruiture.cor.ccs.NumericIdentifier.Companion.numeric
import de.fruiture.cor.ccs.PreReleaseIdentifier.Companion.identifier

private const val LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
private const val NON_DIGITS = "-$LETTERS"
private const val POSITIVE_DIGITS = "123456789"
private const val DIGITS = "0$POSITIVE_DIGITS"
private const val IDENTIFIER_CHARACTERS = "$NON_DIGITS$DIGITS"

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
        val String.alphanumeric get() = AlphaNumericIdentifier(this)
    }
}

@JvmInline
value class NumericIdentifier(private val number: Int) : Comparable<NumericIdentifier> {
    init {
        require(number >= 0)
    }

    override fun compareTo(other: NumericIdentifier) = number.compareTo(other.number)
    override fun toString() = number.toString()
    operator fun plus(i: Int): NumericIdentifier {
        require(i >= 0)
        return NumericIdentifier(number + i)
    }

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
        fun bump() = Numeric(identifier + 1)
    }

    companion object {
        fun identifier(identifier: AlphaNumericIdentifier) = AlphaNumeric(identifier)
        fun identifier(identifier: NumericIdentifier) = Numeric(identifier)
        fun parse(string: String) =
            if (string.all { it.digit }) identifier(NumericIdentifier(string.toInt()))
            else identifier(AlphaNumericIdentifier(string))
    }
}

data class PreReleaseIndicator(val identifiers: List<PreReleaseIdentifier>) : Comparable<PreReleaseIndicator> {
    init {
        require(identifiers.isNotEmpty()) { "at least one identifier required" }
    }

    override fun compareTo(other: PreReleaseIndicator): Int {
        return identifiers.asSequence().zip(other.identifiers.asSequence()) { a, b ->
            a.compareTo(b)
        }.firstOrNull { it != 0 } ?: identifiers.size.compareTo(other.identifiers.size)
    }

    override fun toString() = identifiers.joinToString(".")
    operator fun plus(other: PreReleaseIndicator) =
        PreReleaseIndicator(this.identifiers + other.identifiers)

    fun bump(identifier: PreReleaseIdentifier? = null) =
        if (identifier != null) bumpSpecific(identifier)
        else bumpSpecific(findBumpKey() ?: DEFAULT_PRERELEASE)

    private fun findBumpKey(): PreReleaseIdentifier? {
        identifiers.zipWithNext { key, value ->
            if (key is PreReleaseIdentifier.AlphaNumeric && value is PreReleaseIdentifier.Numeric) {
                return key
            }
        }

        return identifiers.lastOrNull { it is PreReleaseIdentifier.AlphaNumeric }
    }

    private fun bumpSpecific(identifier: PreReleaseIdentifier): PreReleaseIndicator {
        val replaced = sequence {
            var i = 0
            var found = false

            while (i < identifiers.size) {
                val k = identifiers[i]
                if (k == identifier) {
                    found = true
                    if (i + 1 < identifiers.size) {
                        val v = identifiers[i + 1]
                        if (v is PreReleaseIdentifier.Numeric) {
                            yield(k)
                            yield(v.bump())
                            i += 2
                            continue
                        }
                    }

                    yield(k)
                    yield(PreReleaseIdentifier.Numeric(2.numeric))
                    i += 1
                    continue
                }

                yield(k)
                i += 1
                continue
            }

            if (!found) {
                yield(identifier)
                yield(identifier(1.numeric))
            }
        }.toList()

        return copy(identifiers = replaced)
    }

    companion object {
        fun of(vararg identifiers: PreReleaseIdentifier) = PreReleaseIndicator(listOf(*identifiers))
        fun preRelease(string: String): PreReleaseIndicator =
            PreReleaseIndicator(string.split('.').map { PreReleaseIdentifier.parse(it) })

        private val DEFAULT_PRERELEASE = identifier("SNAPSHOT".alphanumeric)

        fun start(identifier: PreReleaseIdentifier?) =
            of(identifier ?: DEFAULT_PRERELEASE, identifier(1.numeric))
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

    fun bump(type: ChangeType): VersionCore {
        return when (type) {
            ChangeType.PATCH -> copy(patch = patch + 1)
            ChangeType.MINOR -> copy(minor = minor + 1, patch = 0.numeric)
            ChangeType.MAJOR -> copy(major = major + 1, minor = 0.numeric, patch = 0.numeric)
        }
    }

    operator fun minus(other: VersionCore) = when {
        major != other.major -> ChangeType.MAJOR
        minor != other.minor -> ChangeType.MINOR
        patch != other.patch -> ChangeType.PATCH
        else -> null
    }
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

    operator fun plus(build: Build): Build = Build(identifiers + build.identifiers)

    companion object {
        fun build(suffix: String) = Build(suffix.split('.').map(BuildIdentifier.Companion::parse))

        val Build?.suffix get() = this?.let { "+$it" } ?: ""

        infix fun Build?.add(build: Build) = this?.let { it + build } ?: build
    }
}

sealed class Version : Comparable<Version> {
    abstract val release: Release

    abstract val core: VersionCore
    abstract val build: Build?

    fun bump(type: ChangeType) = Release(core.bump(type))

    abstract fun build(suffix: String): Version
    abstract operator fun plus(preRelease: PreReleaseIndicator): PreRelease
    abstract operator fun plus(build: Build): Version
    abstract fun bumpPreRelease(identifier: PreReleaseIdentifier? = null): PreRelease

    operator fun minus(version: Version) = core - version.core

    companion object {
        val initial = Release(VersionCore.of(0, 0, 0))
        val stable = Release(VersionCore.of(1, 0, 0))

        fun version(string: String): Version {
            val (prefix, build) = string.indexOf('+').let {
                if (it == -1) string to null else {
                    val prefix = string.substring(0, it)
                    val buildSuffix = string.substring(it + 1)
                    val build = Build.build(buildSuffix)
                    prefix to build
                }
            }

            val hyphen = prefix.indexOf('-')
            return if (hyphen == -1) Release(VersionCore.parse(prefix), build) else {
                PreRelease(
                    VersionCore.parse(prefix.substring(0, hyphen)),
                    PreReleaseIndicator.preRelease(prefix.substring(hyphen + 1)),
                    build
                )
            }
        }
    }
}

data class PreRelease(override val core: VersionCore, val pre: PreReleaseIndicator, override val build: Build? = null) :
    Version() {
    override val release = Release(core)

    override fun compareTo(other: Version): Int {
        return when (other) {
            is PreRelease -> core.compareTo(other.core)
                .then { pre.compareTo(other.pre) }

            is Release -> core.compareTo(other.core).then { -1 }
        }
    }

    override fun toString() = "$core-$pre${build.suffix}"

    override fun build(suffix: String) = copy(build = Build.build(suffix))

    override fun plus(preRelease: PreReleaseIndicator) = copy(pre = pre + preRelease)
    override fun plus(build: Build) = copy(build = this.build add build)

    override fun bumpPreRelease(identifier: PreReleaseIdentifier?) = copy(pre = pre.bump(identifier), build = null)

    fun bumpPreRelease(lastRelease: Release, type: ChangeType, identifier: PreReleaseIdentifier? = null): PreRelease {
        val changeToHere = (release - lastRelease) ?: ChangeType.PATCH
        return if (changeToHere >= type)
            bumpPreRelease(identifier)
        else
            release.bump(type).bumpPreRelease(identifier)
    }
}

data class Release(override val core: VersionCore, override val build: Build? = null) : Version() {
    override val release = this
    override fun compareTo(other: Version): Int {
        return when (other) {
            is PreRelease -> -other.compareTo(this)
            is Release -> core.compareTo(other.core)
        }
    }

    override fun toString() = "$core${build.suffix}"

    fun preRelease(suffix: String) = plus(PreReleaseIndicator.preRelease(suffix))

    override fun build(suffix: String) = copy(build = Build.build(suffix))
    override fun plus(preRelease: PreReleaseIndicator) = PreRelease(core, preRelease, build)
    override fun plus(build: Build) = copy(build = this.build add build)
    override fun bumpPreRelease(identifier: PreReleaseIdentifier?) =
        PreRelease(core, PreReleaseIndicator.start(identifier))
}