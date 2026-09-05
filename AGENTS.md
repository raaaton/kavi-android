# AGENTS.md — Kavi Android

## Repository boundary

- Write only to `raaaton/kavi-android` for Android work.
- `raaaton/kavi` is the read-only iOS product/data reference.
- Do not open Android port PRs against the iOS repository.
- Development for the current port stays on `android-port` until explicitly merged by the user.

## Product invariants

- Native Android only: Kotlin + Jetpack Compose.
- Local-first and fully usable without a backend.
- Room for core local data; DataStore for preferences.
- Preserve stable UUID identity across import/export and future sync.
- Keep explicit `Folder.sortOrder` and `Card.position`; never rely on relationship/list insertion order.
- Preserve independent Flashcards and Test mastery/progress.
- Retain legacy-compatible fields such as folder `colorHex` and deck description unless a migration is deliberate.
- Avoid oversized architecture frameworks and unnecessary abstraction layers.
- No Supabase/account/cloud sync until a later explicit milestone.

## Data safety

- Folder→Deck and Deck→Card are cascade relationships.
- "Delete folder, keep decks" must clear deck folder references before deleting the folder.
- Destructive card operations must normalize remaining positions.
- Deleting/moving source cards must keep authored Test references valid.
- Backup restore is merge/upsert by UUID, never an implicit destructive database replacement.

## CI cadence

Use local/unit checks while developing where possible. Inspect GitHub Actions only at large milestones or when there is a concrete reason to suspect CI failure.
