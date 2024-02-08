package de.fruiture.cor.ccs.git

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test


class KommandSystemCallerTest {

    private val caller = KommandSystemCaller()

    @Test
    fun `invoke git -v`() {

        val result = caller.call("git", listOf("-v"))

        result.code shouldBe 0
        result.stderr shouldBe emptyList()
        result.stdout.first() shouldContain Regex("^git version")
    }

    @Test
    fun `invoke git -invalid-option`() {
        val result = caller.call("git", listOf("-invalid-option"))

        result.code shouldBe 129
        result.stderr.first() shouldBe "unknown option: -invalid-option"
    }
}