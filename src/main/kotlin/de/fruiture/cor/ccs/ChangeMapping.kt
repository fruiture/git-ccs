package de.fruiture.cor.ccs

import de.fruiture.cor.ccs.cc.Type
import de.fruiture.cor.ccs.git.GitCommit
import de.fruiture.cor.ccs.semver.ChangeType

val DEFAULT_COMMIT_TYPE = Type("default")

data class ChangeMapping(
    private val types: Map<Type, ChangeType> = mapOf(
        Type("feat") to ChangeType.MINOR,
        Type("fix") to ChangeType.PATCH,
        DEFAULT_COMMIT_TYPE to ChangeType.PATCH
    )
) {
    private val defaultChange = types[DEFAULT_COMMIT_TYPE]!!

    fun of(history: List<GitCommit>): ChangeType = history.maxOfOrNull(::of) ?: defaultChange

    fun of(commit: GitCommit) =
        if (commit.hasBreakingChange) ChangeType.MAJOR
        else of(commit.type)

    fun of(type: Type) = types.getOrDefault(type, defaultChange)

    operator fun plus(type: Pair<Type, ChangeType>) =
        copy(types = types + type)

    fun add(changeType: ChangeType, types: List<Type>) =
        copy(types = this.types + types.associateWith { changeType })
}