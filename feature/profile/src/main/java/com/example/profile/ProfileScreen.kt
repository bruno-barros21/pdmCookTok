package com.example.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToAuth: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateListDialog by remember { mutableStateOf(false) }
    var newListTitle by remember { mutableStateOf("") }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Perfil", fontWeight = FontWeight.Bold) },
                actions = {
                    // Alternador rápido para validar a restrição de Visitante (Sem Conta)
                    TextButton(onClick = { viewModel.setGuestMode(!state.isGuest) }) {
                        Text(if (state.isGuest) "Simular Login" else "Modo Visitante")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isGuest) {
            GuestProfileGate(
                onCreateAccountClick = {
                    viewModel.setGuestMode(false)
                    onNavigateToAuth()
                },
                onLoginClick = {
                    viewModel.setGuestMode(false)
                    onNavigateToAuth()
                },
                modifier = Modifier.padding(paddingValues)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // 1. Cabeçalho: Nome, @username, "Sobre mim" e botão Lápis
            item {
                ProfileHeaderCard(
                    user = state.user,
                    recipesCount = state.createdRecipes.size,
                    friendsCount = state.friends.size,
                    onEditClick = { viewModel.openEditProfile() }
                )
            }

            // 2. Separadores Principais: Receitas Criadas | Favoritos | Amigos
            item {
                TabRow(selectedTabIndex = state.selectedTab) {
                    Tab(
                        selected = state.selectedTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        text = { Text("Receitas Criadas") }
                    )
                    Tab(
                        selected = state.selectedTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        text = { Text("Favoritos") }
                    )
                    Tab(
                        selected = state.selectedTab == 2,
                        onClick = { viewModel.selectTab(2) },
                        text = { Text("Amigos") }
                    )
                }
            }

            // 3. Conteúdo dinâmico do separador selecionado
            when (state.selectedTab) {
                0 -> {
                    // Dashboard de Estatísticas + Histograma (0 a 5 estrelas)
                    item {
                        CreatorStatsDashboard(stats = state.stats)
                    }

                    item {
                        Text(
                            text = "Histórico de Receitas Publicadas (${state.createdRecipes.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (state.createdRecipes.isEmpty()) {
                        item {
                            EmptySectionPlaceholder(
                                text = "Ainda não publicaste nenhuma receita. Usa o botão '+' para criar a primeira!"
                            )
                        }
                    } else {
                        items(state.createdRecipes, key = { it.id }) { recipe ->
                            RecipeProfileCard(
                                recipe = recipe,
                                showAddToCustomListButton = false,
                                onShareClick = { viewModel.startSharingRecipe(recipe) }
                            )
                        }
                    }
                }

                1 -> {
                    // Barra de Listas Personalizadas de Favoritos (ex: Lasanhas, Sobremesas, + Nova Lista)
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "As Minhas Listas de Favoritos",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                FilledTonalButton(onClick = { showCreateListDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Nova Lista")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    FilterChip(
                                        selected = state.selectedFavoriteListName == "Todas",
                                        onClick = { viewModel.selectFavoriteList("Todas") },
                                        label = { Text("Todas (${state.allFavoriteRecipes.size})") }
                                    )
                                }
                                items(state.customFavoriteLists.keys.toList()) { listName ->
                                    val count = state.customFavoriteLists[listName]?.size ?: 0
                                    FilterChip(
                                        selected = state.selectedFavoriteListName == listName,
                                        onClick = { viewModel.selectFavoriteList(listName) },
                                        label = { Text("$listName ($count)") },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Apagar Lista",
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable { viewModel.deleteFavoriteList(listName) }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    val displayedFavorites = if (state.selectedFavoriteListName == "Todas") {
                        state.allFavoriteRecipes
                    } else {
                        val targetIds = state.customFavoriteLists[state.selectedFavoriteListName] ?: emptySet()
                        state.allFavoriteRecipes.filter { it.id in targetIds }
                    }

                    if (displayedFavorites.isEmpty()) {
                        item {
                            EmptySectionPlaceholder(
                                text = if (state.selectedFavoriteListName == "Todas") {
                                    "Ainda não tens receitas nos favoritos."
                                } else {
                                    "A lista \"${state.selectedFavoriteListName}\" está vazia. Clica no ícone de marcador numa receita favorita para a adicionar aqui."
                                }
                            )
                        }
                    } else {
                        items(displayedFavorites, key = { it.id }) { recipe ->
                            RecipeProfileCard(
                                recipe = recipe,
                                showAddToCustomListButton = true,
                                onAddToCustomListClick = { viewModel.openListAssignmentDialog(recipe) },
                                onShareClick = { viewModel.startSharingRecipe(recipe) }
                            )
                        }
                    }
                }

                2 -> {
                    // Lista de Amigos com scroll, ícone "Olho" (ver perfil) e botão Partilhar Receita
                    item {
                        Text(
                            text = "Lista de Amigos (${state.friends.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(state.friends, key = { it.id }) { friend ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(48.dp),
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
                                    Text(
                                        text = friend.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "@${friend.username}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Ícone "Olho": abre o perfil do colega em semelhança ao perfil pessoal
                                IconButton(onClick = { viewModel.openFriendProfile(friend) }) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = "Ver Perfil do Colega"
                                    )
                                }

                                // Botão para partilhar diretamente uma receita com este amigo
                                IconButton(onClick = { viewModel.startSharingWithFriend(friend) }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Partilhar Receita com ${friend.displayName}"
                                    )
                                }
                            }
                        }
                    }

                    if (state.sharedHistory.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Partilhas Recentes",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(state.sharedHistory, key = { it.id }) { shared ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Enviaste \"${shared.recipeTitle}\" para @${shared.friendUsername}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (shared.note.isNotBlank()) {
                                        Text(
                                            text = "\"${shared.note}\"",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Modais e Diálogos ---
    if (state.isEditingProfile) {
        EditProfileBottomSheet(
            user = state.user,
            onDismiss = { viewModel.closeEditProfile() },
            onSave = { name, username, bio ->
                viewModel.saveProfileChanges(name, username, bio)
            }
        )
    }

    state.inspectedFriend?.let { friend ->
        FriendProfileModal(
            friend = friend,
            onDismiss = { viewModel.closeFriendProfile() },
            onShareWithFriendClick = {
                viewModel.closeFriendProfile()
                viewModel.startSharingWithFriend(friend)
            }
        )
    }

    state.recipeForListAssignment?.let { recipe ->
        ManageRecipeListsDialog(
            recipe = recipe,
            customLists = state.customFavoriteLists,
            onToggleList = { listName -> viewModel.toggleRecipeInCustomList(recipe.id, listName) },
            onDismiss = { viewModel.closeListAssignmentDialog() }
        )
    }

    if (state.recipeToShare != null || state.friendToShareWith != null) {
        val shareableRecipes = (state.createdRecipes + state.allFavoriteRecipes).distinctBy { it.id }
        ShareRecipeDialog(
            friends = state.friends,
            availableRecipes = shareableRecipes,
            preselectedRecipe = state.recipeToShare,
            preselectedFriend = state.friendToShareWith,
            onConfirmShare = { recipe, friend, msg ->
                viewModel.confirmShareRecipeWithFriend(recipe, friend, msg)
            },
            onDismiss = { viewModel.cancelSharing() }
        )
    }

    if (showCreateListDialog) {
        AlertDialog(
            onDismissRequest = { showCreateListDialog = false },
            title = { Text("Criar Lista de Favoritos") },
            text = {
                OutlinedTextField(
                    value = newListTitle,
                    onValueChange = { newListTitle = it },
                    label = { Text("Nome da lista (ex: Lasanhas, Sobremesas)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.createFavoriteList(newListTitle)
                    newListTitle = ""
                    showCreateListDialog = false
                }) {
                    Text("Criar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    newListTitle = ""
                    showCreateListDialog = false
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun EmptySectionPlaceholder(text: String) {
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