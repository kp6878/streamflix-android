package com.example.streamfilx_androidtv.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object ProfilePicker : Screen("profile_picker")
    object Home : Screen("home")
    object Browse : Screen("browse")
    object Search : Screen("search")
    object Library : Screen("library")
    object Downloads : Screen("downloads")
    object Settings : Screen("settings")
    object Player : Screen("player")

    object Detail : Screen("detail/{type}/{id}") {
        const val ROUTE = "detail/{type}/{id}"
        fun route(type: String, id: String) = "detail/$type/$id"
    }

    object ProfileEditor : Screen("profile_editor/{profileId}") {
        const val ROUTE = "profile_editor/{profileId}"
        fun route(profileId: String? = null) = "profile_editor/$profileId"
    }
}
