package com.example.model

enum class Difficulty {
    EASY, MEDIUM, HARD
}

data class Recipe(
    val id: String,
    val title: String,
    val description: String,
    val authorId: String,
    val authorName: String,
    val videoUrl: String?,
    val photoUrl: String?,
    val ingredients: List<Ingredient>,
    val steps: List<String>,
    val durationMinutes: Int,
    val portions: Int,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val curiosities: String? = null,
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val averageRating: Float = 0f,
    val isLiked: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)