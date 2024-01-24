package de.fruiture.cor.ccs.cmd

interface Output {
    fun print(text: String)
    fun println(text: String)
}

interface IO {
    val out: Output
    val err: Output
}

object DefaultIO : IO {
    override val out = object : Output {
        override fun print(text: String) {
            kotlin.io.print(text)
        }

        override fun println(text: String) {
            kotlin.io.println(text)
        }
    }
    override val err = object : Output {
        fun String.indent() = this.lines().map { "ERROR $it" }.joinToString("\n")
        override fun print(text: String) {
            kotlin.io.print(text.indent())
        }

        override fun println(text: String) {
            kotlin.io.println(text.indent())
        }
    }
}