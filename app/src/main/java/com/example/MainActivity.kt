package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.NoteRepository
import com.example.ui.navigation.Screen
import com.example.ui.screens.AddEditNoteScreen
import com.example.ui.screens.NotesListScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodels.NotesViewModel
import com.example.ui.viewmodels.NotesViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Database
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "omninote_database"
        ).fallbackToDestructiveMigration().build()
        
        val repository = NoteRepository(database.noteDao())
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val viewModel: NotesViewModel = viewModel(
                        factory = NotesViewModelFactory(repository)
                    )

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route
                    ) {
                        composable(Screen.Home.route) {
                            NotesListScreen(
                                viewModel = viewModel,
                                onNavigateToAddNote = {
                                    navController.navigate(Screen.AddEditNote.createRoute(null))
                                },
                                onNavigateToEditNote = { id ->
                                    navController.navigate(Screen.AddEditNote.createRoute(id))
                                }
                            )
                        }
                        
                        composable(
                            route = Screen.AddEditNote.routeWithArgs,
                            arguments = listOf(
                                navArgument(Screen.AddEditNote.noteIdArg) {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { entry ->
                            val noteIdString = entry.arguments?.getString(Screen.AddEditNote.noteIdArg)
                            val noteId = noteIdString?.toIntOrNull()
                            AddEditNoteScreen(
                                noteId = noteId,
                                viewModel = viewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
