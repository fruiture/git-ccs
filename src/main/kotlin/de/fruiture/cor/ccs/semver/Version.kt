package de.fruiture.cor.ccs.semver

import de.fruiture.cor.ccs.semver.Build.Companion.add
import de.fruiture.cor.ccs.semver.Build.Companion.suffix
import de.fruiture.cor.ccs.semver.NumericIdentifier.Companion.numeric

private fun Int.then(compare: () -> Int) = if (this == 0) compare() else this

internal data class VersionCore(
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

sealed class Version : Comparable<Version> {
    abstract val release: Release

    internal abstract val core: VersionCore
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

    override fun bumpPreRelease(identifier: PreReleaseIdentifier?) = copy(pre = pre.bump(identifier), build = null)

    fun bumpPreRelease(lastRelease: Release, type: ChangeType, identifier: PreReleaseIdentifier? = null): PreRelease {
        val changeToHere = (release - lastRelease) ?: ChangeType.PATCH
        return if (changeToHere >= type)
            bumpPreRelease(identifier)
        else
            release.bump(type).bumpPreRelease(identifier)
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
    override fun bumpPreRelease(identifier: PreReleaseIdentifier?) =
        PreRelease(core, PreReleaseIndicator.start(identifier))
}

enum class ChangeType {
    PATCH, MINOR, MAJOR
}