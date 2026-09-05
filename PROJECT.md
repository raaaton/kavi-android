# Kavi Android — Project Guide

This document is the technical map for `raaaton/kavi-android`.

The source-of-truth product reference is the native iOS repository `raaaton/kavi`. Android development must not modify that repository. The Android implementation is intentionally native and independent while preserving compatible product concepts and stable identifiers.

## 1. Platform

| Area | Android choice |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Persistence | Room |
| Preferences | Preferences DataStore |
| Navigation | Navigation Compose |
| Async/reactive | Coroutines + Flow |
| Serialization | kotlinx.serialization JSON |
| Build | Gradle Kotlin DSL |
| Minimum Android | API 24 |
| Compile/target SDK | 36 |
| Backend | None |
| Cloud sync | Not implemented in this phase |

The app is local-first and core library operations require no network permission or backend.

## 2. Product/visual invariants

The Android UI adapts Kavi rather than cloning Apple controls pixel-for-pixel.

- dark-only foundation for current parity
- mint brand accent `#46D7A7`
- mostly black/white/neutral surfaces
- Material 3/native Android interactions
- simple hierarchy and small architecture
- stable user-owned data

## 3. Data model

Room stores the same three fundamental entities as the iOS SwiftData model.

### FolderEntity

- `id` — UUID string
- `name`
- `createdAt` — ISO-8601 UTC string
- `iconName`
- `colorHex` — retained for compatibility
- `sortOrder` — explicit persisted order

`Folder → Deck` uses `ON DELETE CASCADE`. To reproduce iOS "keep decks", `LibraryRepository.deleteFolder(..., keepDecks = true)` first clears each deck's `folderId`, then deletes the folder.

### DeckEntity

- stable UUID string
- name + legacy-compatible optional description
- created/updated/opened timestamps
- independent Study/Test session counters and active-session JSON
- study-history JSON
- independent Study/Test activity dates
- pin state
- serialized Test configuration
- nullable `folderId`

`Deck → Card` uses `ON DELETE CASCADE`.

### CardEntity

- stable UUID string
- term / definition
- explicit `position`
- Flashcards mastery
- Test mastery
- studied/correct counters
- star state
- owning `deckId`

Visible card ordering never relies on Room/relationship insertion order. Move/delete operations rewrite contiguous positions.

## 4. UUID policy

Android generates RFC-4122 UUID strings with `UUID.randomUUID()` and never replaces imported IDs. Backup merge uses IDs as identity exactly like iOS. Duplicating a deck/folder intentionally generates new deck/card/question UUIDs and remaps authored-question source card references.

This keeps a future shared sync contract possible without replacing local identifiers.

## 5. Study/Test compatibility

`domain/StudyModels.kt` mirrors the iOS Codable structures needed for resumable Study sessions, including direction, session size, reversed card snapshots, progress and judgments.

`domain/TestModels.kt` mirrors Test question/configuration concepts:

- `useFlashcards`, `ai`, `manual`
- Multiple Choice authored questions
- True/False authored questions
- Multiple Choice / True-False / written runtime questions
- answers and independent Test state
- validation, merge, linked-question removal and duplication remapping

`SessionCodecs` keeps these payloads as local JSON strings in Room. The payload shape deliberately uses iOS-style field names (`deckID`, `cardID`, `sourceCardID`, etc.) rather than Android-specific names.

The full Study/Test run screens are a later milestone.

## 6. Backup compatibility

`BackupModels.kt` follows the current iOS backup envelope contract.

- current schema version: 2
- v1 is accepted and normalized to the current DTO defaults
- v2 is encoded on new exports
- scope: `deck` / `database`
- folder/deck/card UUIDs are preserved
- nullable Swift `Data` payloads are represented in the backup JSON as Base64 strings
- `deckDescription` is intentionally not exported because current iOS `BackupDeckDTO` does not contain it

`BackupService.importEnvelope` is an additive merge/upsert:

- matching UUID → update
- new UUID → insert
- local objects absent from incoming backup are not deleted
- missing incoming `lastOpenedAt` preserves the local value
- relationships are reconstructed by IDs
- authored questions merge by question UUID
- incoming `useFlashcards` explicitly clears custom question pools
- validation occurs after cards have been merged
- the Room transaction rolls back on failure

File-picker/share UI for export/restore is still pending.

## 7. Preferences

DataStore keys intentionally mirror the iOS UserDefaults keys where useful:

- haptics
- celebrations
- folder-scoped search
- Resume / Recent / Pinned Home sections
- study history
- study direction
- shuffle
- starred only
- app language

The Android settings screen already persists these values. Some toggles (for example Resume) will become fully user-visible when the related Study/Test UI is ported.

## 8. Implemented library behavior

- Home
- Recent section
- Pinned section
- folders
- Unfiled
- folder two-column presentation
- folder creation/rename/delete
- keep-decks folder deletion
- folder cascade deletion
- persistent folder ordering
- folder duplication
- deck creation/rename/delete/duplicate
- deck pin state
- card creation/edit/delete
- card star state
- persistent card order
- separate Flashcards/Test progress fields
- neutral dark Kavi styling

## 9. Still to port

Major remaining parity work:

1. full Study setup/run/undo/review/completion/resume UI and progress mutations
2. full Test setup/question factory/run/override/retry/resume UI
3. authored Test editor UI
4. study history UI + recording actions
5. search
6. Bulk Add/import parser + UI
7. backup file picker/share/preview UI
8. external AI handoff flow
9. localization resources (FR/EN/DE/ES + automatic selection)
10. duplicate detection/warning UI
11. richer Android-native reorder gestures/haptics
12. final design polish and accessibility pass
13. iOS ↔ Android cloud synchronization/account layer (explicitly deferred)

## 10. Sync readiness

No cloud schema is introduced yet. The current choices are intentionally sync-friendly:

- stable cross-platform UUID identity
- explicit relationships by UUID
- explicit user-visible ordering
- independent Study/Test fields
- serialized cross-platform domain structures
- local-first source of truth
- no Android-generated integer IDs exposed as domain identity

A later sync milestone can add server metadata/tombstones/conflict handling without replacing the core Kavi entities.

## 11. Testing and CI

JVM/Robolectric tests cover:

- Folder cascade deletion
- deleting a Folder while preserving Deck/Card data in Unfiled
- card order normalization after moves/deletes
- duplication UUID invariants
- authored Test validation/remapping/merge behavior
- Study Resume validation against remaining card IDs

`.github/workflows/android-ci.yml` installs the Android SDK + Gradle 8.13, runs `:app:testDebugUnitTest`, then builds `:app:assembleDebug` and uploads the debug APK artifact.

CI should be inspected at major milestones, not after every micro-edit.
