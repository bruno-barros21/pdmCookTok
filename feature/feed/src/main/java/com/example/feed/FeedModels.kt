package com.example.feed

import com.example.model.Recipe
import java.util.UUID

enum class FeedSortMode(val label: String) {
    COOKTOK_ALGORITHM("Algoritmo CookTok"),
    HIGHEST_RATING("Melhor Rating"),
    MOST_LIKED("Mais Gostos"),
    NEWEST("Mais Recentes")
}

data class FeedComment(
    val id: String = UUID.randomUUID().toString(),
    val authorUsername: String,
    val text: String,
    val timestampLabel: String
)

data class FeedFriend(
    val id: String,
    val displayName: String,
    val username: String,
    val bio: String,
    val followersCount: Int,
    val followingCount: Int,
    val publishedRecipes: List<Recipe> = emptyList()
)

data class FeedRecipeItem(
    val recipe: Recipe,
    val authorDisplayName: String,
    val authorUsername: String,
    val category: String,
    val description: String,
    val servings: Int,
    val ingredientNames: List<String>,
    val preparationSteps: List<String>,
    val viewsCount: Int,
    val likesCount: Int,
    val isLikedByMe: Boolean = false,
    val isFavoriteByMe: Boolean = false,
    val averageRating: Float,
    val ratingsCount: Int,
    val myStarRating: Int? = null, // 0 a 5 estrelas atribuídas pelo utilizador
    val comments: List<FeedComment> = emptyList()
)

data class FeedFilterState(
    val minRating: Float? = null, // null = qualquer, 4.0f = ≥ 4★, 4.5f = ≥ 4.5★
    val selectedCategory: String? = null,
    val sortMode: FeedSortMode = FeedSortMode.COOKTOK_ALGORITHM
) {
    val activeFiltersCount: Int
        get() = (if (minRating != null) 1 else 0) +
            (if (selectedCategory != null) 1 else 0) +
            (if (sortMode != FeedSortMode.COOKTOK_ALGORITHM) 1 else 0)
}

data class FeedUiState(
    /** Regra global: Visitantes veem Top Ratings/Likes e não podem dar like, comentar, avaliar, seguir ou partilhar */
    val isGuest: Boolean = false,
    val isLoading: Boolean = false,
    val isOfflineFallback: Boolean = false,
    /** Interesses do utilizador registado usados pelo Algoritmo do CookTok */
    val userPreferredCategories: Set<String> = setOf("Massas", "Tradicional"),
    /** IDs dos autores que o utilizador segue (Botão Seguir Dinâmico) */
    val followedAuthorIds: Set<String> = setOf("user_2"),
    /** Lista filtrada e ordenada de CookToks prontos para o VerticalPager */
    val feedItems: List<FeedRecipeItem> = emptyList(),
    /** Estado dos filtros (Rating, Categoria e Ordenação) */
    val filterState: FeedFilterState = FeedFilterState(),
    val isFilterSheetOpen: Boolean = false,
    /** Lista de amigos acessível a partir do Feed (botão Amigos + ícone Olho) */
    val friends: List<FeedFriend> = emptyList(),
    val isFriendsSheetOpen: Boolean = false,
    val inspectedFriend: FeedFriend? = null,
    /** Modais de interação por receita */
    val selectedRecipeForDetails: FeedRecipeItem? = null,
    val selectedRecipeForComments: FeedRecipeItem? = null,
    val selectedRecipeForRating: FeedRecipeItem? = null,
    val selectedRecipeForShare: FeedRecipeItem? = null,
    /** Mensagem de feedback (Snackbar) */
    val userMessage: String? = null
)

val defaultFeedCategories = listOf(
    "Prato Principal",
    "Massas",
    "Tradicional",
    "Rápidas & Snacks",
    "Sobremesas"
)