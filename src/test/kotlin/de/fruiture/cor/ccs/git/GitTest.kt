package de.fruiture.cor.ccs.git

import de.fruiture.cor.ccs.semver.Version.Companion.version
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GitTest {

    @Test
    fun `find the latest version tag`() {
        val sys = object : System {
            override fun call(command: String, arguments: List<String>): SystemCallResult {
                command shouldBe "git"
                arguments shouldBe listOf("describe", "--tags", "--match=\"*.*.*\"", "--abbrev=0", "HEAD")

                return SystemCallResult(
                    code = 0,
                    stdout = listOf("1.0.0-alpha"),
                    stderr = emptyList()
                )
            }
        }

        Git(sys).getLatestVersion() shouldBe version("1.0.0-alpha")
    }

    @Test
    fun `get latest release version tag`() {
        val sys = object : System {
            override fun call(command: String, arguments: List<String>): SystemCallResult {
                command shouldBe "git"
                arguments shouldBe listOf(
                    "describe",
                    "--tags",
                    "--match=\"*.*.*\"",
                    "--exclude \"*-*\"",
                    "--abbrev=0",
                    "HEAD"
                )

                return SystemCallResult(
                    code = 0,
                    stdout = listOf("1.0.0"),
                    stderr = emptyList()
                )
            }
        }

        Git(sys).getLatestRelease() shouldBe version("1.0.0")
    }

    @Test
    fun `no release found`() {
        val sys = object : System {
            override fun call(command: String, arguments: List<String>): SystemCallResult {
                command shouldBe "git"
                arguments shouldBe listOf("describe", "--tags", "--match=\"*.*.*\"", "--abbrev=0", "HEAD")

                return SystemCallResult(
                    code = 128,
                    stderr = listOf(
                        "fatal: No tags can describe 'e2b5139bcdfcab80b8ea7f225f511dc3df9ee24e'.",
                        "Try --always, or create some tags."
                    )
                )
            }
        }

        Git(sys).getLatestVersion() shouldBe null
    }

    @Test
    fun `any error`() {
        val sys = object : System {
            override fun call(command: String, arguments: List<String>): SystemCallResult {
                return SystemCallResult(
                    code = 127,
                    stderr = listOf("command not found: $command")
                )
            }
        }

        assertThrows<RuntimeException> { Git(sys).getLatestVersion() }
    }
}