package de.fruiture.cor.ccs.cmd

interface Output {
    fun print(text: String)
    fun println(text: String)
}

interface IO {
    val out: Output
    val err: Output
}
