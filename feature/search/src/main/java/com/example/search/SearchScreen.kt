package com.example.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pesquisar • CookTok", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // 1. Seletor Prévio: Receitas vs. Utilizadores
            item {
                SearchTargetSelector(
                    selectedTarget = state.searchTarget,
                    onTargetSelected = { viewModel.selectSearchTarget(it) }
                )
            }

            // 2. Barra de Pesquisa (Lupa) + Botões Inventário e Filtros
            item {
                CookTokSearchBar(
                    query = state.query,
                    searchTarget = state.searchTarget,
                    inventoryCount = state.selectedInventoryIngredients.size,
                    filtersCount = state.filters.activeCount,
                    onQueryChange = { viewModel.onQueryChanged(it) },
                    onClearQuery = { viewModel.clearQuery() },
                    onSearchSubmit = { viewModel.submitSearch() },
                    onOpenInventoryClick = { viewModel.setInventoryOpen(true) },
                    onOpenFiltersClick = { viewModel.setFiltersOpen(true) }
                )
            }

            if (state.isLoading) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            // 3. Chips ativos do Inventário (quando há ingredientes marcados)
            if (state.searchTarget == SearchTarget.RECIPES && state.selectedInventoryIngredients.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "Ingredientes no Inventário (obrigatórios):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.selectedInventoryIngredients.toList()) { ing ->
                                InputChip(
                                    selected = true,
                                    onClick = { viewModel.toggleInventoryIngredient(ing) },
                                    label = { Text(ing) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remover $ing",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 4. Histórico de Pesquisas (Recentes)
            item {
                RecentSearchesSection(
                    recentSearches = state.recentSearches,
                    onSelectRecent = { viewModel.applyRecentSearch(it) },
                    onRemoveRecent = { viewModel.removeRecentSearch(it) },
                    onClearAll = { viewModel.clearRecentSearches() }
                )
            }

            // 5. Cabeçalho de Resultados
            item {
                val count = if (state.searchTarget == SearchTarget.RECIPES) {
                    state.recipeResults.size
                } else {
                    state.userResults.size
                }
                Text(
                    text = if (state.searchTarget == SearchTarget.RECIPES) {
                        "Receitas Encontradas ($count)"
                    } else {
                        "Utilizadores Encontrados ($count)"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 6. Lista de Resultados (Receitas ou Utilizadores)
            if (state.searchTarget == SearchTarget.RECIPES) {
                if (state.recipeResults.isEmpty()) {
                    item {
                        EmptySearchPlaceholder(
                            text = "Nenhuma receita corresponde à tua pesquisa ou aos ingredientes selecionados no inventário."
                        )
                    }
                } else {
                    items(state.recipeResults, key = { it.recipe.id }) { item ->
                        SearchRecipeCard(
                            item = item,
                            selectedInventory = state.selectedInventoryIngredients,
                            onClick = { viewModel.openRecipeDetail(item) }
                        )
                    }
                }
            } else {
                if (state.userResults.isEmpty()) {
                    item {
                        EmptySearchPlaceholder(
                            text = "Nenhum utilizador encontrado com \"${state.query}\"."
                        )
                    }
                } else {
                    items(state.userResults, key = { it.id }) { user ->
                        SearchUserCard(
                            user = user,
                            onClick = { viewModel.openUserDetail(user) }
                        )
                    }
                }
            }
        }
    }

    // --- Modais (Bottom Sheets) ---
    if (state.isInventoryOpen) {
        InventoryBottomSheet(
            availableIngredients = state.availableIngredients,
            selectedIngredients = state.selectedInventoryIngredients,
            onToggleIngredient = { viewModel.toggleInventoryIngredient(it) },
            onAddCustomIngredient = { viewModel.addCustomIngredientToInventory(it) },
            onClearInventory = { viewModel.clearInventory() },
            onDismiss = { viewModel.setInventoryOpen(false) }
        )
    }

    if (state.isFiltersOpen) {
        FiltersBottomSheet(
            currentFilters = state.filters,
            onApplyFilters = { viewModel.updateFilters(it) },
            onResetFilters = { viewModel.resetFilters() },
            onDismiss = { viewModel.setFiltersOpen(false) }
        )
    }

    state.selectedRecipeForDetail?.let { recipeItem ->
        SearchedRecipeDetailSheet(
            item = recipeItem,
            selectedInventory = state.selectedInventoryIngredients,
            onDismiss = { viewModel.closeRecipeDetail() }
        )
    }

    state.selectedUserForDetail?.let { user ->
        SearchedUserDetailSheet(
            user = user,
            onDismiss = { viewModel.closeUserDetail() }
        )
    }
}

@Composable
private fun EmptySearchPlaceholder(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp)
        )
    }
}