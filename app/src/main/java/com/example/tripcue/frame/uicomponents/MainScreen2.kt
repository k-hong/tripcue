package com.example.tripcue.frame.uicomponents

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.tripcue.frame.model.Routes
import com.example.tripcue.frame.navigation.BottomNavigationBar
import com.example.tripcue.frame.uicomponents.home.Home
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen2(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute by remember(backStackEntry) {
        derivedStateOf {
            backStackEntry?.destination?.route?.let {
                Routes.getRoutes(it)
            } ?: Routes.Home
        }
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var isEditMode by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(false) } // ✅ 새로고침 트리거 상태 추가

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    isEditMode = isEditMode,
                    onEditClick = { isEditMode = true },
                    onDoneClick = {
                        isEditMode = false
                        refreshTrigger = true // ✅ 저장 시 새로고침 트리거
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (currentRoute.isRoot)
                    TopAppBar(
                        title = { Text(text = currentRoute.route) },
                        navigationIcon = {
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    if (drawerState.isOpen) drawerState.close() else drawerState.open()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                else
                    TopAppBar(
                        title = { },
                        navigationIcon = {
                            IconButton(onClick = {
                                navController.popBackStack()
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                    contentDescription = null
                                )
                            }
                        }
                    )
            },
            bottomBar = {
                if (currentRoute.isRoot)
                    BottomNavigationBar(navController)
            },
            floatingActionButton = {
                if (currentRoute == Routes.Schedules)
                    FloatingActionButton(onClick = {
                        navController.navigate(Routes.AddSchedule.route)
                    }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    }
            }
        ) { contentPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                when (currentRoute) {
                    Routes.Home -> Home(navController, refreshTrigger = refreshTrigger)
                    else -> Text("페이지를 찾을 수 없습니다")
                }
                // ✅ 트리거 사용 후 초기화
                if (refreshTrigger) {
                    LaunchedEffect(Unit) {
                        refreshTrigger = false
                    }
                }
            }
        }
    }
}
