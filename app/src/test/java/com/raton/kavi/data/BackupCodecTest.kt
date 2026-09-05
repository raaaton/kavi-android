package com.raton.kavi.data

import com.raton.kavi.domain.BackupCardDTO
import com.raton.kavi.domain.BackupDeckDTO
import com.raton.kavi.domain.BackupEnvelope
import com.raton.kavi.domain.BackupScope
import com.raton.kavi.domain.DeckTestConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCodecTest {
    @Test
    fun v2RoundTripPreservesStableIdsAndTestConfiguration() {
        val envelope = BackupEnvelope(
            exportedAt = "2026-09-05T12:00:00Z",
            scope = BackupScope.database,
            folders = emptyList(),
            decks = listOf(
                BackupDeckDTO(
                    id = "deck-id",
                    name = "Deck",
                    createdAt = "2026-09-05T12:00:00Z",
                    updatedAt = "2026-09-05T12:00:00Z",
                    cards = listOf(
                        BackupCardDTO(
                            id = "card-id",
                            term = "Term",
                            definition = "Definition",
                            position = 0,
                            mastered = false,
                            timesStudied = 0,
                            timesCorrect = 0
                        )
                    ),
                    testConfiguration = DeckTestConfiguration.useFlashcards
                )
            )
        )

        val decoded = BackupCodec.decode(BackupCodec.encode(envelope))
        assertEquals(2, decoded.schemaVersion)
        assertEquals("deck-id", decoded.decks.single().id)
        assertEquals("card-id", decoded.decks.single().cards.single().id)
    }

    @Test
    fun v1WithoutTestFieldsUsesCurrentDefaults() {
        val raw = """
            {
              "schemaVersion": 1,
              "exportedAt": "2026-09-05T12:00:00Z",
              "scope": "database",
              "folders": [],
              "decks": [{
                "id": "deck-id",
                "name": "Legacy",
                "createdAt": "2026-09-05T12:00:00Z",
                "updatedAt": "2026-09-05T12:00:00Z",
                "cards": [{
                  "id": "card-id",
                  "term": "A",
                  "definition": "B",
                  "position": 0,
                  "mastered": false,
                  "timesStudied": 0,
                  "timesCorrect": 0
                }]
              }]
            }
        """.trimIndent()

        val decoded = BackupCodec.decode(raw)
        assertEquals(2, decoded.schemaVersion)
        assertEquals(DeckTestConfiguration.useFlashcards, decoded.decks.single().testConfiguration)
        assertEquals(false, decoded.decks.single().cards.single().testMastered)
    }

    @Test
    fun unknownSchemaIsRejected() {
        val raw = """{"schemaVersion":99,"exportedAt":"x","scope":"database","folders":[],"decks":[]}"""
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(raw) }
    }
}
