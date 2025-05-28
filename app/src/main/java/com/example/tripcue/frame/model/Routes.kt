package com.example.tripcue.frame.model

sealed class Routes (val route: String, val isRoot : Boolean = true) {
    object Home : Routes("Home")
    object Contacts : Routes("Contacts")
    object Favorites : Routes("Favorites")
    object AddContacts : Routes("AddContacts", isRoot = false)

    companion object{
        fun getRoutes(route: String): Routes {
            return when(route){
                Home.route -> Home
                Contacts.route -> Contacts
                Favorites.route -> Favorites
                AddContacts.route -> AddContacts
                else -> Home
            }
        }
    }
}

