package de.fruiture.cor.ccs

import de.fruiture.cor.ccs.cc.Type

data class Headlines(
    val types: Map<Type, String> = mapOf(
        Type("feat") to "Features",
        Type("fix") to "Bugfixes",
    ),
    private val breakingChanges: String = "Breaking Changes"
) {
    operator fun plus(mappings: Map<String, List<Type>>): Headlines {
        return copy(types = types + mappings.flatMap { (headline, types) -> types.map { it to headline } })
    }

    inline fun forEach(function: (String, Set<Type>) -> Unit) {
        val hl = types.map { (t, hl) -> hl to t }
            .groupBy { it.first }
            .mapValues { e -> e.value.map { it.second } }
        hl.forEach {
            function(it.key, it.value.toSet())
        }
    }
}
