package com.example.tripcue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tripcue.frame.model.Routes
import com.example.tripcue.frame.model.ScheduleData
import com.example.tripcue.frame.model.Transportation
import com.example.tripcue.frame.model.WeatherInfo
import com.example.tripcue.frame.uicomponents.MainScreen2
import com.example.tripcue.frame.uicomponents.Schedule.InfoCardScreen
import com.example.tripcue.frame.uicomponents.signup.FillProfileScreen
import com.example.tripcue.frame.uicomponents.signup.LoginScreen
import com.example.tripcue.frame.uicomponents.signup.SignUpScreen
import com.example.tripcue.ui.theme.TripcueTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TripcueTheme {
                MainScreen2()
            }
            val navController = rememberNavController()

            // 👇 항상 LoginScreen부터 시작하도록 지정
            NavHost(
                navController = navController, startDestination = Routes.Login.route
            ) {
                composable(Routes.Login.route) {
                    LoginScreen(navController)
                }
                composable(Routes.SignUp.route) {
                    SignUpScreen(navController)
                }
                composable(Routes.FillProfile.route) {
                    FillProfileScreen(navController)
                }
                composable(Routes.Home.route) {
                    MainScreen2()
                }
            }
//            InfoCardScreen(
//                initialData = ScheduleData(
//                    date = LocalDate.now().toString(),
//                    location = "서울",
//                    transportation = Transportation.BUS,
//                    weather = WeatherInfo("맑음", 24.5)
//                ),
//                onUpdate = { updated ->
//                    // 여기서 업데이트된 정보를 저장하거나 출력할 수 있어요
//                    println("수정된 데이터: $updated")
//                }
//            )
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TripcueTheme {
        Greeting("Android")
    }
}