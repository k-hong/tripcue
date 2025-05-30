package com.example.tripcue.frame.uicomponents.signup

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tripcue.R
import com.example.tripcue.frame.model.Routes
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun SignUpScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as Activity
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var oneTapClient: SignInClient? by remember { mutableStateOf(null) }
    var signInRequest: BeginSignInRequest? by remember { mutableStateOf(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        oneTapClient = Identity.getSignInClient(context)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(context.getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val credential = oneTapClient?.getSignInCredentialFromIntent(result.data)
            val idToken = credential?.googleIdToken
            val userId = credential?.id
            if (idToken != null && userId != null) {
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener(activity) { task ->
                        if (task.isSuccessful) {
                            db.collection("users").document(userId).get()
                                .addOnSuccessListener { document ->
                                    if (!document.exists()) {
                                        // 신규 유저 → 프로필 입력 화면으로 이동
                                        navController.navigate(Routes.FillProfile.route) {
                                            popUpTo(Routes.SignUp.route) {
                                                inclusive = true
                                            }
                                        }
                                    } else {
                                        // 기존 유저 → 홈으로 이동
                                        navController.navigate(Routes.Home.route) {
                                            popUpTo(Routes.SignUp.route) {
                                                inclusive = true
                                            }
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("SignUpScreen", "자동 로그인 시 Firestore 조회 실패: ${e.message}")
                                }
                        } else {
                            Toast.makeText(context, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Google로 회원가입")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            coroutineScope.launch {
                try {
                    val result = oneTapClient?.beginSignIn(signInRequest!!)?.await()
                    val intentSender = result?.pendingIntent?.intentSender
                    if (intentSender != null) {
                        val request = IntentSenderRequest.Builder(intentSender).build()
                        launcher.launch(request)
                    }
                } catch (e: Exception) {
                    Log.e("SignUpScreen", "Google 로그인 실패", e)
                }
            }
        }) {
            Text("Google 회원가입")
        }
    }
}
