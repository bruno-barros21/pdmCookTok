package com.example.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CookTokApi {

    @GET("api/recipes/feed")
    suspend fun fetchFeed(
        @Query("filter") filter: String = "recent"
    ): List<RecipeDto>

    @POST("api/recipes")
    suspend fun createRecipe(
        @Body recipe: RecipeDto
    ): RecipeDto

    @POST("api/recipes/search")
    suspend fun searchByInventory(
        @Body request: InventorySearchRequestDto
    ): List<RecipeDto>

    @POST("api/recipes/{id}/like")
    suspend fun toggleLike(
        @Path("id") recipeId: String
    ): RecipeDto

    @GET("api/recipes/{id}/comments")
    suspend fun fetchComments(
        @Path("id") recipeId: String
    ): List<CommentDto>

    @POST("api/recipes/{id}/comments")
    suspend fun postComment(
        @Path("id") recipeId: String,
        @Body comment: CommentDto
    ): CommentDto

    @GET("api/users/{id}")
    suspend fun fetchUserProfile(
        @Path("id") userId: String
    ): UserDto

    @GET("api/users/{id}/friends")
    suspend fun fetchUserFriends(
        @Path("id") userId: String
    ): List<UserDto>

    @GET("api/users/{id}/stats")
    suspend fun fetchUserStats(
        @Path("id") userId: String
    ): UserDashboardStatsDto
}