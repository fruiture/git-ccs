package de.fruiture.cor.ccs.semver

import de.fruiture.cor.ccs.semver.AlphaNumericIdentifier.Companion.alphanumeric
import de.fruiture.cor.ccs.semver.NumericIdentifier.Companion.numeric
import de.fruiture.cor.ccs.semver.PreReleaseIdentifier.Companion.identifier

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
        fun identifier(string: String) =
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
            PreReleaseIndicator(string.split('.').map { identifier(it) })

        private val DEFAULT_PRERELEASE = identifier("SNAPSHOT".alphanumeric)

        internal fun start(identifier: PreReleaseIdentifier?) =
            of(identifier ?: DEFAULT_PRERELEASE, identifier(1.numeric))
    }
}