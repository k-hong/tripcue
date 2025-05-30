package com.example.tripcue.frame.model

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val interests: List<String> = listOf(),
    val location: String = "",
)