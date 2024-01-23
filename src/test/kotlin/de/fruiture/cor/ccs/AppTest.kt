package de.fruiture.cor.ccs

import de.fruiture.cor.ccs.git.System
import de.fruiture.cor.ccs.git.SystemCallResult
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AppTest {

    @Test
    fun `get next release version`() {
        val sys = object : System {
            override fun call(command: String, arguments: List<String>): SystemCallResult {
                return if (arguments.first() == "describe")
                    SystemCallResult(
                        code = 0,
                        stdout = listOf("1.0.0")
                    )
                else if (arguments.first() == "log" && arguments.last() == "1.0.0..HEAD") {
                    SystemCallResult(
                        code = 0,
                        stdout = """
                            cafebabe 2001-01-01T13:00:00Z
                            feat: a feature is born
                        """.trimIndent().lines()
                    )
                } else throw IllegalArgumentException()
            }
        }

        App(sys).getNextRelease() shouldBe "1.1.0"
    }
}