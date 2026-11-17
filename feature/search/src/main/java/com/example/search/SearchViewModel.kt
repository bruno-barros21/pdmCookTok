package com.example.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.RecipeRepository
import com.example.model.Recipe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var allEnrichedRecipes: List<SearchRecipeItem> = emptyList()
    private var allRegisteredUsers: List<SearchableUser> = emptyList()

    init {
        observeRepositoryData()
    }

    private fun observeRepositoryData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            recipeRepository.getRecipes().collect { recipes ->
                allEnrichedRecipes = recipes.map { enrichRecipe(it) }
                allRegisteredUsers = buildSearchableUsers(recipes)

                // Combina os ingredientes reais das receitas com a lista base da despensa
                val dynamicIngredients = allEnrichedRecipes
                    .flatMap { it.ingredientNames }
                    .map { it.trim().replaceFirstChar { c -> c.uppercase() } }
                    .filter { it.isNotBlank() }

                val mergedPantry = (defaultPantryIngredients() + dynamicIngredients)
                    .distinctBy { it.lowercase() }
                    .sorted()

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        availableIngredients = mergedPantry
                    )
                }
                recomputeResults()
            }
        }
    }

    /**
     * Enriquece o modelo Recipe com metadados pesquisáveis (ingredientes normalizados, categoria e métricas).
     */
    private fun enrichRecipe(recipe: Recipe): SearchRecipeItem {
        val rawIngredients = recipe.ingredients.map { it.name.trim() }.filter { it.isNotBlank() }
        // Caso alguma receita venha sem lista preenchida, infere palavras-chave do título
        val effectiveIngredients = rawIngredients.ifEmpty {
            inferIngredientsFromTitle(recipe.title)
        }

        val category = inferCategory(recipe.title, recipe.prepTimeMinutes)
        val hash = abs(recipe.id.hashCode())
        val rating = 3.8f + ((hash % 13) * 0.1f) // Entre 3.8 e 5.0
        val likes = 45 + (hash % 320)

        val authorDisplay = when (recipe.authorId) {
            "user_1" -> "Chef_Bruno"
            "user_2" -> "mariana_cooks"
            "user_3" -> "chef_tiago"
            "user_4" -> "ines_pastelaria"
            else -> recipe.authorId
        }

        return SearchRecipeItem(
            recipe = recipe,
            authorName = authorDisplay,
            category = category,
            rating = rating.coerceAtMost(5.0f),
            likesCount = likes,
            ingredientNames = effectiveIngredients
        )
    }

    private fun inferIngredientsFromTitle(title: String): List<String> {
        val lower = title.lowercase()
        val detected = mutableListOf<String>()
        if ("frango" in lower) detected += "Frango"
        if ("arroz" in lower) detected += "Arroz"
        if ("lasanha" in lower) {
            detected += listOf("Massa", "Carne Picada", "Queijo", "Tomate")
        }
        if ("bolo" in lower || "mousse" in lower || "doce" in lower) {
            detected += listOf("Açúcar", "Ovo", "Chocolate", "Farinha")
        }
        if ("bacalhau" in lower) detected += listOf("Bacalhau", "Batata", "Azeite", "Cebola")
        return detected.ifEmpty { listOf("Azeite", "Alho", "Cebola") }
    }

    private fun inferCategory(title: String, prepMinutes: Int): String {
        val lower = title.lowercase()
        return when {
            "bolo" in lower || "mousse" in lower || "doce" in lower || "tarte" in lower -> "Sobremesas"
            "lasanha" in lower || "massa" in lower || "esparguete" in lower -> "Massas"
            prepMinutes <= 20 -> "Rápidas & Snacks"
            "bacalhau" in lower || "francesinha" in lower || "cozido" in lower -> "Tradicional"
            else -> "Prato Principal"
        }
    }

    private fun buildSearchableUsers(recipes: List<Recipe>): List<SearchableUser> {
        return listOf(
            SearchableUser(
                id = "user_1",
                displayName = "Bruno Barros",
                username = "Chef_Bruno",
                bio = "Apaixonado por cozinha portuguesa e receitas rápidas no CookTok.",
                followersCount = 248,
                publishedRecipes = recipes.filter { it.authorId == "user_1" }
            ),
            SearchableUser(
                id = "user_2",
                displayName = "Mariana Silva",
                username = "mariana_cooks",
                bio = "Especialista em massas frescas, lasanhas e comida mediterrânica.",
                followersCount = 512,
                publishedRecipes = recipes.take(2)
            ),
            SearchableUser(
                id = "user_3",
                displayName = "Tiago Mendes",
                username = "chef_tiago",
                bio = "Grelhados, petiscos tradicionais e pratos de frango.",
                followersCount = 329,
                publishedRecipes = recipes.drop(2).take(2)
            ),
            SearchableUser(
                id = "user_4",
                displayName = "Inês Costa",
                username = "ines_pastelaria",
                bio = "Doces conventuais, sobremesas e bolos caseiros.",
                followersCount = 890,
                publishedRecipes = recipes.takeLast(1)
            )
        )
    }

    // --- Seletor Prévio (Receitas vs. Utilizadores) ---
    fun selectSearchTarget(target: SearchTarget) {
        _uiState.update { it.copy(searchTarget = target) }
        recomputeResults()
    }

    // --- Barra de Pesquisa (Lupa) ---
    fun activateSearchBar(active: Boolean) {
        _uiState.update { it.copy(isSearchActive = active) }
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery, isSearchActive = true) }
        recomputeResults()
    }

    fun clearQuery() {
        _uiState.update { it.copy(query = "") }
        recomputeResults()
    }

    fun submitSearch(queryText: String = _uiState.value.query) {
        val clean = queryText.trim()
        if (clean.isBlank()) return

        val currentTarget = _uiState.value.searchTarget
        val newEntry = RecentSearchItem(query = clean, target = currentTarget)

        _uiState.update { state ->
            val updatedHistory = (listOf(newEntry) + state.recentSearches.filterNot {
                it.query.equals(clean, ignoreCase = true) && it.target == currentTarget
            }).take(8)

            state.copy(
                query = clean,
                recentSearches = updatedHistory
            )
        }
        recomputeResults()
    }

    // --- Histórico de Pesquisas (Recentes) ---
    fun applyRecentSearch(item: RecentSearchItem) {
        _uiState.update {
            it.copy(
                searchTarget = item.target,
                query = item.query,
                isSearchActive = true
            )
        }
        submitSearch(item.query)
    }

    fun removeRecentSearch(item: RecentSearchItem) {
        _uiState.update { state ->
            state.copy(recentSearches = state.recentSearches - item)
        }
    }

    fun clearRecentSearches() {
        _uiState.update { it.copy(recentSearches = emptyList()) }
    }

    // --- Botão Inventário (Tick Box de Ingredientes) ---
    fun setInventoryOpen(open: Boolean) {
        _uiState.update { it.copy(isInventoryOpen = open) }
    }

    fun toggleInventoryIngredient(ingredient: String) {
        _uiState.update { state ->
            val current = state.selectedInventoryIngredients
            val existingMatch = current.firstOrNull { it.equals(ingredient, ignoreCase = true) }
            val updated = if (existingMatch != null) current - existingMatch else current + ingredient
            state.copy(
                searchTarget = SearchTarget.RECIPES, // Inventário aplica-se à pesquisa de receitas
                selectedInventoryIngredients = updated
            )
        }
        recomputeResults()
    }

    fun addCustomIngredientToInventory(ingredient: String) {
        val clean = ingredient.trim().replaceFirstChar { it.uppercase() }
        if (clean.isBlank()) return

        _uiState.update { state ->
            val updatedPantry = if (state.availableIngredients.none { it.equals(clean, ignoreCase = true) }) {
                (state.availableIngredients + clean).sorted()
            } else {
                state.availableIngredients
            }
            val updatedSelected = if (state.selectedInventoryIngredients.none { it.equals(clean, ignoreCase = true) }) {
                state.selectedInventoryIngredients + clean
            } else {
                state.selectedInventoryIngredients
            }
            state.copy(
                searchTarget = SearchTarget.RECIPES,
                availableIngredients = updatedPantry,
                selectedInventoryIngredients = updatedSelected
            )
        }
        recomputeResults()
    }

    fun clearInventory() {
        _uiState.update { it.copy(selectedInventoryIngredients = emptySet()) }
        recomputeResults()
    }

    // --- Botão Filtros (Duração, Dificuldade, Categoria, Ordenação) ---
    fun setFiltersOpen(open: Boolean) {
        _uiState.update { it.copy(isFiltersOpen = open) }
    }

    fun updateFilters(newFilters: SearchFilters) {
        _uiState.update {
            it.copy(
                searchTarget = SearchTarget.RECIPES,
                filters = newFilters
            )
        }
        recomputeResults()
    }

    fun resetFilters() {
        _uiState.update { it.copy(filters = SearchFilters()) }
        recomputeResults()
    }

    // --- Detalhes de Receita / Utilizador ---
    fun openRecipeDetail(item: SearchRecipeItem) {
        _uiState.update { it.copy(selectedRecipeForDetail = item) }
    }

    fun closeRecipeDetail() {
        _uiState.update { it.copy(selectedRecipeForDetail = null) }
    }

    fun openUserDetail(user: SearchableUser) {
        _uiState.update { it.copy(selectedUserForDetail = user) }
    }

    fun closeUserDetail() {
        _uiState.update { it.copy(selectedUserForDetail = null) }
    }

    // --- Motor de Filtragem e Ordenação ---
    private fun recomputeResults() {
        val state = _uiState.value
        val q = state.query.trim()

        // 1. Filtragem de Utilizadores
        val filteredUsers = if (q.isBlank()) {
            allRegisteredUsers
        } else {
            allRegisteredUsers.filter { user ->
                user.displayName.contains(q, ignoreCase = true) ||
                    user.username.contains(q, ignoreCase = true) ||
                    user.bio.contains(q, ignoreCase = true)
            }
        }

        // 2. Filtragem de Receitas (Texto + Inventário Inclusivo + Filtros Adicionais)
        val filteredRecipes = allEnrichedRecipes.filter { item ->
            val recipe = item.recipe

            // A) Texto escrito na barra de pesquisa (ex: "lasanha")
            val matchesQuery = q.isBlank() ||
                recipe.title.contains(q, ignoreCase = true) ||
                item.category.contains(q, ignoreCase = true) ||
                item.authorName.contains(q, ignoreCase = true) ||
                item.ingredientNames.any { it.contains(q, ignoreCase = true) }

            // B) Regra do Inventário (Tick Box):
            // A receita TEM de conter TODOS os ingredientes selecionados,
            // mas PODE conter outros ingredientes não selecionados (ex: frango, arroz e natas é válida).
            val matchesInventory = state.selectedInventoryIngredients.all { requiredIng ->
                item.ingredientNames.any { recipeIng ->
                    recipeIng.contains(requiredIng, ignoreCase = true)
                }
            }

            // C) Filtro de Duração Máxima
            val maxDur = state.filters.maxDurationMinutes
            val matchesDuration = maxDur == null || recipe.prepTimeMinutes <= maxDur

            // D) Filtro de Dificuldade
            val matchesDifficulty = state.filters.selectedDifficulties.isEmpty() ||
                state.filters.selectedDifficulties.any { diff ->
                    recipe.difficulty.toString().equals(diff, ignoreCase = true)
                }

            // E) Filtro de Categoria
            val matchesCategory = state.filters.selectedCategories.isEmpty() ||
                state.filters.selectedCategories.any { cat ->
                    item.category.equals(cat, ignoreCase = true)
                }

            matchesQuery && matchesInventory && matchesDuration && matchesDifficulty && matchesCategory
        }

        // 3. Ordenação
        val sortedRecipes = when (state.filters.sortBy) {
            RecipeSortOption.RELEVANCE -> filteredRecipes
            RecipeSortOption.HIGHEST_RATING -> filteredRecipes.sortedByDescending { it.rating }
            RecipeSortOption.MOST_LIKED -> filteredRecipes.sortedByDescending { it.likesCount }
            RecipeSortOption.SHORTEST_DURATION -> filteredRecipes.sortedBy { it.recipe.prepTimeMinutes }
            RecipeSortOption.NEWEST -> filteredRecipes.asReversed()
        }

        _uiState.update {
            it.copy(
                recipeResults = sortedRecipes,
                userResults = filteredUsers
            )
        }
    }
}