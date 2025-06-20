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
import com.google.firebase.auth.FirebaseAuth
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun NavGraph(navController: NavHostController) {
    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        Routes.Home.route
    } else {
        Routes.Login.route
    }
    NavHost(
        navController = navController,
        startDestination = startDestination
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

        composable(Routes.AddSchedule.route) {
            AddSchedule(navController, onDone = { navController.popBackStack() })
        }

        composable(
            route = "${Routes.AddDetails.route}",
            arguments = listOf(navArgument("cityDocId") { type = NavType.StringType })
        ) {
            val cityDocId = it.arguments?.getString("cityDocId") ?: return@composable
            AddScheduleTest(navController, cityDocId)
        }

        composable(Routes.Schedules.route) {
            Schedules(navController)
        }

        composable(
            route = "${Routes.InfoCard.route}",
            arguments = listOf(navArgument("cityDocId") { type = NavType.StringType })
        ) {
            val cityDocId = it.arguments?.getString("cityDocId") ?: return@composable
            InfoCardScreen(navController, cityDocId)
        }

        composable(
            route = "${Routes.InventSchedule.route}",
            arguments = listOf(navArgument("cityDocId") { type = NavType.StringType })
        ) {
            val cityDocId = it.arguments?.getString("cityDocId") ?: return@composable
            InventoryScheduleTest(navController, cityDocId)
        }

        composable("edit_profile") {
            EditProfileScreen(navController)
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