package de.fruiture.cor.ccs.semver

sealed class BuildIdentifier {
    data class AlphaNumeric(val identifier: AlphaNumericIdentifier) : BuildIdentifier() {
        override fun toString() = identifier.toString()
    }

    data class Digits(val digits: DigitIdentifier) : BuildIdentifier() {
        override fun toString() = digits.toString()
    }

    companion object {
        fun identifier(identifier: AlphaNumericIdentifier) = AlphaNumeric(identifier)
        fun identifier(digits: DigitIdentifier) = Digits(digits)
        fun identifier(string: String) =
            if (string.all { it.digit }) identifier(DigitIdentifier(string))
            else identifier(AlphaNumericIdentifier(string))
    }
}

data class Build(val identifiers: List<BuildIdentifier>) {
    init {
        require(identifiers.isNotEmpty())
    }

    override fun toString() = identifiers.joinToString(".")

    operator fun plus(build: Build): Build = Build(identifiers + build.identifiers)

    companion object {
        fun build(suffix: String) = Build(suffix.split('.').map(BuildIdentifier.Companion::identifier))

        val Build?.suffix get() = this?.let { "+$it" } ?: ""

        infix fun Build?.add(build: Build) = this?.let { it + build } ?: build
    }
}