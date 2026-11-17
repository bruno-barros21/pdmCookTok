package com.example.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.Recipe
import java.util.Locale

/**
 * Ecrã exibido quando o utilizador está em Modo Visitante (sem conta).
 * Cumpre a regra: "Utilizadores sem conta não poderão ter acesso ao Perfil sem criar conta."
 */
@Composable
fun GuestProfileGate(
    onCreateAccountClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(88.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Perfil Exclusivo para Utilizadores Registados",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Como visitante podes explorar receitas no Feed e pesquisar conteúdos, mas precisas de criar conta para aceder ao teu Perfil, guardar listas de favoritos, partilhar receitas com amigos e publicar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onCreateAccountClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Criar Conta CookTok")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Já tenho conta • Iniciar Sessão")
        }
    }
}

/**
 * Cabeçalho do Perfil Pessoal com Nome, @username, secção "Sobre mim" e botão Lápis.
 */
@Composable
fun ProfileHeaderCard(
    user: UserProfileData,
    recipesCount: Int,
    friendsCount: Int,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar com iniciais
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = user.displayName.take(2).uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Botão Lápis para editar Nome, @username e Sobre mim
                FilledTonalIconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar Perfil (Lápis)"
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Secção "Sobre mim"
            Text(
                text = "Sobre mim",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = user.bio.ifBlank { "Ainda sem descrição pessoal." },
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            // Contadores sociais
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStatCounter(value = recipesCount.toString(), label = "Receitas")
                ProfileStatCounter(value = friendsCount.toString(), label = "Amigos")
                ProfileStatCounter(value = user.followersCount.toString(), label = "Seguidores")
                ProfileStatCounter(value = user.followingCount.toString(), label = "A Seguir")
            }
        }
    }
}

