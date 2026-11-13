package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Difficulty
import com.example.model.Ingredient
import com.example.model.Recipe

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val authorId: String,
    val authorName: String,
    val videoUrl: String?,
    val photoUrl: String?,
    val ingredientsSerialized: String, // Formato: "id|name|quantity|unit;..."
    val stepsSerialized: String,       // Formato: "passo1||passo2"
    val durationMinutes: Int,
    val portions: Int,
    val difficulty: String,
    val curiosities: String?,
    val locationName: String?,
    val latitude: Double?,
    val longitude: Double?,
    val likesCount: Int,
    val commentsCount: Int,
    val averageRating: Float,
    val isLiked: Boolean,
    val isFavorite: Boolean,
    val createdAt: Long
)

fun RecipeEntity.toDomain(): Recipe = Recipe(
    id = id,
    title = title,
    description = description,
    authorId = authorId,
    authorName = authorName,
    videoUrl = videoUrl,
    photoUrl = photoUrl,
    ingredients = if (ingredientsSerialized.isBlank()) emptyList() else {
        ingredientsSerialized.split(";").mapNotNull { item ->
            val parts = item.split("|")
            if (parts.size >= 2) {
                Ingredient(
                    id = parts[0],
                    name = parts[1],
                    quantity = parts.getOrNull(2) ?: "",
                    unit = parts.getOrNull(3) ?: ""
                )
            } else null
        }
    },
    steps = if (stepsSerialized.isBlank()) emptyList() else stepsSerialized.split("||"),
    durationMinutes = durationMinutes,
    portions = portions,
    difficulty = runCatching { Difficulty.valueOf(difficulty) }.getOrDefault(Difficulty.MEDIUM),
    curiosities = curiosities,
    locationName = locationName,
    latitude = latitude,
    longitude = longitude,
    likesCount = likesCount,
    commentsCount = commentsCount,
    averageRating = averageRating,
    isLiked = isLiked,
    isFavorite = isFavorite,
    createdAt = createdAt
)

fun Recipe.toEntity(): RecipeEntity = RecipeEntity(
    id = id,
    title = title,
    description = description,
    authorId = authorId,
    authorName = authorName,
    videoUrl = videoUrl,
    photoUrl = photoUrl,
    ingredientsSerialized = ingredients.joinToString(";") { "${it.id}|${it.name}|${it.quantity}|${it.unit}" },
    stepsSerialized = steps.joinToString("||"),
    durationMinutes = durationMinutes,
    portions = portions,
    difficulty = difficulty.name,
    curiosities = curiosities,
    locationName = locationName,
    latitude = latitude,
    longitude = longitude,
    likesCount = likesCount,
    commentsCount = commentsCount,
    averageRating = averageRating,
    isLiked = isLiked,
    isFavorite = isFavorite,
    createdAt = createdAt
)