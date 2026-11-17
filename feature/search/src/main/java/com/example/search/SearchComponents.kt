package com.example.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Seletor prévio explícito entre "Receitas" e "Utilizadores" antes de escrever.
 */
@Composable
fun SearchTargetSelector(
    selectedTarget: SearchTarget,
    onTargetSelected: (SearchTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SearchTarget.entries.forEach { target ->
            val isSelected = selectedTarget == target
            FilterChip(
                selected = isSelected,
                onClick = { onTargetSelected(target) },
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    Icon(
                        imageVector = if (target == SearchTarget.RECIPES) {
                            Icons.Default.RestaurantMenu
                        } else {
                            Icons.Default.Person
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                label = {
                    Text(
                        text = target.label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

/**
 * Barra de Pesquisa com botão Lupa + botões de ação rápida para Inventário e Filtros.
 */
@Composable
fun CookTokSearchBar(
    query: String,
    searchTarget: SearchTarget,
    inventoryCount: Int,
    filtersCount: Int,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSearchSubmit: () -> Unit,
    onOpenInventoryClick: () -> Unit,
    onOpenFiltersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            placeholder = {
                Text(
                    text = if (searchTarget == SearchTarget.RECIPES) {
                        "Pesquisar receitas (ex: lasanha, frango...)"
                    } else {
                        "Pesquisar utilizadores (@username ou nome)..."
                    }
                )
            },
            leadingIcon = {
                IconButton(onClick = {
                    onSearchSubmit()
                    focusManager.clearFocus()
                }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Botão Lupa"
                    )
                }
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClearQuery) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Limpar Pesquisa"
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearchSubmit()
                    focusManager.clearFocus()
                }
            )
        )

        // Botões de Inventário (Tick Box) e Filtros (Duração, Dificuldade, etc.)
        if (searchTarget == SearchTarget.RECIPES) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onOpenInventoryClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (inventoryCount > 0) "Inventário ($inventoryCount)" else "Inventário",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = onOpenFiltersClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (filtersCount > 0) "Filtros ($filtersCount)" else "Filtros",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Secção "Recentes": histórico de pesquisas efetuadas anteriormente.
 */
@Composable
fun RecentSearchesSection(
    recentSearches: List<RecentSearchItem>,
    onSelectRecent: (RecentSearchItem) -> Unit,
    onRemoveRecent: (RecentSearchItem) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (recentSearches.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recentes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onClearAll) {
                Text("Limpar histórico", style = MaterialTheme.typography.labelMedium)
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(recentSearches) { item ->
                InputChip(
                    selected = false,
                    onClick = { onSelectRecent(item) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text("${item.query} • ${item.target.label}")
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remover pesquisa recente",
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onRemoveRecent(item) }
                        )
                    }
                )
            }
        }
    }
}

/**
 * Modal do Botão Inventário:
 * Tick boxes de ingredientes com lógica inclusiva (contém os selecionados + permite extras).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryBottomSheet(
    availableIngredients: List<String>,
    selectedIngredients: Set<String>,
    onToggleIngredient: (String) -> Unit,
    onAddCustomIngredient: (String) -> Unit,
    onClearInventory: () -> Unit,
    onDismiss: () -> Unit
) {
    var pantryFilter by remember { mutableStateOf("") }
    var customIngredientInput by remember { mutableStateOf("") }

    val filteredPantry = remember(availableIngredients, pantryFilter) {
        if (pantryFilter.isBlank()) {
            availableIngredients
        } else {
            availableIngredients.filter { it.contains(pantryFilter, ignoreCase = true) }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Inventário de Ingredientes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Mostra receitas que contêm todos os ingredientes marcados (mesmo que levem outros extra).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (selectedIngredients.isNotEmpty()) {
                    TextButton(onClick = onClearInventory) {
                        Text("Limpar (${selectedIngredients.size})")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pesquisa rápida dentro da lista de ingredientes
            OutlinedTextField(
                value = pantryFilter,
                onValueChange = { pantryFilter = it },
                label = { Text("Filtrar ingredientes na lista...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Adicionar ingrediente personalizado que não esteja na lista
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customIngredientInput,
                    onValueChange = { customIngredientInput = it },
                    label = { Text("Outro ingrediente (ex: Cogumelos)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalIconButton(
                    onClick = {
                        if (customIngredientInput.isNotBlank()) {
                            onAddCustomIngredient(customIngredientInput)
                            customIngredientInput = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar Ingrediente")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Lista com Tick Boxes (Checkbox)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
            ) {
                items(filteredPantry) { ingredient ->
                    val isChecked = selectedIngredients.any { it.equals(ingredient, ignoreCase = true) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleIngredient(ingredient) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onToggleIngredient(ingredient) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = ingredient,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver Receitas Compatíveis")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Modal do Botão Filtros: Duração máxima, Dificuldade, Categoria e Ordenação.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersBottomSheet(
    currentFilters: SearchFilters,
    onApplyFilters: (SearchFilters) -> Unit,
    onResetFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    var draftDuration by remember(currentFilters) { mutableStateOf(currentFilters.maxDurationMinutes) }
    var draftDifficulties by remember(currentFilters) { mutableStateOf(currentFilters.selectedDifficulties) }
    var draftCategories by remember(currentFilters) { mutableStateOf(currentFilters.selectedCategories) }
    var draftSort by remember(currentFilters) { mutableStateOf(currentFilters.sortBy) }

    val difficulties = listOf("Fácil", "Média", "Difícil")
    val categories = listOf("Prato Principal", "Massas", "Sobremesas", "Rápidas & Snacks", "Tradicional")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtros & Ordenação",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        onResetFilters()
                        onDismiss()
                    }) {
                        Text("Repor")
                    }
                }
            }

            // 1. Duração da Receita
            item {
                Column {
                    Text(
                        text = if (draftDuration == null) {
                            "Duração Máxima: Qualquer"
                        } else {
                            "Duração Máxima: Até $draftDuration min"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val presets = listOf(null to "Qualquer", 15 to "≤ 15m", 30 to "≤ 30m", 45 to "≤ 45m", 60 to "≤ 60m")
                        items(presets) { (minutes, label) ->
                            FilterChip(
                                selected = draftDuration == minutes,
                                onClick = { draftDuration = minutes },
                                label = { Text(label) }
                            )
                        }
                    }
                    Slider(
                        value = (draftDuration ?: 120).toFloat(),
                        onValueChange = { value ->
                            val rounded = (value / 5f).roundToInt() * 5
                            draftDuration = if (rounded >= 120) null else rounded
                        },
                        valueRange = 10f..120f
                    )
                }
            }

            // 2. Dificuldade (Tick boxes)
            item {
                Column {
                    Text(
                        text = "Dificuldade",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        difficulties.forEach { diff ->
                            val selected = diff in draftDifficulties
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    draftDifficulties = if (selected) {
                                        draftDifficulties - diff
                                    } else {
                                        draftDifficulties + diff
                                    }
                                },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                label = { Text(diff) }
                            )
                        }
                    }
                }
            }

            // 3. Categoria / Tipo de Conteúdo
            item {
                Column {
                    Text(
                        text = "Categoria de Receita",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            val selected = cat in draftCategories
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    draftCategories = if (selected) {
                                        draftCategories - cat
                                    } else {
                                        draftCategories + cat
                                    }
                                },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                label = { Text(cat) }
                            )
                        }
                    }
                }
            }

            // 4. Ordenar Resultados
            item {
                Column {
                    Text(
                        text = "Ordenar por",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(RecipeSortOption.entries) { option ->
                            FilterChip(
                                selected = draftSort == option,
                                onClick = { draftSort = option },
                                label = { Text(option.label) }
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        onApplyFilters(
                            SearchFilters(
                                maxDurationMinutes = draftDuration,
                                selectedDifficulties = draftDifficulties,
                                selectedCategories = draftCategories,
                                sortBy = draftSort
                            )
                        )
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Aplicar Filtros")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Cartão de Receita nos resultados da pesquisa.
 */
@Composable
fun SearchRecipeCard(
    item: SearchRecipeItem,
    selectedInventory: Set<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.recipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                AssistChip(
                    onClick = onClick,
                    label = {
                        Text(
                            text = String.format(Locale.US, "%.1f ★", item.rating),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
            }

            Text(
                text = "Por @${item.authorName} • ${item.category}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${item.recipe.prepTimeMinutes} min", style = MaterialTheme.typography.labelMedium)
                }
                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Dificuldade: ${item.recipe.difficulty}", style = MaterialTheme.typography.labelMedium)
                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("❤️ ${item.likesCount}", style = MaterialTheme.typography.labelMedium)
            }

            if (item.ingredientNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ingredientes: ${item.ingredientNames.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (selectedInventory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "✓ Contém os ${selectedInventory.size} ingredientes do teu inventário",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Cartão de Utilizador nos resultados quando o seletor está em "Utilizadores".
 */
@Composable
fun SearchUserCard(
    user: SearchableUser,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = user.displayName.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "@${user.username} • ${user.followersCount} seguidores",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = user.bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Ver Perfil de ${user.displayName}"
                )
            }
        }
    }
}

/**
 * Modal de Detalhes da Receita ao clicar num cartão de resultado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchedRecipeDetailSheet(
    item: SearchRecipeItem,
    selectedInventory: Set<String>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = item.recipe.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Criado por @${item.authorName} • ${item.category}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("⏱️ ${item.recipe.prepTimeMinutes} min", fontWeight = FontWeight.Medium)
                Text("📊 ${item.recipe.difficulty}", fontWeight = FontWeight.Medium)
                Text(String.format(Locale.US, "⭐ %.1f", item.rating), fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Ingredientes (${item.ingredientNames.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            item.ingredientNames.forEach { ing ->
                val isFromInventory = selectedInventory.any { ing.contains(it, ignoreCase = true) }
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isFromInventory) Icons.Default.CheckCircle else Icons.Default.Circle,
                        contentDescription = null,
                        tint = if (isFromInventory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(if (isFromInventory) 18.dp else 8.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFromInventory) "$ing (no teu inventário)" else ing,
                        fontWeight = if (isFromInventory) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

/**
 * Modal de Perfil do Utilizador encontrado na pesquisa por "Utilizadores".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchedUserDetailSheet(
    user: SearchableUser,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "@${user.username} • ${user.followersCount} seguidores",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = user.bio, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Receitas Publicadas (${user.publishedRecipes.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (user.publishedRecipes.isEmpty()) {
                Text(
                    text = "Este utilizador ainda não tem receitas públicas visíveis.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                user.publishedRecipes.forEach { recipe ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(recipe.title, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Dificuldade: ${recipe.difficulty} • ${recipe.prepTimeMinutes} min",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}