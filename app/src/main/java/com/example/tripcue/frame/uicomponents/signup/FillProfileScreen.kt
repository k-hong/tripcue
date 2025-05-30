package com.example.tripcue.frame.uicomponents.signup

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tripcue.frame.model.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun FillProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val db = FirebaseFirestore.getInstance()

    var nickname by remember { mutableStateOf("") }
    var interest by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    if (uid == null) {
        Toast.makeText(context, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("프로필을 입력해주세요")
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("닉네임") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = interest,
            onValueChange = { interest = it },
            label = { Text("관심사") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                isLoading = true
                val data = mapOf(
                    "nickname" to nickname,
                    "interest" to interest
                )
                db.collection("users").document(uid)
                    .update(data)
                    .addOnSuccessListener {
                        isLoading = false
                        Toast.makeText(context, "프로필 저장 완료!", Toast.LENGTH_SHORT).show()
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.FillProfile.route) { inclusive = true }
                        }
                    }
                    .addOnFailureListener {
                        isLoading = false
                        Toast.makeText(context, "유저 정보 저장 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            },
            enabled = nickname.isNotBlank() && interest.isNotBlank()
        ) {
            Text("저장하고 시작하기")
        }
    }
}
