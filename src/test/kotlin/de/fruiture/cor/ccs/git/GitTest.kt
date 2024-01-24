package de.fruiture.cor.ccs.git

import de.fruiture.cor.ccs.semver.Version.Companion.version
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime

class GitTest {

    @Test
    fun `find the latest version tag`() {
        val sys = object : SystemCaller {
            override fun call(command: String, arguments: List<String>): SystemCallResult {
                command shouldBe "git"
                arguments shouldBe listOf("describe", "--tags", "--match=*.*.*", "--abbrev=0", "HEAD")

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
        val sys = object : SystemCaller {
            override fun call(command: String, arguments: List<String>): SystemCallResult {
                command shouldBe "git"
                arguments shouldBe listOf(
                    "describe",
                    "--tags",
                    "--match=*.*.*",
                    "--exclude=*-*",
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
        val sys = object : SystemCaller {
            override fun call(command: String, arguments: List<String>): SystemCallResult {
                command shouldBe "git"
                arguments shouldBe listOf("describe", "--tags", "--match=*.*.*", "--abbrev=0", "HEAD")

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
        val sys = object : SystemCaller {
            override fun call(command: String, arguments: List<String>): SystemCallResult {
                return SystemCallResult(
                    code = 127,
                    stderr = listOf("command not found: $command")
                )
            }
        }

        assertThrows<RuntimeException> { Git(sys).getLatestVersion() }
    }

    @Test
    fun `get machine readable log`() {
        val sys = object : SystemCaller {
            override fun call(command: String, arguments: List<String>): SystemCallResult {
                command shouldBe "git"
                arguments shouldBe listOf("log", "--format=format:%H %aI%n%B%n", "-z", "1.0.0..HEAD")

                return SystemCallResult(
                    code = 0,
                    stdout = """
                        948f00f8b349c6f9652809f924254ffe7a497227 2024-01-20T21:51:33+01:00
                        feat: blablabla

                        BREAKING CHANGE: did something dudu here

                        ${Char.MIN_VALUE}b8d181d9e803da9ceba0c3c4918317124d678656 2024-01-20T21:31:01+01:00
                        non conventional commit
                    """.trimIndent().lines()
                )
            }
        }

        Git(sys).getLog(
            from = version("1.0.0")
        ) shouldBe listOf(
            GitCommit(
                hash = "948f00f8b349c6f9652809f924254ffe7a497227",
                date = ZonedDateTime.parse("2024-01-20T21:51:33+01:00"),
                message = """
                    feat: blablabla

                    BREAKING CHANGE: did something dudu here
                """.trimIndent()
            ),
            GitCommit(
                hash = "b8d181d9e803da9ceba0c3c4918317124d678656",
                date = ZonedDateTime.parse("2024-01-20T21:31:01+01:00"),
                message = "non conventional commit"
            )
        )
    }

    @Test
    fun `get full log`() {
        val sys = object : SystemCaller {
            override fun call(command: String, arguments: List<String>): SystemCallResult {
                command shouldBe "git"
                arguments shouldBe listOf("log", "--format=format:%H %aI%n%B%n", "-z", "HEAD")

                return SystemCallResult(
                    code = 0,
                    stdout = """
                        b8d181d9e803da9ceba0c3c4918317124d678656 2024-01-20T21:31:01+01:00
                        non conventional commit
                    """.trimIndent().lines()
                )
            }
        }

        Git(sys).getLog() shouldBe listOf(
            GitCommit(
                hash = "b8d181d9e803da9ceba0c3c4918317124d678656",
                date = ZonedDateTime.parse("2024-01-20T21:31:01+01:00"),
                message = "non conventional commit"
            )
        )
    }
}