@Composable
private fun ProfileStatCounter(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Página / Modal de Edição de Perfil aberta ao clicar no ícone Lápis.
 * Permite alterar o Nome de exibição, o @username e o texto "Sobre mim".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileBottomSheet(
    user: UserProfileData,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var displayName by remember(user.displayName) { mutableStateOf(user.displayName) }
    var username by remember(user.username) { mutableStateOf(user.username) }
    var bio by remember(user.bio) { mutableStateOf(user.bio) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Editar Perfil",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Identificador (@username)") },
                prefix = { Text("@") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Sobre mim") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { onSave(displayName, username, bio) }) {
                    Text("Aplicar Alterações")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Dashboard de Estatísticas do Criador (visualizações, likes, comentários e histograma de 0 a 5 estrelas).
 */
@Composable
fun CreatorStatsDashboard(
    stats: CreatorStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dashboard de Estatísticas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = String.format(Locale.US, "%.1f ★", stats.averageRating),
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricBox(
                    icon = Icons.Default.Visibility,
                    value = stats.totalViews.toString(),
                    label = "Visualizações",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                MetricBox(
                    icon = Icons.Default.Favorite,
                    value = stats.totalLikes.toString(),
                    label = "Gostos",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                MetricBox(
                    icon = Icons.Default.ChatBubbleOutline,
                    value = stats.totalComments.toString(),
                    label = "Comentários",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Histograma de Avaliações (0 a 5 Estrelas)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            val maxCount = (stats.ratingHistogram.values.maxOrNull() ?: 1).coerceAtLeast(1)

            (5 downTo 0).forEach { stars ->
                val count = stats.ratingHistogram[stars] ?: 0
                val fraction = count.toFloat() / maxCount.toFloat()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$stars ★",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(38.dp)
                    )
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(34.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Cartão de Receita usado tanto nas "Receitas Criadas" como nos "Favoritos".
 */
@Composable
fun RecipeProfileCard(
    recipe: Recipe,
    showAddToCustomListButton: Boolean,
    onAddToCustomListClick: (() -> Unit)? = null,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Dificuldade: ${recipe.difficulty} • ${recipe.prepTimeMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showAddToCustomListButton && onAddToCustomListClick != null) {
                IconButton(onClick = onAddToCustomListClick) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdd,
                        contentDescription = "Organizar nas Listas de Favoritos"
                    )
                }
            }

            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Partilhar Receita com Amigo"
                )
            }
        }
    }
}

/**
 * Perfil de um Colega/Amigo aberto ao clicar no ícone "Olho", em semelhança ao perfil pessoal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendProfileModal(
    friend: FriendProfile,
    onDismiss: () -> Unit,
    onShareWithFriendClick: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = friend.displayName.take(2).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = friend.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@${friend.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                FilledTonalButton(onClick = onShareWithFriendClick) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Partilhar")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Sobre mim", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(text = friend.bio, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStatCounter(value = friend.publishedRecipes.size.toString(), label = "Receitas")
                ProfileStatCounter(value = friend.followersCount.toString(), label = "Seguidores")
                ProfileStatCounter(value = friend.followingCount.toString(), label = "A Seguir")
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Receitas Publicadas por ${friend.displayName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (friend.publishedRecipes.isEmpty()) {
                Text(
                    text = "Este colega ainda não tem receitas públicas visíveis.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(friend.publishedRecipes) { recipe ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(recipe.title, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Dificuldade: ${recipe.difficulty} • ${recipe.prepTimeMinutes} min",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Diálogo para associar uma receita favorita a uma ou mais listas personalizadas (ex: Lasanhas, Sobremesas).
 */
@Composable
fun ManageRecipeListsDialog(
    recipe: Recipe,
    customLists: Map<String, Set<String>>,
    onToggleList: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Guardar \"${recipe.title}\" em:") },
        text = {
            if (customLists.isEmpty()) {
                Text("Ainda não criaste listas personalizadas. Usa o botão '+ Nova Lista' nos Favoritos.")
            } else {
                Column {
                    customLists.forEach { (listName, recipeIds) ->
                        val isChecked = recipe.id in recipeIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleList(listName) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { onToggleList(listName) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = listName, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Concluir")
            }
        }
    )
}

/**
 * Diálogo para partilhar diretamente uma receita com um amigo selecionado da lista.
 */
@Composable
fun ShareRecipeDialog(
    friends: List<FriendProfile>,
    availableRecipes: List<Recipe>,
    preselectedRecipe: Recipe?,
    preselectedFriend: FriendProfile?,
    onConfirmShare: (Recipe, FriendProfile, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRecipe by remember { mutableStateOf(preselectedRecipe ?: availableRecipes.firstOrNull()) }
    var selectedFriend by remember { mutableStateOf(preselectedFriend ?: friends.firstOrNull()) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Partilhar Receita com Amigo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (preselectedRecipe == null) {
                    Text("Escolhe a receita:", fontWeight = FontWeight.SemiBold)
                    if (availableRecipes.isEmpty()) {
                        Text(
                            text = "Não tens receitas criadas ou favoritas para partilhar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        availableRecipes.take(4).forEach { recipe ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedRecipe = recipe },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedRecipe?.id == recipe.id,
                                    onClick = { selectedRecipe = recipe }
                                )
                                Text(recipe.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                } else {
                    Text("Receita: ${preselectedRecipe.title}", fontWeight = FontWeight.SemiBold)
                }

                if (preselectedFriend == null) {
                    Text("Seleciona o amigo:", fontWeight = FontWeight.SemiBold)
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
                } else {
                    Text("Para: ${preselectedFriend.displayName} (@${preselectedFriend.username})", fontWeight = FontWeight.SemiBold)
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Mensagem opcional") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val canSend = selectedRecipe != null && selectedFriend != null
            Button(
                enabled = canSend,
                onClick = {
                    val r = selectedRecipe
                    val f = selectedFriend
                    if (r != null && f != null) {
                        onConfirmShare(r, f, note)
                    }
                }
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