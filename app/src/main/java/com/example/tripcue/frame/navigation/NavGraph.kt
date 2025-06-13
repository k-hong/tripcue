package com.example.tripcue.frame.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.tripcue.frame.model.Routes
import com.example.tripcue.frame.uicomponents.AddSchedule
import com.example.tripcue.frame.uicomponents.EditProfileScreen
import com.example.tripcue.frame.uicomponents.MainScreen2
import com.example.tripcue.frame.uicomponents.Schedule.AddScheduleTest
import com.example.tripcue.frame.uicomponents.Schedule.InfoCardScreen
import com.example.tripcue.frame.uicomponents.Schedule.InventoryScheduleTest
import com.example.tripcue.frame.uicomponents.Schedules
import com.example.tripcue.frame.uicomponents.home.MapScreen
import com.example.tripcue.frame.uicomponents.signup.FillProfileScreen
import com.example.tripcue.frame.uicomponents.signup.FillProfileSurveyScreen
import com.example.tripcue.frame.uicomponents.signup.LoginScreen
import com.example.tripcue.frame.uicomponents.signup.SignUpScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.Login.route
    ) {
        // 로그인 전
        composable(Routes.Login.route) {
            LoginScreen(navController)
        }
        composable(Routes.SignUp.route) {
            SignUpScreen(navController)
        }
        composable(Routes.FillProfile.route) {
            FillProfileScreen(navController)
        }
        composable(Routes.FillProfileSurvey.route) {
            FillProfileSurveyScreen(navController)
        }

        composable(Routes.Home.route) {
            MainScreen2(navController)
        }

        composable(
            route = Routes.PlaceDetail.route,
            arguments = listOf(
                navArgument("lat") { type = NavType.FloatType },
                navArgument("lng") { type = NavType.FloatType },
                navArgument("title") { type = NavType.StringType },
                navArgument("isDomestic") { type = NavType.BoolType } // [추가]
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getFloat("lat")?.toDouble() ?: 0.0
            val lng = backStackEntry.arguments?.getFloat("lng")?.toDouble() ?: 0.0
            val encodedTitle = backStackEntry.arguments?.getString("title") ?: ""
            val isDomestic = backStackEntry.arguments?.getBoolean("isDomestic") ?: true // [추가]

            val title = URLDecoder.decode(encodedTitle, StandardCharsets.UTF_8.name())

            MapScreen(lat = lat, lng = lng, title = title, isDomestic = isDomestic)
        }
    }
}
