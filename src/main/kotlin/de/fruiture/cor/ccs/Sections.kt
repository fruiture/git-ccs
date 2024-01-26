package de.fruiture.cor.ccs

import de.fruiture.cor.ccs.cc.Type

data class Sections(
    val config: Map<String, Set<Type>> = emptyMap(),
    val breakingChanges: String = "Breaking Changes"
) {
    companion object {
        fun default() = Sections(
            mapOf(
                "Features" to setOf(Type("feat")),
                "Bugfixes" to setOf(Type("fix")),
            )
        )
    }

    operator fun plus(mappings: Map<String, Set<Type>>): Sections {
        return copy(config = config + mappings)
    }

    inline fun forEach(function: (String, Set<Type>) -> Unit) {
        config.forEach {
            function(it.key, it.value.toSet())
        }
    }

    fun setBreakingChanges(headline: String) = copy(breakingChanges = headline)
}
