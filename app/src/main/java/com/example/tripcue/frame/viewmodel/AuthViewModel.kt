package com.example.tripcue.frame.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        // 현재 로그인된 사용자 확인
        _currentUser.value = FirebaseAuth.getInstance().currentUser

        // 인증 상태 변경 리스너
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            _currentUser.value = auth.currentUser
        }
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        _isLoading.value = true
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnSuccessListener {
                _isLoading.value = false
                onSuccess()
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                onError(e)
            }
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }

    fun isLoggedIn(): Boolean = currentUser.value != null
}