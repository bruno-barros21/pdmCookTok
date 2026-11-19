package com.example.profile

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
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeRecipesAndInitializeProfile()
    }

    private fun observeRecipesAndInitializeProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            recipeRepository.getRecipes().collect { allRecipes ->
                val currentUserId = _uiState.value.user.id
                val mine = allRecipes.filter { it.authorId == currentUserId }
                val favorites = allRecipes.filter { it.isFavorite }

                // Distribui automaticamente favoritos iniciais pelas listas de exemplo se estiverem vazias
                val updatedLists = _uiState.value.customFavoriteLists.mapValues { (listName, ids) ->
                    if (ids.isEmpty() && favorites.isNotEmpty()) {
                        favorites.filter { recipe ->
                            when (listName.lowercase()) {
                                "lasanhas" -> recipe.title.contains("lasanha", ignoreCase = true)
                                "sobremesas" -> recipe.title.contains("doce", ignoreCase = true) ||
                                    recipe.title.contains("bolo", ignoreCase = true) ||
                                    recipe.title.contains("mousse", ignoreCase = true)
                                else -> false
                            }
                        }.map { it.id }.toSet()
                    } else {
                        ids
                    }
                }

                // Constrói a lista de amigos associando receitas de outros autores disponíveis no feed
                val sampleFriends = buildFriendsList(allRecipes, currentUserId)

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        createdRecipes = mine,
                        allFavoriteRecipes = favorites,
                        customFavoriteLists = updatedLists,
                        friends = sampleFriends,
                        stats = buildCreatorStats(mine)
                    )
                }
            }
        }
    }

    private fun buildFriendsList(allRecipes: List<Recipe>, currentUserId: String): List<FriendProfile> {
        val otherRecipes = allRecipes.filter { it.authorId != currentUserId }
        return listOf(
            FriendProfile(
                id = "user_2",
                displayName = "Mariana Silva",
                username = "mariana_cooks",
                bio = "Especialista em massas frescas e comida mediterrânica.",
                followersCount = 512,
                followingCount = 140,
                publishedRecipes = otherRecipes.take(2)
            ),
            FriendProfile(
                id = "user_3",
                displayName = "Tiago Mendes",
                username = "chef_tiago",
                bio = "Grelhados, petiscos tradicionais e sobremesas fáceis.",
                followersCount = 329,
                followingCount = 210,
                publishedRecipes = otherRecipes.drop(2).take(2)
            ),
            FriendProfile(
                id = "user_4",
                displayName = "Inês Costa",
                username = "ines_pastelaria",
                bio = "Doces conventuais e bolos caseiros todos os fins de semana.",
                followersCount = 890,
                followingCount = 175,
                publishedRecipes = otherRecipes.takeLast(1)
            )
        )
    }

    private fun buildCreatorStats(createdRecipes: List<Recipe>): CreatorStats {
        val multiplier = createdRecipes.size.coerceAtLeast(1)
        val histogram = mapOf(
            5 to 145 * multiplier,
            4 to 68 * multiplier,
            3 to 19 * multiplier,
            2 to 6 * multiplier,
            1 to 3 * multiplier,
            0 to 1 * multiplier
        )
        val totalVotes = histogram.values.sum().coerceAtLeast(1)
        val weightedSum = histogram.entries.sumOf { (stars, count) -> stars * count }
        val avg = weightedSum.toFloat() / totalVotes.toFloat()

        return CreatorStats(
            totalViews = 1280 * multiplier,
            totalLikes = 342 * multiplier,
            totalComments = 76 * multiplier,
            averageRating = avg,
            ratingHistogram = histogram
        )
    }

    // --- Navegação entre Tabs ---
    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    // --- Edição de Perfil (Lápis: Nome, @username e Sobre mim) ---
    fun openEditProfile() {
        _uiState.update { it.copy(isEditingProfile = true) }
    }

    fun closeEditProfile() {
        _uiState.update { it.copy(isEditingProfile = false) }
    }

    fun saveProfileChanges(newDisplayName: String, newUsername: String, newBio: String) {
        val cleanName = newDisplayName.trim()
        val cleanUsername = newUsername.trim().removePrefix("@")

        if (cleanName.isBlank() || cleanUsername.isBlank()) {
            _uiState.update { it.copy(userMessage = "O nome e o @username não podem ficar vazios.") }
            return
        }

        _uiState.update { state ->
            state.copy(
                user = state.user.copy(
                    displayName = cleanName,
                    username = cleanUsername,
                    bio = newBio.trim()
                ),
                isEditingProfile = false,
                userMessage = "Perfil atualizado com sucesso!"
            )
        }
        // Quando o endpoint de edição estiver ligado no RecipeRepository/UserRepository:
        // viewModelScope.launch { userRepository.updateUserProfile(cleanName, cleanUsername, newBio) }
    }

    // --- Listas de Favoritos Personalizadas ---
    fun selectFavoriteList(listName: String) {
        _uiState.update { it.copy(selectedFavoriteListName = listName) }
    }

    fun createFavoriteList(listName: String) {
        val trimmed = listName.trim()
        if (trimmed.isBlank() || trimmed.equals("Todas", ignoreCase = true)) {
            _uiState.update { it.copy(userMessage = "Escolhe um nome válido para a lista.") }
            return
        }
        if (_uiState.value.customFavoriteLists.containsKey(trimmed)) {
            _uiState.update { it.copy(userMessage = "Já existe uma lista chamada \"$trimmed\".") }
            return
        }

        _uiState.update { state ->
            state.copy(
                customFavoriteLists = state.customFavoriteLists + (trimmed to emptySet()),
                selectedFavoriteListName = trimmed,
                userMessage = "Lista \"$trimmed\" criada!"
            )
        }
    }

    fun deleteFavoriteList(listName: String) {
        _uiState.update { state ->
            val updated = state.customFavoriteLists - listName
            val nextSelected = if (state.selectedFavoriteListName == listName) "Todas" else state.selectedFavoriteListName
            state.copy(
                customFavoriteLists = updated,
                selectedFavoriteListName = nextSelected,
                userMessage = "Lista \"$listName\" removida."
            )
        }
    }

    fun openListAssignmentDialog(recipe: Recipe) {
        _uiState.update { it.copy(recipeForListAssignment = recipe) }
    }

    fun closeListAssignmentDialog() {
        _uiState.update { it.copy(recipeForListAssignment = null) }
    }

    fun toggleRecipeInCustomList(recipeId: String, listName: String) {
        _uiState.update { state ->
            val currentSet = state.customFavoriteLists[listName] ?: emptySet()
            val updatedSet = if (recipeId in currentSet) currentSet - recipeId else currentSet + recipeId
            state.copy(
                customFavoriteLists = state.customFavoriteLists + (listName to updatedSet)
            )
        }
    }

    // --- Amigos: Visualizar Perfil do Colega (Ícone "Olho") ---
    fun openFriendProfile(friend: FriendProfile) {
        _uiState.update { it.copy(inspectedFriend = friend) }
    }

    fun closeFriendProfile() {
        _uiState.update { it.copy(inspectedFriend = null) }
    }

    // --- Partilha Direta de Receitas com Amigos ---
    fun startSharingRecipe(recipe: Recipe) {
        _uiState.update { it.copy(recipeToShare = recipe, friendToShareWith = null) }
    }

    fun startSharingWithFriend(friend: FriendProfile) {
        _uiState.update { it.copy(friendToShareWith = friend, recipeToShare = null) }
    }

    fun cancelSharing() {
        _uiState.update { it.copy(recipeToShare = null, friendToShareWith = null) }
    }

    fun confirmShareRecipeWithFriend(recipe: Recipe, friend: FriendProfile, message: String) {
        val record = SharedRecipeRecord(
            id = UUID.randomUUID().toString(),
            recipeTitle = recipe.title,
            friendName = friend.displayName,
            friendUsername = friend.username,
            note = message.trim()
        )
        _uiState.update { state ->
            state.copy(
                recipeToShare = null,
                friendToShareWith = null,
                sharedHistory = listOf(record) + state.sharedHistory,
                userMessage = "Receita \"${recipe.title}\" partilhada com @${friend.username}!"
            )
        }
    }

    // --- Modo Visitante (Sem Conta) vs. Conta Registada ---
    fun setGuestMode(isGuest: Boolean) {
        _uiState.update { it.copy(isGuest = isGuest) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}