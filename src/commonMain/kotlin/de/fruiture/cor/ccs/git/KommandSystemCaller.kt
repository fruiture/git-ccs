package de.fruiture.cor.ccs.git

import com.kgit2.kommand.process.Command
import com.kgit2.kommand.process.Stdio

class KommandSystemCaller : SystemCaller {

    override fun call(command: String, arguments: List<String>): SystemCallResult {
        val child = Command(command)
            .args(arguments)
            .stdout(Stdio.Pipe)
            .stderr(Stdio.Pipe)
            .spawn()

        val code = child.wait()

        return SystemCallResult(
            code = code,
            stdout = child.bufferedStdout()?.lines()?.toList() ?: emptyList(),
            stderr = child.bufferedStderr()?.lines()?.toList() ?: emptyList()
        )
    }
}