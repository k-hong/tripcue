package com.example.tripcue.frame.uicomponents.signup

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as Activity
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

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
            if (idToken != null) {
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener(activity) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user != null) {
                                val userInfo = mapOf(
                                    "uid" to user.uid,
                                    "email" to user.email,
                                    "name" to user.displayName,
                                    "photoUrl" to user.photoUrl?.toString()
                                )

                                firestore.collection("users")
                                    .document(user.uid)
                                    .set(userInfo, SetOptions.merge())
                                    .addOnSuccessListener {
                                        Log.d("Firestore", "사용자 정보 저장 성공")

                                        firestore.collection("users").document(user.uid).get()
                                            .addOnSuccessListener { document ->
                                                val hasExtraFields =
                                                    document.getString("nickname") != null &&
                                                            document.getString("region") != null &&
                                                            (document.get("interests") as? List<*>)?.isNotEmpty() == true

                                                if (hasExtraFields) {
                                                    navController.navigate(Routes.Home.route) {
                                                        popUpTo(Routes.Login.route) { inclusive = true }
                                                    }
                                                } else {
                                                    navController.navigate(Routes.FillProfileSurvey.route) {
                                                        popUpTo(Routes.Login.route) { inclusive = true }
                                                    }
                                                }
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("Firestore", "사용자 정보 저장 실패", e)
                                        Toast.makeText(context, "사용자 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Log.e("LoginScreen", "로그인 성공했지만 사용자 정보 없음")
                                Toast.makeText(context, "로그인 사용자 없음", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Log.e("LoginScreen", "로그인 실패", task.exception)
                            Toast.makeText(context, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Log.e("LoginScreen", "ID 토큰이 null임")
                Toast.makeText(context, "Google 로그인 ID 토큰이 null입니다", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e("LoginScreen", "Google 로그인 실패: 결과 코드 ${result.resultCode}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Google로 로그인")
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
                    Log.e("LoginScreen", "Google 로그인 실패", e)
                    Toast.makeText(context, "Google 로그인 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }) {
            Text("Google 로그인")
        }
    }
}
