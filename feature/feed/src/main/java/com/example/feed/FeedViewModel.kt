package com.example.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.RecipeRepository
import com.example.model.Recipe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    /** Cache em memória das receitas enriquecidas para preservar likes/ratings/comentários ao filtrar */
    private var allFeedItems: List<FeedRecipeItem> = emptyList()

    init {
        observeRecipesOfflineFirst()
    }

    private fun observeRecipesOfflineFirst() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            recipeRepository.getRecipes()
                .catch {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isOfflineFallback = true,
                            userMessage = "Modo offline ativo: a mostrar dados locais disponíveis."
                        )
                    }
                }
                .collect { recipes ->
                    val previousById = allFeedItems.associateBy { it.recipe.id }
                    allFeedItems = recipes.map { recipe ->
                        val existing = previousById[recipe.id]
                        if (existing != null) {
                            existing.copy(recipe = recipe)
                        } else {
                            enrichRecipeForFeed(recipe)
                        }
                    }

                    val friendsList = buildFriendsList(recipes)
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            friends = friendsList
                        )
                    }
                    recomputeFeedOrderAndFilters()
                }
        }
    }

    private fun enrichRecipeForFeed(recipe: Recipe): FeedRecipeItem {
        val hash = abs(recipe.id.hashCode())
        val (authorDisplay, authorUser) = when (recipe.authorId) {
            "user_1" -> "Bruno Barros" to "Chef_Bruno"
            "user_2" -> "Mariana Silva" to "mariana_cooks"
            "user_3" -> "Tiago Mendes" to "chef_tiago"
            "user_4" -> "Inês Costa" to "ines_pastelaria"
            else -> "Chef CookTok" to recipe.authorId
        }

        val category = inferCategory(recipe.title, recipe.prepTimeMinutes)
        val rawIngredients = recipe.ingredients.map { it.name.trim() }.filter { it.isNotBlank() }
        val ingredients = rawIngredients.ifEmpty { inferIngredientsFromTitle(recipe.title) }

        val steps = listOf(
            "Prepara e mede todos os ingredientes principais (${ingredients.take(3).joinToString(", ")}).",
            "Cozinha em lume médio durante cerca de ${(recipe.prepTimeMinutes / 2).coerceAtLeast(5)} minutos até apurar bem os sabores.",
            "Finaliza o empratamento, ajusta temperos a gosto e serve de imediato."
        )

        val rating = (3.7f + ((hash % 14) * 0.1f)).coerceAtMost(5.0f)
        val ratingsCount = 18 + (hash % 95)
        val likes = 65 + (hash % 420)
        val views = 600 + (hash % 4800)

        val initialComments = listOf(
            FeedComment(
                authorUsername = "mariana_cooks",
                text = "Ficou incrível cá em casa! Recomendo muito experimentar.",
                timestampLabel = "há 2h"
            ),
            FeedComment(
                authorUsername = "chef_tiago",
                text = "Excelente ponto de cozedura e super fácil de acompanhar.",
                timestampLabel = "há 5h"
            )
        )

        return FeedRecipeItem(
            recipe = recipe,
            authorDisplayName = authorDisplay,
            authorUsername = authorUser,
            category = category,
            description = "Receita prática de ${recipe.title} pronta em ${recipe.prepTimeMinutes} minutos.",
            servings = 2 + (hash % 4),
            ingredientNames = ingredients,
            preparationSteps = steps,
            viewsCount = views,
            likesCount = likes,
            isFavoriteByMe = recipe.isFavorite,
            averageRating = rating,
            ratingsCount = ratingsCount,
            comments = initialComments
        )
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

    private fun inferIngredientsFromTitle(title: String): List<String> {
        val lower = title.lowercase()
        val detected = mutableListOf<String>()
        if ("frango" in lower) detected += "Frango"
        if ("arroz" in lower) detected += "Arroz"
        if ("lasanha" in lower) detected += listOf("Massa", "Carne Picada", "Queijo", "Tomate")
        if ("bolo" in lower || "mousse" in lower || "doce" in lower) {
            detected += listOf("Açúcar", "Ovo", "Chocolate", "Farinha")
        }
        if ("bacalhau" in lower) detected += listOf("Bacalhau", "Batata", "Azeite", "Cebola")
        return detected.ifEmpty { listOf("Azeite", "Alho", "Cebola", "Sal q.b.") }
    }

    private fun buildFriendsList(recipes: List<Recipe>): List<FeedFriend> {
        return listOf(
            FeedFriend(
                id = "user_2",
                displayName = "Mariana Silva",
                username = "mariana_cooks",
                bio = "Especialista em massas frescas, lasanhas e cozinha mediterrânica.",
                followersCount = 512,
                followingCount = 140,
                publishedRecipes = recipes.take(2)
            ),
            FeedFriend(
                id = "user_3",
                displayName = "Tiago Mendes",
                username = "chef_tiago",
                bio = "Grelhados, petiscos tradicionais e pratos rápidos do dia a dia.",
                followersCount = 329,
                followingCount = 210,
                publishedRecipes = recipes.drop(2).take(2)
            ),
            FeedFriend(
                id = "user_4",
                displayName = "Inês Costa",
                username = "ines_pastelaria",
                bio = "Doces conventuais, sobremesas fáceis e bolos caseiros.",
                followersCount = 890,
                followingCount = 175,
                publishedRecipes = recipes.takeLast(1)
            )
        )
    }

    // --- Algoritmo do CookTok + Filtros por Rating e Categoria ---
    private fun recomputeFeedOrderAndFilters() {
        val state = _uiState.value
        val filters = state.filterState

        val filtered = allFeedItems.filter { item ->
            val matchesRating = filters.minRating == null || item.averageRating >= filters.minRating
            val matchesCategory = filters.selectedCategory == null ||
                item.category.equals(filters.selectedCategory, ignoreCase = true)
            matchesRating && matchesCategory
        }

        val sorted = when (filters.sortMode) {
            FeedSortMode.COOKTOK_ALGORITHM -> {
                if (state.isGuest) {
                    // Regra obrigatória para Visitantes: CookToks com maiores ratings e likes
                    filtered.sortedByDescending { (it.averageRating * 100f) + (it.likesCount * 0.5f) }
                } else {
                    // Utilizadores registados: interesses/categorias + autores seguidos + rating
                    filtered.sortedByDescending { item ->
                        val interestBonus = if (item.category in state.userPreferredCategories) 120f else 0f
                        val followingBonus = if (item.recipe.authorId in state.followedAuthorIds) 90f else 0f
                        interestBonus + followingBonus + (item.averageRating * 40f) + (item.likesCount * 0.2f)
                    }
                }
            }
            FeedSortMode.HIGHEST_RATING -> filtered.sortedByDescending { it.averageRating }
            FeedSortMode.MOST_LIKED -> filtered.sortedByDescending { it.likesCount }
            FeedSortMode.NEWEST -> filtered.asReversed()
        }

        // Mantém sincronizados os modais eventualmente abertos
        val refreshedDetail = state.selectedRecipeForDetails?.let { open ->
            sorted.firstOrNull { it.recipe.id == open.recipe.id } ?: open
        }
        val refreshedComments = state.selectedRecipeForComments?.let { open ->
            sorted.firstOrNull { it.recipe.id == open.recipe.id } ?: open
        }
        val refreshedRating = state.selectedRecipeForRating?.let { open ->
            sorted.firstOrNull { it.recipe.id == open.recipe.id } ?: open
        }

        _uiState.update {
            it.copy(
                feedItems = sorted,
                selectedRecipeForDetails = refreshedDetail,
                selectedRecipeForComments = refreshedComments,
                selectedRecipeForRating = refreshedRating
            )
        }
    }

    // --- Botão Seguir Dinâmico ---
    fun toggleFollowAuthor(authorId: String, authorUsername: String) {
        if (_uiState.value.isGuest) {
            notifyGuestRestriction("seguir @$authorUsername")
            return
        }

        _uiState.update { state ->
            val alreadyFollowing = authorId in state.followedAuthorIds
            val updated = if (alreadyFollowing) {
                state.followedAuthorIds - authorId
            } else {
                state.followedAuthorIds + authorId
            }
            state.copy(
                followedAuthorIds = updated,
                userMessage = if (alreadyFollowing) {
                    "Deixaste de seguir @$authorUsername."
                } else {
                    "Agora segues @$authorUsername!"
                }
            )
        }
        recomputeFeedOrderAndFilters()
    }

    // --- Like (Bloqueado para Visitantes) ---
    fun toggleLike(recipeId: String) {
        if (_uiState.value.isGuest) {
            notifyGuestRestriction("dar gosto nas receitas")
            return
        }

        allFeedItems = allFeedItems.map { item ->
            if (item.recipe.id == recipeId) {
                val nowLiked = !item.isLikedByMe
                item.copy(
                    isLikedByMe = nowLiked,
                    likesCount = if (nowLiked) item.likesCount + 1 else (item.likesCount - 1).coerceAtLeast(0)
                )
            } else {
                item
            }
        }
        recomputeFeedOrderAndFilters()
    }

    // --- Favorito (Bloqueado para Visitantes) ---
    fun toggleFavorite(recipeId: String) {
        if (_uiState.value.isGuest) {
            notifyGuestRestriction("guardar receitas nos favoritos")
            return
        }

        var message = ""
        allFeedItems = allFeedItems.map { item ->
            if (item.recipe.id == recipeId) {
                val nowFav = !item.isFavoriteByMe
                message = if (nowFav) {
                    "\"${item.recipe.title}\" guardada nos Favoritos!"
                } else {
                    "\"${item.recipe.title}\" removida dos Favoritos."
                }
                item.copy(isFavoriteByMe = nowFav)
            } else {
                item
            }
        }
        _uiState.update { it.copy(userMessage = message) }
        recomputeFeedOrderAndFilters()
    }

    // --- Avaliação 0 a 5 Estrelas (Bloqueado para Visitantes) ---
    fun openRatingDialog(item: FeedRecipeItem) {
        if (_uiState.value.isGuest) {
            notifyGuestRestriction("avaliar receitas com estrelas")
            return
        }
        _uiState.update { it.copy(selectedRecipeForRating = item) }
    }

    fun closeRatingDialog() {
        _uiState.update { it.copy(selectedRecipeForRating = null) }
    }

    fun submitStarRating(recipeId: String, stars: Int) {
        if (_uiState.value.isGuest) {
            notifyGuestRestriction("avaliar receitas")
            return
        }
        val clamped = stars.coerceIn(0, 5)
        allFeedItems = allFeedItems.map { item ->
            if (item.recipe.id == recipeId) {
                val previous = item.myStarRating
                val currentTotalScore = item.averageRating * item.ratingsCount
                val (newCount, newAverage) = if (previous == null) {
                    val c = item.ratingsCount + 1
                    c to ((currentTotalScore + clamped) / c)
                } else {
                    val c = item.ratingsCount.coerceAtLeast(1)
                    c to ((currentTotalScore - previous + clamped) / c)
                }
                item.copy(
                    myStarRating = clamped,
                    ratingsCount = newCount,
                    averageRating = newAverage.coerceIn(0f, 5f)
                )
            } else {
                item
            }
        }
        _uiState.update {
            it.copy(
                selectedRecipeForRating = null,
                userMessage = "Avaliação de $clamped ★ registada!"
            )
        }
        recomputeFeedOrderAndFilters()
    }

    // --- Comentários (Todos veem; apenas registados podem publicar) ---
    fun openCommentsSheet(item: FeedRecipeItem) {
        _uiState.update { it.copy(selectedRecipeForComments = item) }
    }

    fun closeCommentsSheet() {
        _uiState.update { it.copy(selectedRecipeForComments = null) }
    }

    fun addComment(recipeId: String, commentText: String) {
        if (_uiState.value.isGuest) {
            notifyGuestRestriction("escrever comentários")
            return
        }
        val clean = commentText.trim()
        if (clean.isBlank()) return

        val newComment = FeedComment(
            authorUsername = "Chef_Bruno",
            text = clean,
            timestampLabel = "agora"
        )
        allFeedItems = allFeedItems.map { item ->
            if (item.recipe.id == recipeId) {
                item.copy(comments = listOf(newComment) + item.comments)
            } else {
                item
            }
        }
        recomputeFeedOrderAndFilters()
    }

    // --- Caixa de Detalhes da Receita ("Ver Receita") ---
    fun openRecipeDetails(item: FeedRecipeItem) {
        _uiState.update { it.copy(selectedRecipeForDetails = item) }
    }

    fun closeRecipeDetails() {
        _uiState.update { it.copy(selectedRecipeForDetails = null) }
    }

    // --- Filtros por Rating / Categoria / Ordenação ---
    fun setFilterSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isFilterSheetOpen = open) }
    }

    fun applyFilters(newFilterState: FeedFilterState) {
        _uiState.update {
            it.copy(
                filterState = newFilterState,
                isFilterSheetOpen = false
            )
        }
        recomputeFeedOrderAndFilters()
    }

    fun resetFilters() {
        _uiState.update {
            it.copy(
                filterState = FeedFilterState(),
                isFilterSheetOpen = false
            )
        }
        recomputeFeedOrderAndFilters()
    }

    // --- Botão Amigos e Visualização de Perfil (Ícone "Olho") ---
    fun setFriendsSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isFriendsSheetOpen = open) }
    }

    fun inspectFriendProfile(friend: FeedFriend) {
        _uiState.update {
            it.copy(
                isFriendsSheetOpen = false,
                inspectedFriend = friend
            )
        }
    }

    fun closeInspectedFriendProfile() {
        _uiState.update { it.copy(inspectedFriend = null) }
    }

    // --- Partilha Direta de Receita com Amigo ---
    fun openShareDialog(item: FeedRecipeItem) {
        if (_uiState.value.isGuest) {
            notifyGuestRestriction("partilhar receitas com amigos")
            return
        }
        _uiState.update { it.copy(selectedRecipeForShare = item) }
    }

    fun closeShareDialog() {
        _uiState.update { it.copy(selectedRecipeForShare = null) }
    }

    fun confirmShareWithFriend(item: FeedRecipeItem, friend: FeedFriend) {
        _uiState.update {
            it.copy(
                selectedRecipeForShare = null,
                userMessage = "Receita \"${item.recipe.title}\" partilhada com @${friend.username}!"
            )
        }
    }

    // --- Alternador de Modo Visitante vs. Conta Registada ---
    fun setGuestMode(isGuest: Boolean) {
        _uiState.update {
            it.copy(
                isGuest = isGuest,
                userMessage = if (isGuest) {
                    "Modo Visitante ativo: a exibir CookToks com maiores ratings e likes."
                } else {
                    "Sessão iniciada: Algoritmo personalizado Para Ti ativo."
                }
            )
        }
        recomputeFeedOrderAndFilters()
    }

    private fun notifyGuestRestriction(actionLabel: String) {
        _uiState.update {
            it.copy(userMessage = "Cria conta ou inicia sessão para $actionLabel.")
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}