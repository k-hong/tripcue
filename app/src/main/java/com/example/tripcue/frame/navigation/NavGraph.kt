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
import com.example.tripcue.frame.uicomponents.AddContacts
import com.example.tripcue.frame.uicomponents.Contacts
import com.example.tripcue.frame.uicomponents.Favorites
import com.example.tripcue.frame.uicomponents.Home
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
            Home()
        }
        composable(Routes.Contacts.route){
            Contacts()
        }
        composable(Routes.Favorites.route){
            Favorites()
        }
        composable(Routes.AddContacts.route){
            AddContacts()
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
        composable(Routes.AddSchedule.route) {
            AddScheduleTest(navController)
        }
        composable(Routes.InventSchedule.route) {
            InventoryScheduleTest(navController)
        }
        composable(Routes.InfoCard.route) {
            InfoCardScreen(navController)
        }

    }
}