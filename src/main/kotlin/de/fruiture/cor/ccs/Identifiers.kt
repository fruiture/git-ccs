package de.fruiture.cor.ccs

private const val LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
private const val NON_DIGITS = "-$LETTERS"
private const val POSITIVE_DIGITS = "123456789"
private const val DIGITS = "0$POSITIVE_DIGITS"
private const val IDENTIFIER_CHARACTERS = "$NON_DIGITS$DIGITS"

internal val Char.digit get() = this in DIGITS
internal  val Char.nonDigit get() = this in NON_DIGITS
internal  val Char.identifier get() = this in IDENTIFIER_CHARACTERS

@JvmInline
value class AlphaNumericIdentifier(private val value: String) : Comparable<AlphaNumericIdentifier> {
    init {
        require(value.isNotEmpty())
        require(value.all { it.identifier })
        require(value.any { it.nonDigit })
    }

    override fun compareTo(other: AlphaNumericIdentifier) = value.compareTo(other.value)
    override fun toString() = value

    companion object {
        val String.alphanumeric get() = AlphaNumericIdentifier(this)
    }
}

@JvmInline
value class NumericIdentifier(private val value: Int) : Comparable<NumericIdentifier> {
    init {
        require(value >= 0)
    }

    override fun compareTo(other: NumericIdentifier) = value.compareTo(other.value)
    override fun toString() = value.toString()
    operator fun plus(i: Int): NumericIdentifier {
        require(i >= 0)
        return NumericIdentifier(value + i)
    }

    companion object {
        val Int.numeric get() = NumericIdentifier(this)
    }
}

@JvmInline
value class DigitIdentifier(private val value: String) {
    init {
        require(value.isNotEmpty())
        require(value.all { it.digit })
    }

    override fun toString() = value

    companion object {
        val String.digits get() = DigitIdentifier(this)
    }
}