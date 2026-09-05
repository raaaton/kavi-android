package com.raton.kavi.domain

import kotlin.math.roundToInt
import kotlinx.serialization.Serializable

@Serializable
enum class StudyDirection {
    termToDefinition,
    definitionToTerm,
    random
}

@Serializable
enum class StudyOutcome { knew, review }

@Serializable
enum class SessionSize { ten, twenty, all }

@Serializable
data class StudyCardProgressSnapshot(
    val mastered: Boolean,
    val timesStudied: Int,
    val timesCorrect: Int
)

@Serializable
data class StudyJudgment(
    val cardID: String,
    val outcome: StudyOutcome,
    val previousProgress: StudyCardProgressSnapshot
)

@Serializable
data class StudyCardSnapshot(
    val id: String,
    val term: String,
    val definition: String
)

@Serializable
data class StudySessionItem(
    val card: StudyCardSnapshot,
    val isReversed: Boolean
) {
    val id: String get() = card.id
    val front: String get() = if (isReversed) card.definition else card.term
    val back: String get() = if (isReversed) card.term else card.definition
}

@Serializable
data class StudySessionState(
    val direction: StudyDirection,
    val shuffle: Boolean,
    val sessionSize: SessionSize? = SessionSize.all,
    val starredOnly: Boolean? = false,
    val initialCardCount: Int,
    val items: List<StudySessionItem>,
    val currentIndex: Int = 0,
    val cardsSeen: Int = 0,
    val correctAnswers: Int = 0,
    val reviewAnswers: Int = 0,
    val isComplete: Boolean = false,
    val judgments: List<StudyJudgment>? = emptyList()
) {
    val successRate: Int
        get() = if (cardsSeen == 0) 0 else ((correctAnswers.toDouble() / cardsSeen) * 100).roundToInt()
}

@Serializable
data class ActiveStudySessionSnapshot(
    val deckID: String,
    val sessionNumber: Int,
    val state: StudySessionState
)

@Serializable
enum class StudyHistoryMode { flashcards, test }

@Serializable
data class StudyHistoryEntry(
    val id: String,
    val completedAt: Double,
    val mode: StudyHistoryMode,
    val itemCount: Int,
    val correctCount: Int,
    val incorrectCount: Int
) {
    val successRate: Int
        get() = if (itemCount == 0) 0 else ((correctCount.toDouble() / itemCount) * 100).roundToInt()
}
