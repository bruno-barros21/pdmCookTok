package com.example.model

data class Comment(
    val id: String,
    val recipeId: String,
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String?,
    val text: String,
    val rating: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)