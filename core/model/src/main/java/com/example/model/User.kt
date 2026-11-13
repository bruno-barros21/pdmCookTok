package com.example.model

data class User(
    val id: String,
    val username: String,
    val displayName: String,
    val bio: String = "",
    val avatarUrl: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isFollowing: Boolean = false
)

data class FavoriteList(
    val id: String,
    val name: String,
    val recipeIds: List<String> = emptyList()
)

data class UserDashboardStats(
    val totalViews: Int = 0,
    val totalLikes: Int = 0,
    val totalComments: Int = 0,
    val ratingsHistogram: Map<Int, Int> = emptyMap() // Estrelas (1..5) -> Quantidade de avaliações
)