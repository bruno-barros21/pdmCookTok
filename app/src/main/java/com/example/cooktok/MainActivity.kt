package com.example.cooktok

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cooktok.ui.theme.CookTokTheme
import com.example.creation.CreationScreen
import com.example.data.notifications.CookTokNotificationHelper
import com.example.feed.FeedScreen
import com.example.profile.ProfileScreen
import com.example.search.SearchScreen
import dagger.hilt.android.AndroidEntryPoint

private enum class CookTokDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    FEED("feed", "Feed", Icons.Default.Home),
    SEARCH("search", "Pesquisar", Icons.Default.Search),
    CREATION("creation", "Criar", Icons.Default.AddCircle),
    PROFILE("profile", "Perfil", Icons.Default.Person)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val openedRecipeIdFromNotification = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNotificationIntent(intent)

        setContent {
            // Nota: se em ui/theme/Theme.kt o tema tiver outro nome (ex: PdmCookTokTheme), ajusta aqui
            CookTokTheme {
                CookTokAppRoot(
                    notificationRecipeId = openedRecipeIdFromNotification.value,
                    onNotificationConsumed = { openedRecipeIdFromNotification.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val recipeId = intent?.getStringExtra(CookTokNotificationHelper.EXTRA_RECIPE_ID)
        if (!recipeId.isNullOrBlank()) {
            openedRecipeIdFromNotification.value = recipeId
        }
    }
}

@Composable
private fun CookTokAppRoot(
    notificationRecipeId: String?,
    onNotificationConsumed: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val snackbarHostState = remember { SnackbarHostState() }
    var showAuthPlaceholderDialog by remember { mutableStateOf(false) }

    // Quando o utilizador toca numa notificação, leva-o ao Feed e confirma a receção do deep-link
    LaunchedEffect(notificationRecipeId) {
        if (notificationRecipeId != null) {
            navController.navigate(CookTokDestination.FEED.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            snackbarHostState.showSnackbar("Aberto a partir da notificação (Receita #$notificationRecipeId)")
            onNotificationConsumed()
        }
    }

    // Esconde a barra inferior no ecrã de Criação para não sobrepor a barra Back/Post
    val showBottomBar = currentRoute != CookTokDestination.CREATION.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    CookTokDestination.entries.forEach { destination ->
                        val isSelected = currentRoute == destination.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (destination == CookTokDestination.CREATION) {
                                    // Mantém o ecrã anterior no backstack para o botão "Back" voltar exatamente para ele
                                    navController.navigate(destination.route) {
                                        launchSingleTop = true
                                    }
                                } else {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label
                                )
                            },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = CookTokDestination.FEED.route
            ) {
                composable(CookTokDestination.FEED.route) {
                    FeedScreen(
                        onNavigateToAuth = { showAuthPlaceholderDialog = true }
                    )
                }

                composable(CookTokDestination.SEARCH.route) {
                    SearchScreen()
                }

                composable(CookTokDestination.CREATION.route) {
                    CreationScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToAuth = { showAuthPlaceholderDialog = true },
                        onRecipePublished = {
                            // O utilizador permanece para poder testar a Partilha Externa ou voltar atrás
                        }
                    )
                }

                composable(CookTokDestination.PROFILE.route) {
                    ProfileScreen(
                        onNavigateToAuth = { showAuthPlaceholderDialog = true }
                    )
                }
            }
        }
    }

    // Diálogo temporário até implementarem o ecrã de Login/Registo real na Fase 2
    if (showAuthPlaceholderDialog) {
        AlertDialog(
            onDismissRequest = { showAuthPlaceholderDialog = false },
            title = { Text("Autenticação CookTok") },
            text = {
                Text("Sessão de utilizador registado (@Chef_Bruno) ativada! Na Fase 2 este fluxo ficará ligado ao ecrã de Login/Registo do user-service.")
            },
            confirmButton = {
                Button(onClick = { showAuthPlaceholderDialog = false }) {
                    Text("Continuar")
                }
            }
        )
    }
}