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

    companion object {
        fun of(vararg identifiers: PreReleaseIdentifier) = PreReleaseIndicator(listOf(*identifiers))
        fun preRelease(string: String): PreReleaseIndicator =
            PreReleaseIndicator(string.split('.').map { identifier(it) })
    }

    interface Strategy {
        fun next(indicator: PreReleaseIndicator): PreReleaseIndicator
        fun start(): PreReleaseIndicator

        companion object {
            val DEFAULT_STATIC = "SNAPSHOT".alphanumeric
            val DEFAULT_COUNTER = "RC".alphanumeric

            fun counter(identifier: AlphaNumericIdentifier = DEFAULT_COUNTER): Strategy = CounterStrategy(identifier)

            private data class CounterStrategy(val identifier: AlphaNumericIdentifier) : Strategy {
                override fun next(indicator: PreReleaseIndicator) =
                    indicator.bumpCounter(identifier(identifier))

                override fun start() =
                    of(identifier(identifier), identifier(1.numeric))
            }

            private fun PreReleaseIndicator.bumpCounter(identifier: PreReleaseIdentifier): PreReleaseIndicator {
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

            fun static(identifier: AlphaNumericIdentifier = DEFAULT_STATIC): Strategy = Static(identifier)

            private data class Static(val identifier: AlphaNumericIdentifier) : Strategy {
                override fun next(indicator: PreReleaseIndicator) =
                    identifier(identifier).let {
                        if (it in indicator.identifiers) indicator else indicator + of(it)
                    }

                override fun start() = of(identifier(identifier))
            }

            operator fun Strategy.plus(then: Strategy): Strategy = Combined(this, then)

            fun deduct(spec: String): Strategy {
                val identifiers = preRelease(spec).identifiers
                return sequence {
                    var i = 0
                    while (i < identifiers.size) {
                        val k = identifiers[i]
                        if (k is PreReleaseIdentifier.AlphaNumeric) {
                            if (i + 1 < identifiers.size) {
                                val v = identifiers[i + 1]
                                if (v is PreReleaseIdentifier.Numeric) {
                                    this.yield(counter(k.identifier))
                                    i += 2
                                    continue
                                }
                            }

                            this.yield(static(k.identifier))
                        }
                        i++
                    }
                }.reduce { a, b -> a + b }
            }

            private data class Combined(val first: Strategy, val second: Strategy) : Strategy {
                override fun next(indicator: PreReleaseIndicator): PreReleaseIndicator {
                    return indicator.let(first::next).let(second::next)
                }

                override fun start(): PreReleaseIndicator {
                    return first.start().let(second::next)
                }
            }
        }
    }
}


