package com.example.tripcue.frame.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person

object NavBarItems{
    val BarItems = listOf(
        BarItem(
            title = Routes.Home.route,
            selectIcon = Icons.Outlined.Home,
            onSelectedIcon = Icons.Filled.Home,
            route = Routes.Home.route
        ),
        BarItem(
            title = Routes.AddSchedule.route,
            selectIcon = Icons.Outlined.AddCircle,
            onSelectedIcon = Icons.Filled.AddCircle,
            route = Routes.AddSchedule.route
        ),
        BarItem(
            title = Routes.Schedules.route,
            selectIcon = Icons.Outlined.CalendarMonth,
            onSelectedIcon = Icons.Filled.CalendarMonth,
            route = Routes.Schedules.route,
        )
    )
}