package com.example.search

import com.example.model.Recipe

/**
 * Seletor prévio exigido antes de pesquisar: Receitas ou Utilizadores.
 */
enum class SearchTarget(val label: String) {
    RECIPES("Receitas"),
    USERS("Utilizadores")
}

/**
 * Critérios de ordenação dos resultados de receitas.
 */
enum class RecipeSortOption(val label: String) {
    RELEVANCE("Relevância"),
    HIGHEST_RATING("Melhor Rating"),
    MOST_LIKED("Mais Gostos"),
    SHORTEST_DURATION("Menor Duração"),
    NEWEST("Mais Recentes")
}

/**
 * Representação enriquecida de uma receita para pesquisa, filtros e ordenação.
 */
data class SearchRecipeItem(
    val recipe: Recipe,
    val authorName: String,
    val category: String,
    val rating: Float,
    val likesCount: Int,
    val ingredientNames: List<String>
)

/**
 * Utilizador registado pesquisável na aba "Utilizadores".
 */
data class SearchableUser(
    val id: String,
    val displayName: String,
    val username: String,
    val bio: String,
    val followersCount: Int,
    val publishedRecipes: List<Recipe> = emptyList()
)

/**
 * Registo do histórico de pesquisas recentes.
 */
data class RecentSearchItem(
    val query: String,
    val target: SearchTarget
)

/**
 * Filtros adicionais (Duração, Dificuldade, Categoria e Ordenação).
 */
data class SearchFilters(
    val maxDurationMinutes: Int? = null, // null = sem limite de tempo
    val selectedDifficulties: Set<String> = emptySet(),
    val selectedCategories: Set<String> = emptySet(),
    val sortBy: RecipeSortOption = RecipeSortOption.RELEVANCE
) {
    val activeCount: Int
        get() = (if (maxDurationMinutes != null) 1 else 0) +
            selectedDifficulties.size +
            selectedCategories.size +
            (if (sortBy != RecipeSortOption.RELEVANCE) 1 else 0)
}

data class SearchUiState(
    val isLoading: Boolean = false,
    /** Seletor ativo antes/durante a pesquisa: Receitas vs. Utilizadores */
    val searchTarget: SearchTarget = SearchTarget.RECIPES,
    /** Texto introduzido na barra de pesquisa (Lupa) */
    val query: String = "",
    /** Indica se a caixa de pesquisa está ativa/focada */
    val isSearchActive: Boolean = false,
    /** Histórico de pesquisas recentes */
    val recentSearches: List<RecentSearchItem> = listOf(
        RecentSearchItem("Lasanha", SearchTarget.RECIPES),
        RecentSearchItem("Frango", SearchTarget.RECIPES),
        RecentSearchItem("mariana_cooks", SearchTarget.USERS)
    ),
    /** Catálogo completo de ingredientes disponíveis para tick box no Inventário */
    val availableIngredients: List<String> = defaultPantryIngredients(),
    /** Ingredientes marcados no Inventário (todos têm de existir na receita) */
    val selectedInventoryIngredients: Set<String> = emptySet(),
    /** Controla a abertura do modal de Inventário (tick boxes) */
    val isInventoryOpen: Boolean = false,
    /** Filtros adicionais (duração, dificuldade, categoria, ordenação) */
    val filters: SearchFilters = SearchFilters(),
    /** Controla a abertura do modal de Filtros */
    val isFiltersOpen: Boolean = false,
    /** Resultados filtrados de receitas */
    val recipeResults: List<SearchRecipeItem> = emptyList(),
    /** Resultados filtrados de utilizadores */
    val userResults: List<SearchableUser> = emptyList(),
    /** Receita aberta no detalhe ao clicar num resultado */
    val selectedRecipeForDetail: SearchRecipeItem? = null,
    /** Utilizador aberto no modal ao clicar num resultado de utilizador */
    val selectedUserForDetail: SearchableUser? = null
)

fun defaultPantryIngredients(): List<String> = listOf(
    "Frango",
    "Arroz",
    "Natas",
    "Massa",
    "Carne Picada",
    "Queijo",
    "Tomate",
    "Ovo",
    "Batata",
    "Bacalhau",
    "Alho",
    "Cebola",
    "Azeite",
    "Leite",
    "Chocolate",
    "Açúcar",
    "Farinha"
)