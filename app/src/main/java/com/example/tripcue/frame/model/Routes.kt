package com.example.tripcue.frame.model

sealed class Routes (val route: String, val isRoot : Boolean = true) {
    object Home : Routes("Home")
    object AddSchedule : Routes("AddSchedule")
    object Schedules : Routes("Schedules")
    object Login : Routes(route = "Login", isRoot = false)
    object SignUp : Routes(route = "SignUp", isRoot = false)
    object FillProfile : Routes(route = "FillProfile", isRoot = false)
    object AddDetails : Routes(route = "AddScheduleTest", isRoot = false)
    object InventSchedule : Routes(route = "InventoryScheduleTest", isRoot = false)
    object InfoCard : Routes(route = "InfoCardScreen", isRoot = false)
    object FillProfileSurvey : Routes(route = "fill_profile_survey", isRoot = false)
    object PlaceDetail : Routes("place_detail", isRoot = false)


    companion object{
        fun getRoutes(route: String): Routes {
            return when(route){
                Home.route -> Home
                AddSchedule.route -> AddSchedule
                Schedules.route -> Schedules
                Login.route -> Login
                SignUp.route -> SignUp
                FillProfile.route -> FillProfile
                AddDetails.route -> AddDetails
                InventSchedule.route -> InventSchedule
                InfoCard.route -> InfoCard
                FillProfileSurvey.route -> FillProfileSurvey

                else -> Home
            }
        }
    }
}

