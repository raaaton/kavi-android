package com.raton.kavi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TestModelsTest {
    @Test
    fun answerNormalizationMatchesCaseDiacriticWidthAndWhitespace() {
        assertEquals("eleve test", TestText.normalize("  ÉLÈVE   Ｔｅｓｔ  "))
    }

    @Test
    fun invalidAuthoredQuestionIsRejected() {
        val configuration = DeckTestConfiguration(
            mode = DeckTestCreationMode.manual,
            multipleChoice = listOf(
                AuthoredMultipleChoiceQuestion(
                    id = "question",
                    sourceCardID = "card",
                    prompt = "Prompt",
                    choices = listOf("Same", " same "),
                    correctChoiceIndex = 0
                )
            )
        )
        assertThrows(IllegalArgumentException::class.java) {
            configuration.validated(setOf("card"))
        }
    }

    @Test
    fun duplicationRemapsBothCardAndQuestionIds() {
        val configuration = DeckTestConfiguration(
            mode = DeckTestCreationMode.manual,
            trueFalse = listOf(
                AuthoredTrueFalseQuestion("q1", "card-old", "Statement", true)
            )
        )
        var counter = 0
        val duplicate = configuration.duplicated(mapOf("card-old" to "card-new")) { "new-${++counter}" }
        assertEquals("card-new", duplicate.trueFalse.single().sourceCardID)
        assertNotEquals("q1", duplicate.trueFalse.single().id)
    }

    @Test
    fun incomingUseFlashcardsExplicitlyClearsAuthoredPools() {
        val local = DeckTestConfiguration(
            mode = DeckTestCreationMode.manual,
            trueFalse = listOf(AuthoredTrueFalseQuestion("q", "card", "Statement", true))
        )
        assertEquals(DeckTestConfiguration.useFlashcards, local.mergingQuestions(DeckTestConfiguration.useFlashcards))
    }
}
