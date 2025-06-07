package com.example.tripcue.frame.navigation

import android.R.attr.type
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.tripcue.frame.model.Routes
import com.example.tripcue.frame.model.ScheduleData
import com.example.tripcue.frame.uicomponents.Home
import com.example.tripcue.frame.uicomponents.AddSchedule
import com.example.tripcue.frame.uicomponents.Schedules
import com.example.tripcue.frame.uicomponents.Schedule.AddScheduleTest
import com.example.tripcue.frame.uicomponents.Schedule.InfoCardScreen
import com.example.tripcue.frame.uicomponents.Schedule.InventoryScheduleTest
import com.example.tripcue.frame.uicomponents.signup.FillProfileScreen
import com.example.tripcue.frame.uicomponents.signup.LoginScreen
import com.example.tripcue.frame.uicomponents.signup.SignUpScreen
import com.example.tripcue.frame.viewmodel.ScheduleViewModel
import com.google.gson.Gson
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.Home.route
    ){
        composable(Routes.Home.route){
            Home() // 홈 화면
        }
        composable(Routes.AddSchedule.route){
            AddSchedule( onDone = {
                navController.popBackStack() // 또는 안전하게 Home으로 이동
                // navController.navigate(Routes.Home.route) { popUpTo(Routes.Home.route) { inclusive = true } }
            }) // 일정 화면
        }
        composable(Routes.Schedules.route){
            Schedules() // 일정 화면
        }

        composable(Routes.Login.route) {
            LoginScreen(navController)
        }
        composable(Routes.SignUp.route) {
            SignUpScreen(navController)
        }
        composable(Routes.FillProfile.route) {
            FillProfileScreen(navController)
        }


        composable(Routes.InventSchedule.route) {
            InventoryScheduleTest(navController)
        }
        composable(Routes.InfoCard.route) {
            InfoCardScreen(navController)
        }

    }
}