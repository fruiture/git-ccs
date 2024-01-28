package de.fruiture.cor.ccs.git

import de.fruiture.cor.ccs.semver.Version.Companion.version
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime

class GitTest {

    private val forEachRef = listOf(
        "for-each-ref",
        "--merged", "HEAD",
        "--sort=-committerdate",
        "--format=%(refname:short)",
        "refs/tags/*.*.*"
    )

    @Test
    fun `get latest version or release using git for-each-ref`() {
        val sys = mockk<SystemCaller>().apply {
            every { call("git", forEachRef) } returns SystemCallResult(
                code = 0, stdout = listOf(
                    "2.1.0-SNAPSHOT.1",
                    "2.1.0-SNAPSHOT.2",
                    "2.0.0",
                    "1.3.9",
                    "1.3.9-RC.7"
                )
            )
        }

        Git(sys).getLatestVersion() shouldBe version("2.1.0-SNAPSHOT.2")
        Git(sys).getLatestVersion(before = version("2.1.0-SNAPSHOT.2")) shouldBe version("2.1.0-SNAPSHOT.1")
        Git(sys).getLatestVersion(before = version("2.1.0-SNAPSHOT.1")) shouldBe version("2.0.0")

        Git(sys).getLatestRelease() shouldBe version("2.0.0")
        Git(sys).getLatestRelease(before = version("2.1.0-SNAPSHOT.2")) shouldBe version("2.0.0")
        Git(sys).getLatestRelease(before = version("2.0.0")) shouldBe version("1.3.9")
        Git(sys).getLatestRelease(before = version("1.3.9")) shouldBe null
        Git(sys).getLatestVersion(before = version("1.3.9")) shouldBe version("1.3.9-RC.7")
        Git(sys).getLatestVersion(before = version("1.3.9-RC.7")) shouldBe null
    }

    @Test
    fun `find no version`() {
        val sys = mockk<SystemCaller>().apply {
            every { call("git", forEachRef) } returns SystemCallResult(
                code = 0, stdout = emptyList()
            )
        }

        Git(sys).getLatestRelease() shouldBe null
        Git(sys).getLatestVersion() shouldBe null
    }

    @Test
    fun `tolerate non-versions`() {
        val sys = mockk<SystemCaller>().apply {
            every { call("git", forEachRef) } returns SystemCallResult(
                code = 0, stdout = listOf(
                    "2.1.0-SNAPSHOT.1",
                    "2.1.0-SNAPSHOT.2",
                    "accidental.version.string",
                    "2.0.0"
                )
            )
        }

        Git(sys).getLatestRelease() shouldBe version("2.0.0")
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

    @Test
    fun `get log until a version`() {
        val sys = mockk<SystemCaller>().apply {
            every {
                call(
                    "git", listOf(
                        "log",
                        "--format=format:%H %aI%n%B%n", "-z", "1.0.0"
                    )
                )
            } returns SystemCallResult(
                0, stdout = """
                        b8d181d9e803da9ceba0c3c4918317124d678656 2024-01-20T21:31:01+01:00
                        first commit
                    """.trimIndent().lines()
            )
        }
        Git(sys).getLog(to = version("1.0.0")) shouldHaveSize 1
    }

}