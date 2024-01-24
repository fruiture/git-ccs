package de.fruiture.cor.ccs.git

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class JvmProcessCaller(private val timeoutMillis: Long = 1_000) : SystemCaller {
    override fun call(command: String, arguments: List<String>): SystemCallResult {
        val process = ProcessBuilder(listOf(command) + arguments).start()
        try {
            if (process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
                return SystemCallResult(
                    process.exitValue(),
                    stdout = process.inputReader().lines().toList(),
                    stderr = process.errorReader().lines().toList()
                )
            } else {
                throw TimeoutException("running $command $arguments exceeded the timeout")
            }
        } finally {
            process.destroy()
        }
    }
}
