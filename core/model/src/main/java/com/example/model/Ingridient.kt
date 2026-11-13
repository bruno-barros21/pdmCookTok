package com.example.model

data class Ingredient(
    val id: String,
    val name: String,
    val quantity: String = "",
    val unit: String = ""
)
