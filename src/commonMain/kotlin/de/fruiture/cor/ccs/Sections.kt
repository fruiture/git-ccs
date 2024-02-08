package de.fruiture.cor.ccs

import de.fruiture.cor.ccs.cc.Type

data class Sections(
    val config: Map<String, Set<Type>> = emptyMap(),
    val breakingChanges: String = "Breaking Changes"
) {
    interface TypeFilter {
        operator fun contains(type: Type): Boolean
    }

    companion object {
        fun default() = Sections(
            mapOf(
                "Features" to setOf(FEATURE_COMMIT_TYPE),
                "Bugfixes" to setOf(FIX_COMMIT_TYPE),
                "Other" to setOf(DEFAULT_COMMIT_TYPE),
            )
        )
    }

    private val allAssignedTypes = config.values.flatten().toSet()

    fun toFilter(set: Set<Type>): TypeFilter {
        val matchesDefault = set.contains(DEFAULT_COMMIT_TYPE)
        return object : TypeFilter {
            override fun contains(type: Type): Boolean {
                if (matchesDefault) {
                    if (type !in allAssignedTypes)
                        return true
                }
                return set.contains(type)
            }
        }
    }

    operator fun plus(mappings: Map<String, Set<Type>>): Sections {
        return copy(config = config + mappings)
    }

    inline fun forEach(function: (String, TypeFilter) -> Unit) {
        config.forEach {
            function(it.key, toFilter(it.value.toSet()))
        }
    }

    fun setBreakingChanges(headline: String) = copy(breakingChanges = headline)
}
