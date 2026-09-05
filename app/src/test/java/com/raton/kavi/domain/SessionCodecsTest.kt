package com.raton.kavi.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionCodecsTest {
    @Test
    fun resumeRejectsMissingRemainingCards() {
        val state = StudySessionState(
            direction = StudyDirection.termToDefinition,
            shuffle = false,
            initialCardCount = 2,
            items = listOf(
                StudySessionItem(StudyCardSnapshot("a", "A", "1"), false),
                StudySessionItem(StudyCardSnapshot("b", "B", "2"), false)
            ),
            currentIndex = 1,
            cardsSeen = 1
        )
        val raw = SessionCodecs.encodeStudy(ActiveStudySessionSnapshot("deck", 1, state))

        assertTrue(SessionCodecs.canResumeStudy(raw, "deck", setOf("a", "b")))
        assertFalse(SessionCodecs.canResumeStudy(raw, "deck", setOf("a")))
        assertFalse(SessionCodecs.canResumeStudy(raw, "other", setOf("a", "b")))
    }
}
