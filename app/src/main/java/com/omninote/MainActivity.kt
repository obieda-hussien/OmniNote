package com.omninote

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
import com.omninote.data.AppDatabase
import com.omninote.data.NoteRepository
import com.omninote.ui.navigation.Screen
import com.omninote.ui.screens.AddEditNoteScreen
import com.omninote.ui.screens.NotesListScreen
import com.omninote.ui.theme.MyApplicationTheme
import com.omninote.ui.viewmodels.NotesViewModel
import com.omninote.ui.viewmodels.NotesViewModelFactory

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
        
        var initialSharedText: String? = null
        var initialSharedUris: List<android.net.Uri>? = null

        if (intent?.action == android.content.Intent.ACTION_SEND) {
            if ("text/plain" == intent.type) {
                initialSharedText = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
            } else {
                val uri = intent.getParcelableExtra<android.net.Uri>(android.content.Intent.EXTRA_STREAM)
                if (uri != null) initialSharedUris = listOf(uri)
            }
            intent.action = android.content.Intent.ACTION_MAIN
        } else if (intent?.action == android.content.Intent.ACTION_SEND_MULTIPLE) {
            val uris = intent.getParcelableArrayListExtra<android.net.Uri>(android.content.Intent.EXTRA_STREAM)
            if (uris != null) initialSharedUris = uris
            intent.action = android.content.Intent.ACTION_MAIN
        }
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val viewModel: NotesViewModel = viewModel(
                        factory = NotesViewModelFactory(repository)
                    )
                    
                    // Pass it if it's the first time
                    val isFirstRun = remember { androidx.compose.runtime.mutableStateOf(true) }
                    if (isFirstRun.value) {
                        if (initialSharedText != null) viewModel.sharedText = initialSharedText
                        if (initialSharedUris != null) viewModel.sharedUris = initialSharedUris
                        isFirstRun.value = false
                    }

                    val startDestination = if (initialSharedText != null || initialSharedUris != null) {
                        Screen.AddEditNote.createRoute(null)
                    } else {
                        Screen.Home.route
                    }

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
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
