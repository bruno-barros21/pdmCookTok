package com.example.creation

import java.util.UUID

enum class MediaType(val label: String) {
    PHOTO("Fotografia"),
    VIDEO("Vídeo Curto")
}

enum class MediaSource(val label: String) {
    GALLERY("Galeria"),
    CAMERA("Câmara")
}

enum class RecipeDifficultyOption(val label: String) {
    EASY("Fácil"),
    MEDIUM("Média"),
    HARD("Difícil")
}

enum class CameraPermissionState {
    NOT_REQUESTED,
    GRANTED,
    DENIED_WITH_FALLBACK
}

data class SelectedMedia(
    val uriString: String,
    val type: MediaType,
    val source: MediaSource
)

data class DraftIngredient(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val quantity: String = ""
)

data class DraftPreparationStep(
    val id: String = UUID.randomUUID().toString(),
    val instruction: String = ""
)

/**
 * Representação pronta a enviar ao RecipeRepository / API quando o utilizador carrega em "Post".
 */
data class NewRecipePayload(
    val id: String = UUID.randomUUID().toString(),
    val authorId: String = "user_1",
    val title: String,
    val description: String,
    val curiosities: String,
    val category: String,
    val prepTimeMinutes: Int,
    val servings: Int,
    val difficulty: RecipeDifficultyOption,
    val ingredients: List<DraftIngredient>,
    val preparationSteps: List<DraftPreparationStep>,
    val media: SelectedMedia,
    val locationTag: String = ""
)

data class CreationUiState(
    /** Regra global: utilizadores sem conta não podem publicar receitas */
    val isGuest: Boolean = false,
    /** Média selecionada (foto ou vídeo curto, vindo da Galeria ou da Câmara) */
    val selectedMedia: SelectedMedia? = null,
    /** Controla o BottomSheet que dá a escolher entre Galeria e Câmara (para foto e vídeo curto) */
    val isMediaPickerSheetOpen: Boolean = false,
    /** Tipo de captura pendente enquanto se aguarda resposta da permissão CAMERA */
    val pendingCameraMediaType: MediaType? = null,
    /** Estado da permissão perigosa CAMERA (fluxo Check -> Explain -> Request -> Handle Result) */
    val cameraPermissionState: CameraPermissionState = CameraPermissionState.NOT_REQUESTED,
    /** Controla o diálogo de justificação (Rationale) antes de pedir acesso à câmara */
    val showCameraRationaleDialog: Boolean = false,
    /** Campos do formulário completo de detalhes da receita */
    val title: String = "",
    val description: String = "",
    val curiosities: String = "",
    val category: String = "Prato Principal",
    val prepTimeMinutesInput: String = "30",
    val servingsInput: String = "2",
    val difficulty: RecipeDifficultyOption = RecipeDifficultyOption.MEDIUM,
    val ingredients: List<DraftIngredient> = listOf(
        DraftIngredient(name = "", quantity = "")
    ),
    val preparationSteps: List<DraftPreparationStep> = listOf(
        DraftPreparationStep(instruction = "")
    ),
    /** Validação e submissão */
    val validationErrors: List<String> = emptyList(),
    val isPosting: Boolean = false,
    val publishedRecipesSession: List<NewRecipePayload> = emptyList(),
    val userMessage: String? = null,
    val locationTag: String = ""
)

val defaultCreationCategories = listOf(
    "Prato Principal",
    "Massas",
    "Tradicional",
    "Rápidas & Snacks",
    "Sobremesas",
    "Vegetariano"
)