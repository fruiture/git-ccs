package de.fruiture.cor.ccs.git

interface System {
    fun call(command: String, arguments: List<String> = emptyList()): SystemCallResult
}

data class SystemCallResult(
    val code: Int,
    val stdout: List<String> = emptyList(),
    val stderr: List<String> = emptyList()
)