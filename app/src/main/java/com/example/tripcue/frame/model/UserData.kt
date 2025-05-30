package com.example.tripcue.frame.model

data class UserData(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val nickname: String = "",
    val age: Int = 0,
    val gender: String = "",
    val region: String = "",
    val interests: List<String> = emptyList(),

)
