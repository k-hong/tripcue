package com.example.tripcue.frame.model

sealed class Routes (val route: String, val isRoot : Boolean = true) {
    object Home : Routes("Home")
    object Contacts : Routes("Contacts")
    object Favorites : Routes("Favorites")
    object AddContacts : Routes("AddContacts", isRoot = false)
    object Login : Routes(route = "Login", isRoot = false)
    object SignUp : Routes(route = "SignUp", isRoot = false)
    object FillProfile : Routes(route = "FillProfile", isRoot = false)
    object AddSchedule : Routes(route = "AddScheduleTest", isRoot = false)
    object InventSchedule : Routes(route = "InventoryScheduleTest", isRoot = false)
    object InfoCard : Routes(route = "InfoCardScreen", isRoot = false)

    companion object{
        fun getRoutes(route: String): Routes {
            return when(route){
                Home.route -> Home
                Contacts.route -> Contacts
                Favorites.route -> Favorites
                AddContacts.route -> AddContacts
                Login.route -> Login
                SignUp.route -> SignUp
                FillProfile.route -> FillProfile
                AddSchedule.route -> AddSchedule
                InventSchedule.route -> InventSchedule
                InfoCard.route -> InfoCard
                else -> Home
            }
        }
    }
}

