package com.example.profile

import com.example.model.Recipe

data class UserProfileData(
    val id: String,
    val displayName: String,
    val username: String,
    val bio: String,
    val followersCount: Int = 0,
    val followingCount: Int = 0
)

data class FriendProfile(
    val id: String,
    val displayName: String,
    val username: String,
    val bio: String,
    val followersCount: Int,
    val followingCount: Int,
    val publishedRecipes: List<Recipe> = emptyList()
)

data class CreatorStats(
    val totalViews: Int = 0,
    val totalLikes: Int = 0,
    val totalComments: Int = 0,
    val averageRating: Float = 0f,
    /** Mapa de estrelas (0 a 5) para o número de avaliações recebidas */
    val ratingHistogram: Map<Int, Int> = mapOf(
        5 to 0,
        4 to 0,
        3 to 0,
        2 to 0,
        1 to 0,
        0 to 0
    )
)

data class SharedRecipeRecord(
    val id: String,
    val recipeTitle: String,
    val friendName: String,
    val friendUsername: String,
    val note: String
)

data class ProfileUiState(
    /** Controla a regra: utilizadores sem conta não têm acesso ao Perfil */
    val isGuest: Boolean = false,
    val isLoading: Boolean = false,
    val user: UserProfileData = UserProfileData(
        id = "user_1",
        displayName = "Bruno Barros",
        username = "Chef_Bruno",
        bio = "Apaixonado por cozinha portuguesa, receitas rápidas e experiências gastronómicas no CookTok.",
        followersCount = 248,
        followingCount = 94
    ),
    /** Tab ativo: 0 = Receitas Criadas & Estatísticas, 1 = Favoritos, 2 = Amigos */
    val selectedTab: Int = 0,
    /** Controla a abertura da página/modal de edição (ícone Lápis) */
    val isEditingProfile: Boolean = false,
    /** Receitas publicadas pelo próprio utilizador */
    val createdRecipes: List<Recipe> = emptyList(),
    /** Todas as receitas marcadas como favoritas */
    val allFavoriteRecipes: List<Recipe> = emptyList(),
    /** Listas de favoritos personalizadas (ex: "Lasanhas", "Sobremesas") -> IDs das receitas */
    val customFavoriteLists: Map<String, Set<String>> = mapOf(
        "Lasanhas" to emptySet(),
        "Sobremesas" to emptySet()
    ),
    /** Filtro de lista selecionada no separador Favoritos ("Todas" ou nome da lista customizada) */
    val selectedFavoriteListName: String = "Todas",
    /** Lista de amigos do utilizador */
    val friends: List<FriendProfile> = emptyList(),
    /** Amigo selecionado ao clicar no ícone "olho" para abrir o perfil do colega */
    val inspectedFriend: FriendProfile? = null,
    /** Receita selecionada para partilhar com um amigo */
    val recipeToShare: Recipe? = null,
    /** Amigo selecionado para receber uma receita */
    val friendToShareWith: FriendProfile? = null,
    /** Receita selecionada para adicionar/remover de listas de favoritos personalizadas */
    val recipeForListAssignment: Recipe? = null,
    /** Dashboard de estatísticas do criador */
    val stats: CreatorStats = CreatorStats(),
    /** Histórico recente de partilhas diretas com amigos */
    val sharedHistory: List<SharedRecipeRecord> = emptyList(),
    /** Mensagem de feedback temporário (Snackbar) */
    val userMessage: String? = null
)