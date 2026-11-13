package com.example.network

import com.example.model.Comment
import com.example.model.Difficulty
import com.example.model.Ingredient
import com.example.model.Recipe
import com.example.model.User
import com.example.model.UserDashboardStats

data class IngredientDto(
    val id: String,
    val name: String,
    val quantity: String = "",
    val unit: String = ""
)

data class RecipeDto(
    val id: String,
    val title: String,
    val description: String,
    val authorId: String,
    val authorName: String,
    val videoUrl: String? = null,
    val photoUrl: String? = null,
    val ingredients: List<IngredientDto> = emptyList(),
    val steps: List<String> = emptyList(),
    val durationMinutes: Int = 0,
    val portions: Int = 1,
    val difficulty: String = "MEDIUM",
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

data class UserDto(
    val id: String,
    val username: String,
    val displayName: String,
    val bio: String = "",
    val avatarUrl: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isFollowing: Boolean = false
)

data class UserDashboardStatsDto(
    val totalViews: Int = 0,
    val totalLikes: Int = 0,
    val totalComments: Int = 0,
    val ratingsHistogram: Map<Int, Int> = emptyMap()
)

data class CommentDto(
    val id: String,
    val recipeId: String,
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String? = null,
    val text: String,
    val rating: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class InventorySearchRequestDto(
    val ingredients: List<String>
)

// Mapeadores DTO -> Domain (:core:model)
fun IngredientDto.toDomain(): Ingredient = Ingredient(
    id = id,
    name = name,
    quantity = quantity,
    unit = unit
)

fun Ingredient.toDto(): IngredientDto = IngredientDto(
    id = id,
    name = name,
    quantity = quantity,
    unit = unit
)

fun RecipeDto.toDomain(): Recipe = Recipe(
    id = id,
    title = title,
    description = description,
    authorId = authorId,
    authorName = authorName,
    videoUrl = videoUrl,
    photoUrl = photoUrl,
    ingredients = ingredients.map { it.toDomain() },
    steps = steps,
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

fun Recipe.toDto(): RecipeDto = RecipeDto(
    id = id,
    title = title,
    description = description,
    authorId = authorId,
    authorName = authorName,
    videoUrl = videoUrl,
    photoUrl = photoUrl,
    ingredients = ingredients.map { it.toDto() },
    steps = steps,
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

fun UserDto.toDomain(): User = User(
    id = id,
    username = username,
    displayName = displayName,
    bio = bio,
    avatarUrl = avatarUrl,
    followersCount = followersCount,
    followingCount = followingCount,
    isFollowing = isFollowing
)

fun UserDashboardStatsDto.toDomain(): UserDashboardStats = UserDashboardStats(
    totalViews = totalViews,
    totalLikes = totalLikes,
    totalComments = totalComments,
    ratingsHistogram = ratingsHistogram
)

fun CommentDto.toDomain(): Comment = Comment(
    id = id,
    recipeId = recipeId,
    authorId = authorId,
    authorName = authorName,
    authorAvatarUrl = authorAvatarUrl,
    text = text,
    rating = rating,
    createdAt = createdAt
)