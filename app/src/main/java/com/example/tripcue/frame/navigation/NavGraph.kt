package com.example.tripcue.frame.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.tripcue.frame.model.Routes
import com.example.tripcue.frame.uicomponents.EditProfileScreen
import com.example.tripcue.frame.uicomponents.MainScreen2
import com.example.tripcue.frame.uicomponents.Schedule.AddScheduleTest
import com.example.tripcue.frame.uicomponents.Schedule.InfoCardScreen
import com.example.tripcue.frame.uicomponents.Schedule.InventoryScheduleTest
import com.example.tripcue.frame.uicomponents.signup.FillProfileScreen
import com.example.tripcue.frame.uicomponents.signup.FillProfileSurveyScreen
import com.example.tripcue.frame.uicomponents.signup.LoginScreen
import com.example.tripcue.frame.uicomponents.signup.SignUpScreen

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

        // 내부 기능 화면들
        composable(Routes.AddSchedule.route) {
            AddScheduleTest(navController)
        }
        composable(Routes.Schedules.route) {
        //    Schedules()
        }
        composable(Routes.InventSchedule.route) {
            InventoryScheduleTest(navController)
        }
        composable(Routes.InfoCard.route) {
            InfoCardScreen(navController)
        }
        composable("edit_profile") {
            EditProfileScreen(navController)
        }
    }
}
