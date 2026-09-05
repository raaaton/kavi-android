package com.raton.kavi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.raton.kavi.data.CardEntity
import com.raton.kavi.data.DeckEntity
import com.raton.kavi.data.FolderEntity
import com.raton.kavi.data.LibraryRepository
import com.raton.kavi.data.LibrarySnapshot
import com.raton.kavi.data.SettingsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    snapshot: LibrarySnapshot,
    settings: SettingsState,
    repository: LibraryRepository,
    onOpenFolder: (String?) -> Unit,
    onOpenDeck: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var creationChoice by remember { mutableStateOf(false) }
    var createFolder by remember { mutableStateOf(false) }
    var createDeck by remember { mutableStateOf(false) }
    var editingFolder by remember { mutableStateOf<FolderEntity?>(null) }
    var deletingFolder by remember { mutableStateOf<FolderEntity?>(null) }
    val pinned = snapshot.decks.filter { it.isPinned }
        .sortedByDescending { it.lastOpenedAt ?: it.updatedAt }
    val recent = snapshot.decks.filter { it.lastOpenedAt != null }
        .sortedByDescending { it.lastOpenedAt }.take(2)
    val unfiled = snapshot.decksIn(null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kavi", fontWeight = FontWeight.SemiBold) },
                actions = { TextButton(onClick = onOpenSettings) { Text("Settings") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creationChoice = true }) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (settings.homeRecentEnabled && recent.isNotEmpty()) {
                item { SectionTitle("Recent") }
                items(recent, key = { "recent-${it.id}" }) { deck ->
                    DeckRow(deck, snapshot.cardCount(deck.id), onClick = { onOpenDeck(deck.id) })
                }
            }
            if (settings.homePinnedEnabled && pinned.isNotEmpty()) {
                item { SectionTitle("Pinned") }
                items(pinned, key = { "pinned-${it.id}" }) { deck ->
                    DeckRow(deck, snapshot.cardCount(deck.id), onClick = { onOpenDeck(deck.id) })
                }
            }
            item { SectionTitle("Folders") }
            if (snapshot.folders.isEmpty() && unfiled.isEmpty()) {
                item { EmptySurface("Create your first folder or deck. Everything stays on this device.") }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    snapshot.folders.chunked(2).forEach { rowFolders ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowFolders.forEach { folder ->
                                FolderCard(
                                    folder = folder,
                                    deckCount = snapshot.folderCount(folder.id),
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpenFolder(folder.id) },
                                    onMoveUp = { scope.launch { repository.moveFolder(folder.id, -1) } },
                                    onMoveDown = { scope.launch { repository.moveFolder(folder.id, 1) } },
                                    onEdit = { editingFolder = folder },
                                    onDuplicate = { scope.launch { repository.duplicateFolder(folder.id) } },
                                    onDelete = { deletingFolder = folder }
                                )
                            }
                            if (rowFolders.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            if (unfiled.isNotEmpty()) {
                item {
                    FolderCard(
                        folder = null,
                        deckCount = unfiled.size,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenFolder(null) }
                    )
                }
            }
        }
    }

    if (creationChoice) {
        AlertDialog(
            onDismissRequest = { creationChoice = false },
            title = { Text("Create") },
            text = { Text("Choose what to add to your local library.") },
            confirmButton = {
                Row {
                    TextButton(onClick = { creationChoice = false; createFolder = true }) { Text("Folder") }
                    TextButton(onClick = { creationChoice = false; createDeck = true }) { Text("Deck") }
                }
            },
            dismissButton = { TextButton(onClick = { creationChoice = false }) { Text("Cancel") } }
        )
    }
    if (createFolder) {
        NameDialog("New folder", "Folder name", onDismiss = { createFolder = false }) { name ->
            scope.launch { repository.createFolder(name) }
            createFolder = false
        }
    }
    if (createDeck) {
        NameDialog("New deck", "Deck name", onDismiss = { createDeck = false }) { name ->
            scope.launch { repository.createDeck(name) }
            createDeck = false
        }
    }
    editingFolder?.let { folder ->
        NameDialog("Rename folder", "Folder name", initial = folder.name, onDismiss = { editingFolder = null }) { name ->
            scope.launch { repository.renameFolder(folder.id, name) }
            editingFolder = null
        }
    }
    deletingFolder?.let { folder ->
        AlertDialog(
            onDismissRequest = { deletingFolder = null },
            title = { Text("Delete ${folder.name}?") },
            text = { Text("You can keep its decks by moving them to Unfiled, or delete the complete folder contents.") },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        scope.launch { repository.deleteFolder(folder.id, false) }
                        deletingFolder = null
                    }) { Text("Delete all") }
                    TextButton(onClick = {
                        scope.launch { repository.deleteFolder(folder.id, true) }
                        deletingFolder = null
                    }) { Text("Keep decks") }
                }
            },
            dismissButton = { TextButton(onClick = { deletingFolder = null }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    folderId: String?,
    snapshot: LibrarySnapshot,
    repository: LibraryRepository,
    onBack: () -> Unit,
    onOpenDeck: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val folder = folderId?.let { id -> snapshot.folders.firstOrNull { it.id == id } }
    val decks = snapshot.decksIn(folderId)
    var createDeck by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folder?.name ?: "Unfiled") },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹ Back") } },
                actions = { TextButton(onClick = { createDeck = true }) { Text("+ Deck") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (decks.isEmpty()) item { EmptySurface("No decks here yet.") }
            items(decks, key = { it.id }) { deck ->
                DeckRow(
                    deck = deck,
                    cardCount = snapshot.cardCount(deck.id),
                    onClick = { onOpenDeck(deck.id) },
                    trailing = {
                        TextButton(onClick = {
                            scope.launch { repository.setPinned(deck.id, !deck.isPinned) }
                        }) { Text(if (deck.isPinned) "★" else "☆") }
                    }
                )
            }
        }
    }

    if (createDeck) {
        NameDialog("New deck", "Deck name", onDismiss = { createDeck = false }) { name ->
            scope.launch { repository.createDeck(name, folderId) }
            createDeck = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    deckId: String,
    snapshot: LibrarySnapshot,
    repository: LibraryRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val deck = snapshot.decks.firstOrNull { it.id == deckId }
    val cards = snapshot.cardsIn(deckId)
    var addCard by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<CardEntity?>(null) }
    var rename by remember { mutableStateOf(false) }
    var deleteDeck by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }

    LaunchedEffect(deckId) { repository.markOpened(deckId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(deck?.name ?: "Deck", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹ Back") } },
                actions = {
                    if (deck != null) {
                        TextButton(onClick = {
                            scope.launch { repository.setPinned(deck.id, !deck.isPinned) }
                        }) { Text(if (deck.isPinned) "★" else "☆") }
                        Box {
                            TextButton(onClick = { menu = true }) { Text("⋮") }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; rename = true })
                                DropdownMenuItem(text = { Text("Duplicate") }, onClick = {
                                    menu = false
                                    scope.launch { repository.duplicateDeck(deck.id) }
                                })
                                DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; deleteDeck = true })
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (deck != null) FloatingActionButton(onClick = { addCard = true }) { Text("+") }
        }
    ) { padding ->
        if (deck == null) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp)) { Text("Deck not found") }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { ProgressSurface(cards) }
                item { SectionTitle("Cards · ${cards.size}") }
                if (cards.isEmpty()) item { EmptySurface("Add a first card to this deck.") }
                items(cards, key = { it.id }) { card ->
                    CardRow(
                        card = card,
                        onEdit = { editingCard = card },
                        onStar = { scope.launch { repository.setStarred(card.id, !card.isStarred) } },
                        onMoveUp = { scope.launch { repository.moveCard(card.id, -1) } },
                        onMoveDown = { scope.launch { repository.moveCard(card.id, 1) } },
                        onDelete = { scope.launch { repository.deleteCard(card.id) } }
                    )
                }
            }
        }
    }

    if (addCard) {
        CardEditorDialog(onDismiss = { addCard = false }) { term, definition ->
            scope.launch { repository.createCard(deckId, term, definition) }
            addCard = false
        }
    }
    editingCard?.let { card ->
        CardEditorDialog(card, onDismiss = { editingCard = null }) { term, definition ->
            scope.launch { repository.updateCard(card.id, term, definition) }
            editingCard = null
        }
    }
    if (rename && deck != null) {
        NameDialog("Rename deck", "Deck name", deck.name, onDismiss = { rename = false }) { name ->
            scope.launch { repository.renameDeck(deck.id, name) }
            rename = false
        }
    }
    if (deleteDeck && deck != null) {
        AlertDialog(
            onDismissRequest = { deleteDeck = false },
            title = { Text("Delete ${deck.name}?") },
            text = { Text("Its cards and local progress will be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.deleteDeck(deck.id) }
                    deleteDeck = false
                    onBack()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteDeck = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ProgressSurface(cards: List<CardEntity>) {
    val flashcards = if (cards.isEmpty()) 0f else cards.count { it.mastered }.toFloat() / cards.size
    val tests = if (cards.isEmpty()) 0f else cards.count { it.testMastered }.toFloat() / cards.size
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Flashcards progress", style = MaterialTheme.typography.labelLarge)
            LinearProgressIndicator(progress = flashcards, modifier = Modifier.fillMaxWidth())
            Text("Test progress", style = MaterialTheme.typography.labelLarge)
            LinearProgressIndicator(progress = tests, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun FolderCard(
    folder: FolderEntity?,
    deckCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (folder == null) "Unfiled" else "▰  ${folder.name}",
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("$deckCount deck${if (deckCount == 1) "" else "s"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (folder != null) {
                Row {
                    onMoveUp?.let { TextButton(onClick = it, contentPadding = PaddingValues(6.dp)) { Text("↑") } }
                    onMoveDown?.let { TextButton(onClick = it, contentPadding = PaddingValues(6.dp)) { Text("↓") } }
                    onEdit?.let { TextButton(onClick = it, contentPadding = PaddingValues(6.dp)) { Text("Edit") } }
                    onDuplicate?.let { TextButton(onClick = it, contentPadding = PaddingValues(6.dp)) { Text("Copy") } }
                    onDelete?.let { TextButton(onClick = it, contentPadding = PaddingValues(6.dp)) { Text("Delete") } }
                }
            }
        }
    }
}

@Composable
private fun DeckRow(
    deck: DeckEntity,
    cardCount: Int,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(deck.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$cardCount cards", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun CardRow(
    card: CardEntity,
    onEdit: () -> Unit,
    onStar: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(14.dp)) {
            Text(card.term, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(card.definition, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(Modifier.padding(top = 10.dp))
            Row {
                TextButton(onClick = onStar, contentPadding = PaddingValues(6.dp)) { Text(if (card.isStarred) "★" else "☆") }
                TextButton(onClick = onMoveUp, contentPadding = PaddingValues(6.dp)) { Text("↑") }
                TextButton(onClick = onMoveDown, contentPadding = PaddingValues(6.dp)) { Text("↓") }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onDelete, contentPadding = PaddingValues(6.dp)) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun EmptySurface(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Text(text, Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NameDialog(
    title: String,
    label: String,
    initial: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(enabled = value.trim().isNotEmpty(), onClick = { onConfirm(value.trim()) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CardEditorDialog(
    card: CardEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var term by remember(card?.id) { mutableStateOf(card?.term.orEmpty()) }
    var definition by remember(card?.id) { mutableStateOf(card?.definition.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (card == null) "New card" else "Edit card") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = term, onValueChange = { term = it }, label = { Text("Term") })
                OutlinedTextField(value = definition, onValueChange = { definition = it }, label = { Text("Definition") })
            }
        },
        confirmButton = {
            TextButton(
                enabled = term.trim().isNotEmpty() && definition.trim().isNotEmpty(),
                onClick = { onConfirm(term.trim(), definition.trim()) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}