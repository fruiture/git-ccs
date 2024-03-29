package de.fruiture.cor.ccs.git

import de.fruiture.cor.ccs.git.VersionTag.Companion.versionTag
import de.fruiture.cor.ccs.semver.Version.Companion.version
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test

class GitTest {

    private val forEachRef = listOf(
        "for-each-ref",
        "--merged", "HEAD",
        "--sort=-committerdate",
        "--format=%(refname:short)",
        "refs/tags/*.*.*"
    )

    @Test
    fun `get latest version or release tag using git for-each-ref`() {
        val sys = mockk<SystemCaller>().apply {
            every { call("git", forEachRef) } returns SystemCallResult(
                code = 0, stdout = listOf(
                    "v2.1.0-SNAPSHOT.1",
                    "2.1.0-SNAPSHOT.2",
                    "rel2.0.0",
                    "1.3.9",
                    "1.3.9-RC.7"
                )
            )
        }

        val git = Git(sys)

        git.getLatestVersionTag() shouldBe versionTag("2.1.0-SNAPSHOT.2")
        git.getLatestVersionTag(before(version("2.1.0-SNAPSHOT.2"))) shouldBe versionTag("v2.1.0-SNAPSHOT.1")
        git.getLatestVersionTag(before(version("2.1.0-SNAPSHOT.1"))) shouldBe versionTag("rel2.0.0")

        git.getLatestReleaseTag() shouldBe versionTag("rel2.0.0")
        git.getLatestReleaseTag(before(version("2.1.0-SNAPSHOT.2"))) shouldBe versionTag("rel2.0.0")
        git.getLatestReleaseTag(before(version("2.0.0"))) shouldBe versionTag("1.3.9")
        git.getLatestReleaseTag(before(version("1.3.9"))) shouldBe null

        git.getLatestVersionTag(before(version("1.3.9"))) shouldBe versionTag("1.3.9-RC.7")
        git.getLatestVersionTag(before(version("1.3.9-RC.7"))) shouldBe null
    }

    @Test
    fun `find no version`() {
        val sys = mockk<SystemCaller>().apply {
            every { call("git", forEachRef) } returns SystemCallResult(
                code = 0, stdout = emptyList()
            )
        }

        Git(sys).getLatestReleaseTag() shouldBe null
        Git(sys).getLatestVersionTag() shouldBe null
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

        Git(sys).getLatestReleaseTag() shouldBe versionTag("2.0.0")
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

        shouldThrow<RuntimeException> { Git(sys).getLatestVersionTag() }
    }

    @Test
    fun `get machine readable log`() {
        val sys = object : SystemCaller {
            override fun call(command: String, arguments: List<String>): SystemCallResult {
                command shouldBe "git"
                arguments shouldBe listOf("log", "--format=format:%H %aI%n%B%n%x1E", "1.0.0..HEAD")

                return SystemCallResult(
                    code = 0,
                    stdout = """
                        948f00f8b349c6f9652809f924254ffe7a497227 2024-01-20T21:51:33+01:00
                        feat: blablabla

                        BREAKING CHANGE: did something dudu here

                        ${RECORD_SEPARATOR}
                        b8d181d9e803da9ceba0c3c4918317124d678656 2024-01-20T21:31:01+01:00
                        non conventional commit
                    """.trimIndent().lines()
                )
            }
        }

        Git(sys).getLogX(
            from = TagName("1.0.0")
        ) shouldBe listOf(
            GitCommit(
                hash = "948f00f8b349c6f9652809f924254ffe7a497227",
                date = ZonedDateTime("2024-01-20T21:51:33+01:00"),
                message = """
                    feat: blablabla

                    BREAKING CHANGE: did something dudu here
                """.trimIndent()
            ),
            GitCommit(
                hash = "b8d181d9e803da9ceba0c3c4918317124d678656",
                date = ZonedDateTime("2024-01-20T21:31:01+01:00"),
                message = "non conventional commit"
            )
        )
    }

    @Test
    fun `get full log`() {
        val sys = object : SystemCaller {
            override fun call(command: String, arguments: List<String>): SystemCallResult {
                command shouldBe "git"
                arguments shouldBe listOf("log", "--format=format:%H %aI%n%B%n%x1E", "HEAD")

                return SystemCallResult(
                    code = 0,
                    stdout = """
                        b8d181d9e803da9ceba0c3c4918317124d678656 2024-01-20T21:31:01+01:00
                        non conventional commit
                    """.trimIndent().lines()
                )
            }
        }

        Git(sys).getLogX() shouldBe listOf(
            GitCommit(
                hash = "b8d181d9e803da9ceba0c3c4918317124d678656",
                date = ZonedDateTime("2024-01-20T21:31:01+01:00"),
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
                        "--format=format:%H %aI%n%B%n%x1E", "v1.0.0"
                    )
                )
            } returns SystemCallResult(
                0, stdout = """
                        b8d181d9e803da9ceba0c3c4918317124d678656 2024-01-20T21:31:01+01:00
                        first commit
                    """.trimIndent().lines()
            )
        }
        Git(sys).getLogX(to = TagName("v1.0.0")) shouldHaveSize 1
    }

}