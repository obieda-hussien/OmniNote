package com.omninote.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AddEditNote : Screen("addEditNote") {
        const val noteIdArg = "noteId"
        val routeWithArgs = "$route?$noteIdArg={$noteIdArg}"
        fun createRoute(noteId: Int?) = if (noteId != null) "$route?$noteIdArg=$noteId" else route
    }
}
