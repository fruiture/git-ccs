package de.fruiture.cor.ccs.cmd

object JvmIO : IO {
    override val out = object : Output {
        override fun print(text: String) {
            kotlin.io.print(text)
        }

        override fun println(text: String) {
            kotlin.io.println(text)
        }
    }
    override val err = object : Output {
        override fun print(text: String) {
            System.err.print(text)
        }

        override fun println(text: String) {
            System.err.println(text)
        }
    }
}