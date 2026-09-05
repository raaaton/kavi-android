package com.raton.kavi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raton.kavi.data.AppLanguage
import com.raton.kavi.data.PreferencesRepository
import com.raton.kavi.data.SettingsState
import com.raton.kavi.domain.StudyDirection
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    repository: PreferencesRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹ Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item { SettingsHeader("General") }
            item { ToggleRow("Haptics", state.hapticsEnabled) { scope.launch { repository.setHaptics(it) } } }
            item { ToggleRow("Celebrations", state.celebrationsEnabled) { scope.launch { repository.setCelebrations(it) } } }
            item { ToggleRow("Folder-scoped search", state.searchScopeEnabled) { scope.launch { repository.setSearchScope(it) } } }
            item { SettingsHeader("Home") }
            item { ToggleRow("Resume section", state.homeResumeEnabled) { scope.launch { repository.setHomeResume(it) } } }
            item { ToggleRow("Recent section", state.homeRecentEnabled) { scope.launch { repository.setHomeRecent(it) } } }
            item { ToggleRow("Pinned section", state.homePinnedEnabled) { scope.launch { repository.setHomePinned(it) } } }
            item { SettingsHeader("Study") }
            item { ToggleRow("Study history", state.studyHistoryEnabled) { scope.launch { repository.setStudyHistory(it) } } }
            item { ToggleRow("Shuffle", state.studyShuffle) { scope.launch { repository.setStudyShuffle(it) } } }
            item { ToggleRow("Starred only", state.studyStarredOnly) { scope.launch { repository.setStudyStarredOnly(it) } } }
            item {
                ChoiceGroup(
                    title = "Direction",
                    values = StudyDirection.entries,
                    selected = state.studyDirection,
                    label = {
                        when (it) {
                            StudyDirection.termToDefinition -> "Term → definition"
                            StudyDirection.definitionToTerm -> "Definition → term"
                            StudyDirection.random -> "Random"
                        }
                    },
                    onSelected = { scope.launch { repository.setStudyDirection(it) } }
                )
            }
            item {
                ChoiceGroup(
                    title = "Language",
                    values = AppLanguage.entries,
                    selected = state.language,
                    label = { value -> value.name.replaceFirstChar { it.uppercase() } },
                    onSelected = { scope.launch { repository.setLanguage(it) } }
                )
            }
        }
    }
}

@Composable
private fun SettingsHeader(text: String) {
    Text(text, Modifier.padding(top = 18.dp, bottom = 8.dp), fontWeight = FontWeight.SemiBold)
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
    HorizontalDivider()
}

@Composable
private fun <T> ChoiceGroup(
    title: String,
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(title, fontWeight = FontWeight.Medium)
        values.forEach { value ->
            Row(
                Modifier.fillMaxWidth().clickable { onSelected(value) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = value == selected, onClick = { onSelected(value) })
                Text(label(value))
            }
        }
    }
    HorizontalDivider()
}
