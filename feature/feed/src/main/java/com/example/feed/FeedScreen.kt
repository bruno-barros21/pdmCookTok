package com.example.feed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel = hiltViewModel(),
    onNavigateToAuth: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading && state.feedItems.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.feedItems.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Nenhum CookTok encontrado para estes filtros.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.resetFilters() }) {
                            Text("Limpar Filtros")
                        }
                    }
                }

                else -> {
                    val pagerState = rememberPagerState(pageCount = { state.feedItems.size })

                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        val item = state.feedItems[pageIndex]
                        CookTokFeedPage(
                            item = item,
                            isFollowingAuthor = item.recipe.authorId in state.followedAuthorIds,
                            isGuest = state.isGuest,
                            onToggleFollow = {
                                viewModel.toggleFollowAuthor(item.recipe.authorId, item.authorUsername)
                            },
                            onToggleLike = { viewModel.toggleLike(item.recipe.id) },
                            onOpenComments = { viewModel.openCommentsSheet(item) },
                            onOpenRating = { viewModel.openRatingDialog(item) },
                            onToggleFavorite = { viewModel.toggleFavorite(item.recipe.id) },
                            onOpenRecipeDetails = { viewModel.openRecipeDetails(item) },
                            onOpenShare = { viewModel.openShareDialog(item) }
                        )
                    }
                }
            }

            // Barra Superior Flutuante: Amigos | Modo (Para Ti / Top Visitante) | Filtros Rating | Simular Visitante
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Botão Amigos (abre lista de amigos com ícone "Olho")
                FilledTonalIconButton(onClick = { viewModel.setFriendsSheetOpen(true) }) {
                    Icon(Icons.Default.Group, contentDescription = "Amigos")
                }

                // Indicador do Algoritmo ativo + alternador rápido de Modo Visitante para testes
                Surface(
                    onClick = { viewModel.setGuestMode(!state.isGuest) },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.isOfflineFallback) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = "Offline",
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (state.isGuest) "Top CookToks • Visitante" else "Para Ti • CookTok",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Botão Filtros por Rating / Categoria / Ordenação
                BadgedBox(
                    badge = {
                        if (state.filterState.activeFiltersCount > 0) {
                            Badge { Text(state.filterState.activeFiltersCount.toString()) }
                        }
                    }
                ) {
                    FilledTonalIconButton(onClick = { viewModel.setFilterSheetOpen(true) }) {
                        Icon(Icons.Default.Tune, contentDescription = "Filtros por Rating")
                    }
                }
            }
        }
    }

    // --- Modais e Diálogos do Feed ---
    state.selectedRecipeForDetails?.let { item ->
        RecipeDetailsBottomSheet(
            item = item,
            onDismiss = { viewModel.closeRecipeDetails() }
        )
    }

    state.selectedRecipeForComments?.let { item ->
        FeedCommentsBottomSheet(
            item = item,
            isGuest = state.isGuest,
            onAddComment = { text -> viewModel.addComment(item.recipe.id, text) },
            onLoginClick = {
                viewModel.closeCommentsSheet()
                viewModel.setGuestMode(false)
                onNavigateToAuth()
            },
            onDismiss = { viewModel.closeCommentsSheet() }
        )
    }

    state.selectedRecipeForRating?.let { item ->
        StarRatingDialog(
            item = item,
            onSubmitRating = { stars -> viewModel.submitStarRating(item.recipe.id, stars) },
            onDismiss = { viewModel.closeRatingDialog() }
        )
    }

    if (state.isFilterSheetOpen) {
        FeedFiltersBottomSheet(
            currentFilter = state.filterState,
            isGuest = state.isGuest,
            onApply = { viewModel.applyFilters(it) },
            onReset = { viewModel.resetFilters() },
            onDismiss = { viewModel.setFilterSheetOpen(false) }
        )
    }

    if (state.isFriendsSheetOpen) {
        FeedFriendsListSheet(
            friends = state.friends,
            followedAuthorIds = state.followedAuthorIds,
            onInspectFriend = { viewModel.inspectFriendProfile(it) },
            onToggleFollow = { friend -> viewModel.toggleFollowAuthor(friend.id, friend.username) },
            onDismiss = { viewModel.setFriendsSheetOpen(false) }
        )
    }

    state.inspectedFriend?.let { friend ->
        FeedFriendProfileSheet(
            friend = friend,
            isFollowing = friend.id in state.followedAuthorIds,
            onToggleFollow = { viewModel.toggleFollowAuthor(friend.id, friend.username) },
            onDismiss = { viewModel.closeInspectedFriendProfile() }
        )
    }

    state.selectedRecipeForShare?.let { item ->
        FeedShareDialog(
            item = item,
            friends = state.friends,
            onConfirmShare = { friend -> viewModel.confirmShareWithFriend(item, friend) },
            onDismiss = { viewModel.closeShareDialog() }
        )
    }
}