package de.fruiture.cor.ccs.git

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.util.concurrent.TimeoutException

@EnabledOnOs(OS.LINUX, OS.MAC)
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
        assertThrows<TimeoutException> { JvmProcessCaller(50).call("sleep", listOf("1")) }
    }
}