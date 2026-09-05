package com.raton.kavi.domain

import java.text.Normalizer
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
enum class DeckTestCreationMode { useFlashcards, ai, manual }

@Serializable
data class AuthoredMultipleChoiceQuestion(
    val id: String,
    val sourceCardID: String,
    val prompt: String,
    val choices: List<String>,
    val correctChoiceIndex: Int
)

@Serializable
data class AuthoredTrueFalseQuestion(
    val id: String,
    val sourceCardID: String,
    val statement: String,
    val correctAnswer: Boolean
)

@Serializable
data class DeckTestConfiguration(
    val mode: DeckTestCreationMode = DeckTestCreationMode.useFlashcards,
    val multipleChoice: List<AuthoredMultipleChoiceQuestion> = emptyList(),
    val trueFalse: List<AuthoredTrueFalseQuestion> = emptyList()
) {
    fun removingQuestions(linkedTo: Set<String>): DeckTestConfiguration = copy(
        multipleChoice = multipleChoice.filterNot { it.sourceCardID in linkedTo },
        trueFalse = trueFalse.filterNot { it.sourceCardID in linkedTo }
    )

    fun duplicated(cardIDMap: Map<String, String>, newID: () -> String): DeckTestConfiguration {
        if (mode == DeckTestCreationMode.useFlashcards) return useFlashcards
        return copy(
            multipleChoice = multipleChoice.mapNotNull { question ->
                val source = cardIDMap[question.sourceCardID] ?: return@mapNotNull null
                question.copy(id = newID(), sourceCardID = source)
            },
            trueFalse = trueFalse.mapNotNull { question ->
                val source = cardIDMap[question.sourceCardID] ?: return@mapNotNull null
                question.copy(id = newID(), sourceCardID = source)
            }
        )
    }

    companion object {
        val useFlashcards = DeckTestConfiguration()
    }
}

@Serializable
enum class TestQuestionType { multipleChoice, trueFalse, written }

@Serializable
data class TestQuestion(
    val id: String,
    val cardID: String,
    val type: TestQuestionType,
    val prompt: String,
    val secondaryText: String? = null,
    val correctAnswer: String,
    val referenceAnswer: String? = null,
    val choices: List<String> = emptyList()
)

@Serializable
data class TestAnswerRecord(
    val question: TestQuestion,
    val givenAnswer: String,
    val isCorrect: Boolean,
    val wasOverridden: Boolean
) {
    val id: String get() = question.id
}

@Serializable
data class TestSessionState(
    val questions: List<TestQuestion>,
    val currentIndex: Int = 0,
    val answers: List<TestAnswerRecord> = emptyList(),
    val currentAnswer: TestAnswerRecord? = null
) {
    val isComplete: Boolean get() = currentIndex >= questions.size
    val correctCount: Int get() = answers.count { it.isCorrect }
    val score: Int get() = if (answers.isEmpty()) 0 else ((correctCount.toDouble() / answers.size) * 100).toInt()
}

@Serializable
data class ActiveTestSessionSnapshot(
    val deckID: String,
    val sessionNumber: Int,
    val selectedTypes: Set<TestQuestionType>,
    val direction: StudyDirection,
    val shuffle: Boolean,
    val starredOnly: Boolean,
    val sessionSize: SessionSize,
    val state: TestSessionState
)

object TestText {
    fun clean(value: String): String = value.trim()

    fun normalize(value: String): String {
        val decomposed = Normalizer.normalize(clean(value), Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
        return decomposed
            .lowercase(Locale.FRANCE)
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .joinToString(" ")
    }
}
