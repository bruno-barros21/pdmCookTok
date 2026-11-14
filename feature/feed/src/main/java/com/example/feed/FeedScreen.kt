package com.example.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.Recipe

@Composable
fun FeedRoute(
    viewModel: FeedViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    FeedScreen(
        uiState = uiState,
        onFilterSelected = viewModel::onFilterChange,
        onLikeClick = viewModel::onToggleLike,
        onFavoriteClick = viewModel::onToggleFavorite,
        onRetryClick = { viewModel.refresh() },
        modifier = modifier
    )
}

@Composable
fun FeedScreen(
    uiState: FeedUiState,
    onFilterSelected: (String) -> Unit,
    onLikeClick: (String) -> Unit,
    onFavoriteClick: (Recipe) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "CookTok Feed",
            style = MaterialTheme.typography.headlineMedium
        )

        // Filtros do Feed (Recentes, Populares, Melhor Rating)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            FilterChip(
                selected = uiState.selectedFilter == "recent",
                onClick = { onFilterSelected("recent") },
                label = { Text("Recentes") }
            )
            FilterChip(
                selected = uiState.selectedFilter == "popular",
                onClick = { onFilterSelected("popular") },
                label = { Text("Populares") }
            )
            FilterChip(
                selected = uiState.selectedFilter == "rating",
                onClick = { onFilterSelected("rating") },
                label = { Text("Top Rating") }
            )
        }

        if (uiState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // Banner de Graceful Degradation (Modo Offline)
        if (uiState.isOfflineError) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = "Modo Offline: a mostrar receitas guardadas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    AssistChip(
                        onClick = onRetryClick,
                        label = { Text("Tentar") }
                    )
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(uiState.recipes, key = { it.id }) { recipe ->
                RecipeFeedCard(
                    recipe = recipe,
                    onLikeClick = { onLikeClick(recipe.id) },
                    onFavoriteClick = { onFavoriteClick(recipe) }
                )
            }
        }
    }
}

@Composable
fun RecipeFeedCard(
    recipe: Recipe,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = recipe.title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Por ${recipe.authorName} · ${recipe.durationMinutes} min · ${recipe.difficulty}",
                style = MaterialTheme.typography.labelMedium
            )
            recipe.locationName?.let { loc ->
                Text(text = "📍 $loc", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = recipe.description, style = MaterialTheme.typography.bodyMedium)

            if (recipe.ingredients.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ingredientes: " + recipe.ingredients.joinToString { "${it.name} (${it.quantity}${it.unit})" },
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onLikeClick) {
                    Text(if (recipe.isLiked) "♥ ${recipe.likesCount}" else "♡ ${recipe.likesCount}")
                }
                OutlinedButton(onClick = onFavoriteClick) {
                    Text(if (recipe.isFavorite) "★ Guardado" else "☆ Guardar Offline")
                }
            }
        }
    }
}