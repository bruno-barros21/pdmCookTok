package com.example.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Página full-screen estilo TikTok / CookTok para cada receita do VerticalPager.
 */
@Composable
fun CookTokFeedPage(
    item: FeedRecipeItem,
    isFollowingAuthor: Boolean,
    isGuest: Boolean,
    onToggleFollow: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenComments: () -> Unit,
    onOpenRating: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenRecipeDetails: () -> Unit,
    onOpenShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Paleta visual variada por categoria para simular o fundo do vídeo curto
    val gradientColors = remember(item.category) {
        when (item.category) {
            "Massas" -> listOf(Color(0xFF3E1F14), Color(0xFF7A3B23), Color(0xFF1A100D))
            "Sobremesas" -> listOf(Color(0xFF3B1B36), Color(0xFF6B2D5C), Color(0xFF170C16))
            "Tradicional" -> listOf(Color(0xFF1B3327), Color(0xFF2D5944), Color(0xFF0E1A14))
            "Rápidas & Snacks" -> listOf(Color(0xFF1E2B45), Color(0xFF2F4B7C), Color(0xFF0E1420))
            else -> listOf(Color(0xFF2B241D), Color(0xFF544332), Color(0xFF14110E))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors))
    ) {
        // Indicador central do player de vídeo curto
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.16f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Pré-visualização do Vídeo CookTok",
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.35f)
            ) {
                Text(
                    text = "${item.category} • ${item.recipe.prepTimeMinutes} min • ${item.recipe.difficulty}",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Coluna direita de interações estilo CookTok
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FeedActionButton(
                icon = if (item.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                tint = if (item.isLikedByMe) Color(0xFFFF4D67) else Color.White,
                label = item.likesCount.toString(),
                onClick = onToggleLike
            )

            FeedActionButton(
                icon = Icons.Default.ChatBubbleOutline,
                tint = Color.White,
                label = item.comments.size.toString(),
                onClick = onOpenComments
            )

            FeedActionButton(
                icon = if (item.myStarRating != null) Icons.Default.Star else Icons.Default.StarBorder,
                tint = if (item.myStarRating != null) Color(0xFFFFC107) else Color.White,
                label = String.format(Locale.US, "%.1f", item.averageRating),
                onClick = onOpenRating
            )

            FeedActionButton(
                icon = if (item.isFavoriteByMe) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                tint = if (item.isFavoriteByMe) Color(0xFFFFD54F) else Color.White,
                label = "Guardar",
                onClick = onToggleFavorite
            )

            // Botão "Ver Receita" (abre caixa com ingredientes, preparação, duração, porções e dificuldade)
            FeedActionButton(
                icon = Icons.Default.RestaurantMenu,
                tint = Color(0xFF80CBC4),
                label = "Receita",
                onClick = onOpenRecipeDetails
            )

            FeedActionButton(
                icon = Icons.AutoMirrored.Filled.Send,
                tint = Color.White,
                label = "Partilhar",
                onClick = onOpenShare
            )
        }

        // Bloco inferior esquerdo: Autor + Botão Seguir Dinâmico + Título + Métricas + Atalho "Ver Receita"
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.76f)
                .padding(start = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = item.authorDisplayName.take(2).uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "@${item.authorUsername}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.viewsCount} visualizações",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))

                // Botão Seguir Dinâmico
                Button(
                    onClick = onToggleFollow,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowingAuthor && !isGuest) {
                            Color.White.copy(alpha = 0.24f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Text(
                        text = if (isFollowingAuthor && !isGuest) "A Seguir ✓" else "Seguir",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }

            Text(
                text = item.recipe.title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = item.description,
                color = Color.White.copy(alpha = 0.88f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            FilledTonalButton(
                onClick = onOpenRecipeDetails,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RestaurantMenu,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ver Receita Completa", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun FeedActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.size(46.dp),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.35f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(25.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Caixa de Detalhes da Receita ("Ver Receita"): ingredientes, duração, porções, dificuldade e passos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailsBottomSheet(
    item: FeedRecipeItem,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            item {
                Text(
                    text = item.recipe.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Por @${item.authorUsername} • ${item.category}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecipeMetricChip(
                        icon = Icons.Default.Schedule,
                        text = "${item.recipe.prepTimeMinutes} min",
                        modifier = Modifier.weight(1f)
                    )
                    RecipeMetricChip(
                        icon = Icons.Default.People,
                        text = "${item.servings} porções",
                        modifier = Modifier.weight(1f)
                    )
                    RecipeMetricChip(
                        icon = Icons.Default.Star,
                        text = "${item.recipe.difficulty}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item { HorizontalDivider() }

            item {
                Text(
                    text = "Ingredientes (${item.ingredientNames.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(item.ingredientNames) { ing ->
                Text(text = "• $ing", style = MaterialTheme.typography.bodyMedium)
            }

            item { HorizontalDivider() }

            item {
                Text(
                    text = "Modo de Preparação",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(item.preparationSteps.indices.toList()) { idx ->
                Text(
                    text = "${idx + 1}. ${item.preparationSteps[idx]}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun RecipeMetricChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Modal de Comentários: todos podem ler, mas apenas utilizadores registados podem comentar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedCommentsBottomSheet(
    item: FeedRecipeItem,
    isGuest: Boolean,
    onAddComment: (String) -> Unit,
    onLoginClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var commentDraft by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Comentários (${item.comments.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(item.comments, key = { it.id }) { comment ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "@${comment.authorUsername}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = comment.timestampLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = comment.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isGuest) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Visitantes não podem comentar.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onLoginClick) {
                            Text("Criar Conta")
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = commentDraft,
                        onValueChange = { commentDraft = it },
                        placeholder = { Text("Escreve um comentário...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    FilledIconButton(
                        enabled = commentDraft.isNotBlank(),
                        onClick = {
                            onAddComment(commentDraft)
                            commentDraft = ""
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar Comentário")
                    }
                }
            }
        }
    }
}

/**
 * Diálogo de Avaliação de 0 a 5 Estrelas (requisito do guião).
 */
@Composable
fun StarRatingDialog(
    item: FeedRecipeItem,
    onSubmitRating: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStars by remember(item.recipe.id) { mutableStateOf(item.myStarRating ?: 5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Avaliar \"${item.recipe.title}\"") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Escolhe a tua pontuação de 0 a 5 estrelas:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { selectedStars = star }) {
                            Icon(
                                imageVector = if (star <= selectedStars) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "$star estrelas",
                                tint = if (star <= selectedStars) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                TextButton(onClick = { selectedStars = 0 }) {
                    Text("Atribuir 0 estrelas (Não gostei)")
                }
                Text(
                    text = "Selecionado: $selectedStars ★",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmitRating(selectedStars) }) {
                Text("Submeter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * BottomSheet de Filtros por Rating, Categoria e Ordenação do Feed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedFiltersBottomSheet(
    currentFilter: FeedFilterState,
    isGuest: Boolean,
    onApply: (FeedFilterState) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var draftMinRating by remember(currentFilter) { mutableStateOf(currentFilter.minRating) }
    var draftCategory by remember(currentFilter) { mutableStateOf(currentFilter.selectedCategory) }
    var draftSort by remember(currentFilter) { mutableStateOf(currentFilter.sortMode) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtros do Feed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onReset) {
                    Text("Repor")
                }
            }

            // Filtro por Rating (estrelas)
            Column {
                Text("Rating Mínimo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val options = listOf(null to "Todos", 3.5f to "≥ 3.5 ★", 4.0f to "≥ 4.0 ★", 4.5f to "≥ 4.5 ★")
                    options.forEach { (minVal, label) ->
                        FilterChip(
                            selected = draftMinRating == minVal,
                            onClick = { draftMinRating = minVal },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // Filtro por Categoria
            Column {
                Text("Categoria", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = draftCategory == null,
                            onClick = { draftCategory = null },
                            label = { Text("Todas") }
                        )
                    }
                    items(defaultFeedCategories) { cat ->
                        FilterChip(
                            selected = draftCategory == cat,
                            onClick = { draftCategory = if (draftCategory == cat) null else cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            // Ordenação
            Column {
                Text(
                    text = if (isGuest) "Ordenação (Visitante: Top Ratings por defeito)" else "Ordenação",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FeedSortMode.entries) { mode ->
                        FilterChip(
                            selected = draftSort == mode,
                            onClick = { draftSort = mode },
                            label = { Text(mode.label) }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    onApply(
                        FeedFilterState(
                            minRating = draftMinRating,
                            selectedCategory = draftCategory,
                            sortMode = draftSort
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Aplicar ao Feed")
            }
        }
    }
}

/**
 * BottomSheet de Amigos no Feed (com scroll + ícone "Olho" para abrir o perfil do colega).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedFriendsListSheet(
    friends: List<FeedFriend>,
    followedAuthorIds: Set<String>,
    onInspectFriend: (FeedFriend) -> Unit,
    onToggleFollow: (FeedFriend) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Amigos (${friends.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Toca no ícone de olho para abrir o perfil de um colega.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(friends, key = { it.id }) { friend ->
                    val isFollowing = friend.id in followedAuthorIds
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = friend.displayName.take(2).uppercase(),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(friend.displayName, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "@${friend.username}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            TextButton(onClick = { onToggleFollow(friend) }) {
                                Text(if (isFollowing) "A Seguir" else "Seguir")
                            }
                            // Ícone "Olho" para abrir o perfil do colega
                            IconButton(onClick = { onInspectFriend(friend) }) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Ver Perfil de ${friend.displayName}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Perfil do Colega aberto pelo ícone "Olho" a partir da lista de Amigos do Feed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedFriendProfileSheet(
    friend: FeedFriend,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = friend.displayName.take(2).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(friend.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("@${friend.username}", color = MaterialTheme.colorScheme.primary)
                }
                FilledTonalButton(onClick = onToggleFollow) {
                    Text(if (isFollowing) "A Seguir ✓" else "Seguir")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Sobre mim", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(friend.bio, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${friend.publishedRecipes.size} Receitas • ${friend.followersCount} Seguidores • ${friend.followingCount} A Seguir",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            Text("Receitas Publicadas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (friend.publishedRecipes.isEmpty()) {
                Text("Sem receitas públicas visíveis.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                friend.publishedRecipes.forEach { recipe ->
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
        }
    }
}

/**
 * Diálogo de Partilha Direta de uma receita do Feed com um Amigo.
 */
@Composable
fun FeedShareDialog(
    item: FeedRecipeItem,
    friends: List<FeedFriend>,
    onConfirmShare: (FeedFriend) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFriend by remember { mutableStateOf(friends.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Partilhar \"${item.recipe.title}\"") },
        text = {
            Column {
                Text("Seleciona um amigo:", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                friends.forEach { friend ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFriend = friend },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedFriend?.id == friend.id,
                            onClick = { selectedFriend = friend }
                        )
                        Text("${friend.displayName} (@${friend.username})")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedFriend != null,
                onClick = { selectedFriend?.let(onConfirmShare) }
            ) {
                Text("Enviar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}