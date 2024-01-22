package de.fruiture.cor.ccs.cc

import de.fruiture.cor.ccs.cc.Scope.Companion.suffix

@JvmInline
value class Type(private val value: String) {
    init {
        require(value.isNotBlank())
    }

    override fun toString() = value
}

@JvmInline
value class Description(val value: String) {
    init {
        require(value.isNotBlank())
    }

    override fun toString() = value
}


@JvmInline
value class Body(private val value: String) {
    constructor(paragraphs: List<String>) : this(paragraphs.joinToString("\n\n"))

    init {
        require(value.isNotBlank())
    }

    override fun toString() = value
}

sealed class Footer {
    abstract val key: String
    abstract val value: String

    override fun toString() = "$key: $value"
}

data class GenericFooter(override val key: String, override val value: String) : Footer() {
    init {
        require(key.none { it.isWhitespace() })
        require(value.isNotBlank())
    }

    override fun toString() = super.toString()
}

private val VALID_KEYS = listOf("BREAKING CHANGE", "BREAKING-CHANGE")
private val DEFAULT_KEY = VALID_KEYS.first()

data class BreakingChange(override val key: String, override val value: String) : Footer() {
    constructor(value: String) : this(DEFAULT_KEY, value)

    init {
        require(key in VALID_KEYS)
        require(value.isNotBlank())
    }

    override fun toString() = super.toString()
}

@JvmInline
value class Scope(private val value: String) {
    init {
        require(value.isNotBlank())
    }

    override fun toString() = value

    companion object {
        val Scope?.suffix get() = if (this == null) "" else "($this)"
    }
}

data class ConventionalCommitMessage(
    val type: Type,
    val description: Description,
    val scope: Scope? = null,
    val body: Body? = null,
    val footers: List<Footer> = emptyList(),
    val headlineBreakingChange: Boolean = false
) {
    private val exclamation: String get() = if (headlineBreakingChange) "!" else ""

    val breakingChange: BreakingChange? by lazy {
        if (headlineBreakingChange) BreakingChange(description.value)
        else footers.filterIsInstance<BreakingChange>().firstOrNull()
    }

    companion object {
        fun message(text: String): ConventionalCommitMessage {
            val paragraphs = text.split(Regex("\\n\\n"))

            val headline = headline(paragraphs.first())
            val tail = paragraphs.drop(1)

            return if (tail.isNotEmpty()) {
                val footers = footers(tail.last())

                if (footers.isNotEmpty()) {
                    val body = tail.dropLast(1)
                    if (body.isNotEmpty())
                        headline + Body(body) + footers
                    else
                        headline + footers
                } else {
                    headline + Body(tail)
                }
            } else {
                headline
            }
        }

        private fun headline(headline: String): ConventionalCommitMessage {
            return Regex("(\\w+)(?:\\((\\w+)\\))?(!)?: (.+)").matchEntire(headline)?.destructured?.let { (type, scope, excl, description) ->
                return ConventionalCommitMessage(
                    type = Type(type),
                    description = Description(description),
                    scope = if (scope.isNotEmpty()) Scope(scope) else null,
                    headlineBreakingChange = excl.isNotEmpty()
                )
            } ?: throw IllegalArgumentException("no valid headline: '$headline‘")
        }

        private fun footers(paragraph: String) =
            Regex("^([\\S-]+|BREAKING CHANGE): (.+?)$", RegexOption.MULTILINE).findAll(paragraph)
                .map { it.destructured }
                .map { (k, v) ->
                    footer(k, v)
                }.toList()

        private fun footer(k: String, v: String) =
            if (k == "BREAKING CHANGE" || k == "BREAKING-CHANGE") BreakingChange(k, v)
            else GenericFooter(k, v)
    }

    private operator fun plus(footers: List<Footer>) =
        copy(footers = this.footers + footers)

    private operator fun plus(body: Body) = copy(body = body)

    override fun toString(): String {
        val out = StringBuilder()
        out.append(headline())

        body?.let {
            out.appendLine()
            out.appendLine()
            out.append(it)
        }

        if (footers.isNotEmpty()) {
            out.appendLine()
            out.appendLine()
            out.append(footers.joinToString("\n"))
        }
        return out.toString()
    }

    private fun headline() = "$type${scope.suffix}$exclamation: $description"
}
