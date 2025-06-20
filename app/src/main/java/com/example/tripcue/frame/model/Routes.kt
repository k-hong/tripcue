package com.example.tripcue.frame.model

sealed class Routes (val route: String, val isRoot : Boolean = true) {
    object Home : Routes("Home")
    object AddSchedule : Routes("AddSchedule")
    object Schedules : Routes("Schedules")
    object Login : Routes(route = "Login", isRoot = false)
    object SignUp : Routes(route = "SignUp", isRoot = false)
    object FillProfile : Routes(route = "FillProfile", isRoot = false)
    object AddDetails : Routes(route = "AddScheduleTest/{cityDocId}", isRoot = false) {
        fun createRoute(cityDocId: String) = "AddScheduleTest/$cityDocId"
    }
    object InventSchedule : Routes("InventoryScheduleTest/{cityDocId}", isRoot = false) {
        fun createRoute(cityDocId: String) = "InventoryScheduleTest/$cityDocId"
    }
    object InfoCard : Routes(route = "InfoCardScreen/{cityDocId}", isRoot = false) {
        fun createRoute(cityDocId: String) = "InfoCardScreen/$cityDocId"
    }
    object FillProfileSurvey : Routes(route = "fill_profile_survey", isRoot = false)
    object PlaceDetail : Routes("map_screen/{lat}/{lng}/{title}/{isDomestic}", isRoot = false) {
        fun createRoute(lat: Double, lng: Double, title: String, isDomestic: Boolean) =
            "map_screen/$lat/$lng/$title/$isDomestic"
    }
    object MySchedulesMap : Routes("my_schedules_map/{cityDocId}", isRoot = false) {
        fun createRoute(cityDocId: String) = "my_schedules_map/$cityDocId"
    }

    companion object{
        fun getRoutes(route: String): Routes {
            return when{
                route.startsWith("InventoryScheduleTest") -> InventSchedule
                route.startsWith("AddScheduleTest") -> AddDetails
                route.startsWith("InfoCardScreen") -> InfoCard
                route.startsWith("map_screen") -> PlaceDetail
                route.startsWith("my_schedules_map") -> MySchedulesMap
                route == Home.route -> Home
                route == AddSchedule.route -> AddSchedule
                route == Schedules.route -> Schedules
                route == Login.route -> Login
                route == SignUp.route -> SignUp
                route == FillProfile.route -> FillProfile
                route == FillProfileSurvey.route -> FillProfileSurvey

                else -> Home
            }
        }
    }
}