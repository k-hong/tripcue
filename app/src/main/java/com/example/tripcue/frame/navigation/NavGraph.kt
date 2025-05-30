package com.example.tripcue.frame.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.tripcue.frame.model.Routes
import com.example.tripcue.frame.uicomponents.AddContacts
import com.example.tripcue.frame.uicomponents.Contacts
import com.example.tripcue.frame.uicomponents.Favorites
import com.example.tripcue.frame.uicomponents.Home
import com.example.tripcue.frame.uicomponents.signup.FillProfileScreen
import com.example.tripcue.frame.uicomponents.signup.LoginScreen
import com.example.tripcue.frame.uicomponents.signup.SignUpScreen

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

    }
}