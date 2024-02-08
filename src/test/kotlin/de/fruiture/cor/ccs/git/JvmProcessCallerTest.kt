package de.fruiture.cor.ccs.git

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.concurrent.TimeoutException
import kotlin.test.Test

class JvmProcessCallerTest {

    @Test
    fun `invoke git -v`() {
        val result = JvmProcessCaller().call("git", listOf("-v"))

        result.code shouldBe 0
        result.stderr shouldBe emptyList()
        result.stdout.first() shouldContain Regex("^git version")
    }

    @Test
    fun `invoke git -invalid-option`() {
        val result = JvmProcessCaller().call("git", listOf("-invalid-option"))

        result.code shouldBe 129
        result.stderr.first() shouldBe "unknown option: -invalid-option"
    }

    @Test
    fun `invoke sleep and hit timeout`() {
        shouldThrow<TimeoutException> { JvmProcessCaller(50).call("sleep", listOf("1")) }
    }
}