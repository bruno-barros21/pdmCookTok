package com.example.data

import com.example.database.RecipeDao
import com.example.database.RecipeEntity
import com.example.database.toDomain
import com.example.database.toEntity
import com.example.model.Recipe
import com.example.network.CookTokApi
import com.example.network.InventorySearchRequestDto
import com.example.network.toDomain
import com.example.network.toDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface RecipeRepository {
    fun observeFeed(): Flow<List<Recipe>>
    fun observeFavorites(): Flow<List<Recipe>>
    fun searchLocalRecipes(query: String): Flow<List<Recipe>>
    suspend fun refreshFeed(filter: String = "recent"): Result<Unit>
    suspend fun searchByInventory(ingredients: List<String>): Result<List<Recipe>>
    suspend fun createRecipe(recipe: Recipe): Result<Recipe>
    suspend fun toggleFavorite(recipeId: String)
    suspend fun toggleLike(recipeId: String): Result<Unit>
}

@Singleton
class OfflineFirstRecipeRepository @Inject constructor(
    private val recipeDao: RecipeDao,
    private val cookTokApi: CookTokApi
) : RecipeRepository {

    override fun observeFeed(): Flow<List<Recipe>> =
        recipeDao.observeAll().map { entities ->
            entities.map(RecipeEntity::toDomain)
        }

    override fun observeFavorites(): Flow<List<Recipe>> =
        recipeDao.observeFavorites().map { entities ->
            entities.map(RecipeEntity::toDomain)
        }

    override fun searchLocalRecipes(query: String): Flow<List<Recipe>> =
        (if (query.isBlank()) recipeDao.observeAll() else recipeDao.searchRecipes(query))
            .map { entities -> entities.map(RecipeEntity::toDomain) }

    override suspend fun refreshFeed(filter: String): Result<Unit> = runCatching {
        val remoteDtos = cookTokApi.fetchFeed(filter)
        val entities = remoteDtos.map { dto ->
            val existing = recipeDao.getById(dto.id)
            dto.toDomain()
                .copy(isFavorite = existing?.isFavorite ?: dto.isFavorite)
                .toEntity()
        }
        recipeDao.upsertAll(entities)
    }

    override suspend fun searchByInventory(ingredients: List<String>): Result<List<Recipe>> = runCatching {
        val remoteDtos = cookTokApi.searchByInventory(InventorySearchRequestDto(ingredients))
        val recipes = remoteDtos.map { it.toDomain() }
        recipeDao.upsertAll(recipes.map { it.toEntity() })
        recipes
    }.recoverCatching {
        // Fallback offline: filtra localmente no Room caso não haja rede
        val query = ingredients.firstOrNull().orEmpty()
        recipeDao.getById(query)?.let { listOf(it.toDomain()) } ?: emptyList()
    }

    override suspend fun createRecipe(recipe: Recipe): Result<Recipe> = runCatching {
        // Guarda primeiro localmente para suporte offline imediato
        recipeDao.upsert(recipe.toEntity())
        val createdDto = cookTokApi.createRecipe(recipe.toDto())
        val syncedRecipe = createdDto.toDomain()
        recipeDao.upsert(syncedRecipe.toEntity())
        syncedRecipe
    }

    override suspend fun toggleFavorite(recipeId: String) {
        val current = recipeDao.getById(recipeId) ?: return
        recipeDao.setFavorite(recipeId, !current.isFavorite)
    }

    override suspend fun toggleLike(recipeId: String): Result<Unit> = runCatching {
        val current = recipeDao.getById(recipeId) ?: return@runCatching
        val newLiked = !current.isLiked
        val newCount = if (newLiked) current.likesCount + 1 else (current.likesCount - 1).coerceAtLeast(0)
        // Atualização otimista local no Room
        recipeDao.updateLike(recipeId, newLiked, newCount)
        // Sincroniza com a API
        cookTokApi.toggleLike(recipeId)
    }
}