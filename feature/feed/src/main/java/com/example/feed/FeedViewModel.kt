package com.example.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.RecipeRepository
import com.example.model.Recipe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val recipes: List<Recipe> = emptyList(),
    val selectedFilter: String = "recent",
    val isLoading: Boolean = false,
    val isOfflineError: Boolean = false
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: RecipeRepository
) : ViewModel() {

    private val selectedFilter = MutableStateFlow("recent")
    private val isLoading = MutableStateFlow(false)
    private val isOfflineError = MutableStateFlow(false)

    val uiState: StateFlow<FeedUiState> = combine(
        repository.observeFeed(),
        selectedFilter,
        isLoading,
        isOfflineError
    ) { recipes, filter, loading, offline ->
        val sorted = when (filter) {
            "popular" -> recipes.sortedByDescending { it.likesCount }
            "rating" -> recipes.sortedByDescending { it.averageRating }
            else -> recipes.sortedByDescending { it.createdAt }
        }
        FeedUiState(
            recipes = sorted,
            selectedFilter = filter,
            isLoading = loading,
            isOfflineError = offline
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FeedUiState(isLoading = true)
    )

    init {
        refresh("recent")
    }

    fun onFilterChange(filter: String) {
        selectedFilter.value = filter
        refresh(filter)
    }

    fun refresh(filter: String = selectedFilter.value) {
        viewModelScope.launch {
            isLoading.value = true
            val result = repository.refreshFeed(filter)
            isOfflineError.value = result.isFailure
            isLoading.value = false
        }
    }

    fun onToggleLike(recipeId: String) {
        viewModelScope.launch {
            repository.toggleLike(recipeId)
        }
    }

    fun onToggleFavorite(recipe: Recipe) {
        viewModelScope.launch {
            repository.toggleFavorite(recipe.id)
        }
    }
}