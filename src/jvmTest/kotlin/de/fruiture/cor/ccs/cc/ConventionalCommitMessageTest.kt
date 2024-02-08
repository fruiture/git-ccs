package de.fruiture.cor.ccs.cc

import de.fruiture.cor.ccs.cc.ConventionalCommitMessage.Companion.message
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ConventionalCommitMessageTest {

    @Test
    fun `parse a conventional commit`() {
        val msg = message("docs: correct spelling of CHANGELOG")

        msg.type shouldBe Type("docs")
        msg.description shouldBe Description("correct spelling of CHANGELOG")
        msg.scope shouldBe null
    }

    @Test
    fun `parse with scope`() {
        val msg = message("fix(parser): support scopes")

        msg.type shouldBe Type("fix")
        msg.scope shouldBe Scope("parser")
    }

    @Test
    fun `parse with body`() {
        val msg = message(
            """
            fix: prevent racing of requests

            Introduce a request id and a reference to latest request. Dismiss
            incoming responses other than from latest request.
        """.trimIndent()
        )

        msg.type shouldBe Type("fix")
        msg.description shouldBe Description("prevent racing of requests")
        msg.body shouldBe Body("Introduce a request id and a reference to latest request. Dismiss\nincoming responses other than from latest request.")
    }

    @Test
    fun `multi-paragraph body`() {
        message(
            """
            fix: prevent racing of requests

            Introduce a request id and a reference to latest request. Dismiss
            incoming responses other than from latest request.

            Remove timeouts which were used to mitigate the racing issue but are
            obsolete now.
        """.trimIndent()
        ).body shouldBe Body(
            """
            Introduce a request id and a reference to latest request. Dismiss
            incoming responses other than from latest request.

            Remove timeouts which were used to mitigate the racing issue but are
            obsolete now.
        """.trimIndent()
        )
    }

    @Test
    fun `breaking change in headline`() {
        message("fix!: bugfix breaks API").breakingChange shouldBe "bugfix breaks API"
    }

    @Test
    fun `breaking change in the footer (and useless newlines)`() {
        message(
            """
            feat: cool stuff
            
            BREAKING CHANGE: but the api changes
            
            BREAKING CHANGE: but the api changes
            
            
        """.trimIndent()
        ).breakingChange shouldBe "but the api changes"
    }

    @Test
    fun `breaking change in the footer after body`() {
        val msg = message(
            """
            feat: cool stuff
            
            Paragraph1
            
            Paragraph2
            
            BREAKING CHANGE: but the api changes
        """.trimIndent()
        )
        msg.breakingChange shouldBe "but the api changes"
        msg.body shouldBe Body(
            """
            Paragraph1
            
            Paragraph2
        """.trimIndent()
        )
    }

    @Test
    fun footers() {
        val msg = message(
            """
            fun: Fun
            
            Done-By: Me
            BREAKING CHANGE: Me too
        """.trimIndent()
        )

        msg.breakingChange shouldBe "Me too"
        msg.footers shouldBe listOf(
            GenericFooter("Done-By", "Me"),
            BreakingChange("Me too")
        )
    }

    @Test
    fun `convert to string`() {
        ConventionalCommitMessage(
            type = Type("feat"),
            description = Description("some text"),
            scope = Scope("test")
        ).toString() shouldBe "feat(test): some text"
    }

    @Test
    fun `all aspects in and out`() {
        message(
            """
            fun(fun)!: Fun
            
            Fun Fun Fun!
            
            More Fun
            
            Done-By: Me
            BREAKING-CHANGE: Me too
        """.trimIndent()
        ).toString() shouldBe """
            fun(fun)!: Fun
            
            Fun Fun Fun!
            
            More Fun
            
            Done-By: Me
            BREAKING-CHANGE: Me too
        """.trimIndent()
    }
}