package de.fruiture.cor.ccs

import de.fruiture.cor.ccs.git.System
import de.fruiture.cor.ccs.git.SystemCallResult
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AppTest {

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

    @Test
    fun `get next release version`() {
        App(sys).getNextRelease() shouldBe "1.1.0"
    }

    @Test
    fun `get next pre-release version`() {
        App(sys).getNextPreRelease() shouldBe "1.1.0-SNAPSHOT.1"
        App(sys).getNextPreRelease("alpha") shouldBe "1.1.0-alpha.1"
    }
}