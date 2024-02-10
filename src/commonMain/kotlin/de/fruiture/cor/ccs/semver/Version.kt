package de.fruiture.cor.ccs.semver

import de.fruiture.cor.ccs.semver.Build.Companion.add
import de.fruiture.cor.ccs.semver.Build.Companion.suffix
import de.fruiture.cor.ccs.semver.NumericIdentifier.Companion.numeric
import de.fruiture.cor.ccs.semver.PreReleaseIndicator.Strategy.Companion.counter

enum class ChangeType {
    NONE, PATCH, MINOR, MAJOR
}

private fun Int.then(compare: () -> Int) = if (this == 0) compare() else this

internal data class VersionCore(
    val major: NumericIdentifier, val minor: NumericIdentifier, val patch: NumericIdentifier
) : Comparable<VersionCore> {
    init {
        require((major.zero && minor.zero && patch.zero).not())
    }

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

    fun bump(type: ChangeType) = when (type) {
        ChangeType.NONE -> this
        ChangeType.PATCH -> copy(patch = patch + 1)
        ChangeType.MINOR -> copy(minor = minor + 1, patch = 0.numeric)
        ChangeType.MAJOR -> copy(major = major + 1, minor = 0.numeric, patch = 0.numeric)
    }

    val type: ChangeType by lazy {
        if (patch.nonZero) ChangeType.PATCH
        else if (minor.nonZero) ChangeType.MINOR
        else if (major.nonZero) ChangeType.MAJOR
        else throw IllegalStateException("v0.0.0 is not defined")
    }
}

private val searchRegexp = Regex("\\d+\\.\\d+\\.\\d+(?:[-+.\\w]+)?")

sealed class Version : Comparable<Version> {
    abstract val release: Release
    abstract val build: Build?

    internal abstract val core: VersionCore
    val type get() = core.type

    abstract fun build(suffix: String): Version
    abstract operator fun plus(preRelease: PreReleaseIndicator): PreRelease
    abstract operator fun plus(build: Build): Version

    abstract fun next(type: ChangeType): Release
    internal abstract fun nextPreRelease(strategy: PreReleaseIndicator.Strategy = counter()): PreRelease
    abstract fun nextPreRelease(type: ChangeType, strategy: PreReleaseIndicator.Strategy = counter()): PreRelease

    companion object {
        val initial = Release(VersionCore.of(0, 0, 1))
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

        fun extractVersion(string: String) = searchRegexp.find(string)?.let { match ->
            runCatching { version(match.value) }.getOrNull()
        }
    }
}

data class PreRelease internal constructor(
    override val core: VersionCore,
    val pre: PreReleaseIndicator,
    override val build: Build? = null
) :
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

    override fun nextPreRelease(strategy: PreReleaseIndicator.Strategy) =
        copy(pre = strategy.next(pre), build = null)

    override fun next(type: ChangeType): Release {
        val changeToHere = release.type
        return if (changeToHere >= type) release
        else release.next(type)
    }


    override fun nextPreRelease(type: ChangeType, strategy: PreReleaseIndicator.Strategy): PreRelease {
        val changeToHere = release.type
        return if (changeToHere >= type) nextPreRelease(strategy)
        else release.next(type).nextPreRelease(strategy)
    }
}

data class Release internal constructor(override val core: VersionCore, override val build: Build? = null) : Version() {
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

    override fun next(type: ChangeType) = Release(core.bump(type))
    override fun nextPreRelease(strategy: PreReleaseIndicator.Strategy) =
        PreRelease(core, strategy.start())

    override fun nextPreRelease(type: ChangeType, strategy: PreReleaseIndicator.Strategy) =
        next(type).nextPreRelease(strategy)
}
