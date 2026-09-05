package com.raton.kavi.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.raton.kavi.KaviApplication
import com.raton.kavi.data.LibrarySnapshot
import com.raton.kavi.data.SettingsState

@Composable
fun KaviApp() {
    val application = LocalContext.current.applicationContext as KaviApplication
    val repository = application.libraryRepository
    val preferences = application.preferencesRepository
    val snapshot by repository.library.collectAsStateWithLifecycle(
        initialValue = LibrarySnapshot(emptyList(), emptyList(), emptyList())
    )
    val settings by preferences.settings.collectAsStateWithLifecycle(initialValue = SettingsState())
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                snapshot = snapshot,
                settings = settings,
                repository = repository,
                onOpenFolder = { id -> navController.navigate("folder/${id ?: "unfiled"}") },
                onOpenDeck = { id -> navController.navigate("deck/$id") },
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable(
            route = "folder/{folderId}",
            arguments = listOf(navArgument("folderId") { type = NavType.StringType })
        ) { entry ->
            val raw = entry.arguments?.getString("folderId").orEmpty()
            FolderScreen(
                folderId = raw.takeUnless { it == "unfiled" },
                snapshot = snapshot,
                repository = repository,
                onBack = navController::popBackStack,
                onOpenDeck = { navController.navigate("deck/$it") }
            )
        }
        composable(
            route = "deck/{deckId}",
            arguments = listOf(navArgument("deckId") { type = NavType.StringType })
        ) { entry ->
            DeckDetailScreen(
                deckId = entry.arguments?.getString("deckId").orEmpty(),
                snapshot = snapshot,
                repository = repository,
                onBack = navController::popBackStack
            )
        }
        composable("settings") {
            SettingsScreen(
                state = settings,
                repository = preferences,
                onBack = navController::popBackStack
            )
        }
    }
}
