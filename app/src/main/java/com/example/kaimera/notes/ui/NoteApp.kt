package com.example.kaimera.notes.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun NoteApp(viewModel: NoteViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "note_list") {
        composable("note_list") {
            NoteListScreen(
                viewModel = viewModel,
                onNoteClick = { noteId ->
                    navController.navigate("note_edit/$noteId")
                },
                onAddNoteClick = {
                    navController.navigate("note_edit/-1")
                }
            )
        }
        composable(
            route = "note_edit/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.IntType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getInt("noteId") ?: -1
            NoteEditScreen(
                viewModel = viewModel,
                noteId = noteId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
