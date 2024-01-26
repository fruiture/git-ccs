package de.fruiture.cor.ccs

import de.fruiture.cor.ccs.git.Git
import de.fruiture.cor.ccs.git.SystemCallResult
import de.fruiture.cor.ccs.git.SystemCaller
import de.fruiture.cor.ccs.semver.AlphaNumericIdentifier.Companion.alphanumeric
import de.fruiture.cor.ccs.semver.PreReleaseIndicator.Strategy.Companion.counter
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AppTest {

    private val oneFeatureAfterMajorRelease = Git(object : SystemCaller {
        override fun call(command: String, arguments: List<String>): SystemCallResult {
            return if (arguments.first() == "describe")
                SystemCallResult(code = 0, stdout = listOf("1.0.0"))
            else if (arguments.first() == "log" && arguments.last() == "1.0.0..HEAD") {
                SystemCallResult(
                    code = 0,
                    stdout = """
                            cafebabe 2001-01-01T13:00Z
                            feat: a feature is born
                        """.trimIndent().lines()
                )
            } else throw IllegalArgumentException()
        }
    })

    @Test
    fun `get next release version`() {
        App(oneFeatureAfterMajorRelease).getNextRelease() shouldBe "1.1.0"
    }

    @Test
    fun `get next pre-release version`() {
        App(oneFeatureAfterMajorRelease).getNextPreRelease(counter()) shouldBe "1.1.0-SNAPSHOT.1"
        App(oneFeatureAfterMajorRelease).getNextPreRelease(counter("alpha".alphanumeric)) shouldBe "1.1.0-alpha.1"
    }

    private val noReleaseYet = Git(object : SystemCaller {
        override fun call(command: String, arguments: List<String>): SystemCallResult {
            return if (arguments.first() == "describe")
                SystemCallResult(code = 128, stderr = listOf("fatal: No tags can describe ..."))
            else if (arguments.first() == "log" && arguments.last() == "HEAD") {
                SystemCallResult(
                    code = 0,
                    stdout = """
                            cafebabe 2001-01-01T13:00Z
                            feat: a feature is born
                        """.trimIndent().lines()
                )
            } else throw IllegalArgumentException()
        }
    })

    @Test
    fun `get initial release or snapshot`() {
        App(noReleaseYet).getNextRelease() shouldBe "0.0.1"
        App(noReleaseYet).getNextPreRelease(counter("RC".alphanumeric)) shouldBe "0.0.1-RC.1"
        App(noReleaseYet).getNextPreRelease(counter()) shouldBe "0.0.1-SNAPSHOT.1"
    }

    @Test
    fun `get change log`() {
        App(oneFeatureAfterMajorRelease).getChangeLog() shouldBe
                """[{"hash":"cafebabe","date":"2001-01-01T13:00Z","message":"feat: a feature is born",""" +
                """"conventionalCommit":{"type":"feat","description":"a feature is born"}}]"""

        App(noReleaseYet).getChangeLog() shouldBe
                """[{"hash":"cafebabe","date":"2001-01-01T13:00Z","message":"feat: a feature is born",""" +
                """"conventionalCommit":{"type":"feat","description":"a feature is born"}}]"""
    }


    private val hadABreakingChange = Git(object : SystemCaller {
        override fun call(command: String, arguments: List<String>): SystemCallResult {
            return if (arguments.first() == "describe")
                SystemCallResult(code = 0, stdout = listOf("1.2.3-SNAPSHOT.5"))
            else if (arguments.first() == "log" && arguments.last() == "1.2.3-SNAPSHOT.5..HEAD") {
                SystemCallResult(
                    code = 0,
                    stdout = """
                            cafebabe 2001-01-01T13:00Z
                            feat!: a feature with a breaking change
                        """.trimIndent().lines()
                )
            } else throw IllegalArgumentException()
        }
    })

    @Test
    fun `breaking change is recognized`() {
        App(hadABreakingChange).getNextRelease() shouldBe "2.0.0"
        App(hadABreakingChange).getNextPreRelease(counter()) shouldBe "2.0.0-SNAPSHOT.1"
    }
}