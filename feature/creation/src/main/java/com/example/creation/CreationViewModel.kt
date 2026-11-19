package com.example.creation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreationViewModel @Inject constructor(
    @Suppress("unused") private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreationUiState())
    val uiState: StateFlow<CreationUiState> = _uiState.asStateFlow()

    // --- Seletor de Média (Galeria vs. Câmara para Foto e Vídeo Curto) ---
    fun openMediaPickerSheet() {
        _uiState.update { it.copy(isMediaPickerSheetOpen = true) }
    }

    fun closeMediaPickerSheet() {
        _uiState.update { it.copy(isMediaPickerSheetOpen = false) }
    }

    fun onMediaSelected(uriString: String, type: MediaType, source: MediaSource) {
        _uiState.update { state ->
            state.copy(
                selectedMedia = SelectedMedia(uriString = uriString, type = type, source = source),
                isMediaPickerSheetOpen = false,
                pendingCameraMediaType = null,
                validationErrors = state.validationErrors.filterNot { it.contains("foto ou vídeo", ignoreCase = true) },
                userMessage = "${type.label} carregado(a) a partir da ${source.label}."
            )
        }
    }

    fun removeSelectedMedia() {
        _uiState.update { it.copy(selectedMedia = null) }
    }

    // --- Gestão do Fluxo de Permissão da Câmara (Semana 8) ---
    fun requestCameraWithRationale(mediaType: MediaType) {
        _uiState.update {
            it.copy(
                isMediaPickerSheetOpen = false,
                pendingCameraMediaType = mediaType,
                showCameraRationaleDialog = true
            )
        }
    }

    fun dismissCameraRationale() {
        _uiState.update {
            it.copy(
                showCameraRationaleDialog = false,
                pendingCameraMediaType = null
            )
        }
    }

    fun onCameraPermissionGranted() {
        _uiState.update {
            it.copy(
                showCameraRationaleDialog = false,
                cameraPermissionState = CameraPermissionState.GRANTED
            )
        }
    }

    fun onCameraPermissionDenied() {
        _uiState.update {
            it.copy(
                showCameraRationaleDialog = false,
                pendingCameraMediaType = null,
                cameraPermissionState = CameraPermissionState.DENIED_WITH_FALLBACK,
                userMessage = "Permissão de câmara recusada. Podes continuar usando a Galeria."
            )
        }
    }

    fun clearPendingCameraAction() {
        _uiState.update { it.copy(pendingCameraMediaType = null) }
    }

    // --- Campos do Formulário de Detalhes da Receita ---
    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun onCuriositiesChange(value: String) {
        _uiState.update { it.copy(curiosities = value) }
    }

    fun onCategorySelected(category: String) {
        _uiState.update { it.copy(category = category) }
    }

    fun onPrepTimeChange(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(4)
        _uiState.update { it.copy(prepTimeMinutesInput = digitsOnly) }
    }

    fun onServingsChange(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(2)
        _uiState.update { it.copy(servingsInput = digitsOnly) }
    }

    fun onDifficultySelected(difficulty: RecipeDifficultyOption) {
        _uiState.update { it.copy(difficulty = difficulty) }
    }

    // --- Ingredientes Dinâmicos ---
    fun addIngredientRow() {
        _uiState.update { state ->
            state.copy(ingredients = state.ingredients + DraftIngredient())
        }
    }

    fun updateIngredient(id: String, name: String? = null, quantity: String? = null) {
        _uiState.update { state ->
            val updated = state.ingredients.map { item ->
                if (item.id == id) {
                    item.copy(
                        name = name ?: item.name,
                        quantity = quantity ?: item.quantity
                    )
                } else {
                    item
                }
            }
            state.copy(ingredients = updated)
        }
    }

    fun removeIngredientRow(id: String) {
        _uiState.update { state ->
            if (state.ingredients.size <= 1) return@update state
            state.copy(ingredients = state.ingredients.filterNot { it.id == id })
        }
    }

    // --- Passos de Preparação Dinâmicos ---
    fun addPreparationStep() {
        _uiState.update { state ->
            state.copy(preparationSteps = state.preparationSteps + DraftPreparationStep())
        }
    }

    fun updatePreparationStep(id: String, instruction: String) {
        _uiState.update { state ->
            val updated = state.preparationSteps.map { step ->
                if (step.id == id) step.copy(instruction = instruction) else step
            }
            state.copy(preparationSteps = updated)
        }
    }

    fun removePreparationStep(id: String) {
        _uiState.update { state ->
            if (state.preparationSteps.size <= 1) return@update state
            state.copy(preparationSteps = state.preparationSteps.filterNot { it.id == id })
        }
    }

    // --- Botão Post: Validação e Submissão ---
    fun postRecipe(onPublishSuccess: (NewRecipePayload) -> Unit) {
        val state = _uiState.value
        val errors = validateForm(state)

        if (errors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    validationErrors = errors,
                    userMessage = "Preenche os campos obrigatórios antes de publicar."
                )
            }
            return
        }

        val media = state.selectedMedia ?: return
        val validIngredients = state.ingredients.filter { it.name.isNotBlank() }
        val validSteps = state.preparationSteps.filter { it.instruction.isNotBlank() }

        val payload = NewRecipePayload(
            title = state.title.trim(),
            description = state.description.trim(),
            curiosities = state.curiosities.trim(),
            category = state.category,
            prepTimeMinutes = state.prepTimeMinutesInput.toIntOrNull() ?: 30,
            servings = state.servingsInput.toIntOrNull() ?: 2,
            difficulty = state.difficulty,
            ingredients = validIngredients,
            preparationSteps = validSteps,
            media = media,
            locationTag = state.locationTag.trim()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isPosting = true, validationErrors = emptyList()) }
            try {
                // Ponto de integração com :core:data (Offline-First / Room + Retrofit):
                // recipeRepository.createRecipe(payload.toDomainRecipe())
                delay(350) // Simulação curta sem bloquear a Main Thread

                _uiState.update { current ->
                    current.copy(
                        isPosting = false,
                        publishedRecipesSession = listOf(payload) + current.publishedRecipesSession,
                        // Limpa o formulário após publicação bem-sucedida
                        selectedMedia = null,
                        title = "",
                        description = "",
                        curiosities = "",
                        prepTimeMinutesInput = "30",
                        servingsInput = "2",
                        difficulty = RecipeDifficultyOption.MEDIUM,
                        ingredients = listOf(DraftIngredient()),
                        preparationSteps = listOf(DraftPreparationStep()),
                        userMessage = "Receita \"${payload.title}\" publicada em \"Receitas Criadas\"!"
                    )
                }
                onPublishSuccess(payload)
                locationTag = ""
            } catch (e: Exception) {
                // Networking defensivo (Semana 6): nunca deixar a app crashar se o backend estiver offline
                _uiState.update {
                    it.copy(
                        isPosting = false,
                        userMessage = "Sem ligação ao servidor. Tenta novamente ou verifica o Docker."
                    )
                }
            }
        }
    }

    private fun validateForm(state: CreationUiState): List<String> {
        val errors = mutableListOf<String>()
        if (state.selectedMedia == null) {
            errors += "Seleciona ou captura uma foto ou vídeo curto da receita."
        }
        if (state.title.isBlank()) {
            errors += "O título da receita é obrigatório."
        }
        val prepMinutes = state.prepTimeMinutesInput.toIntOrNull()
        if (prepMinutes == null || prepMinutes <= 0) {
            errors += "Indica um tempo de preparação válido (em minutos)."
        }
        val servings = state.servingsInput.toIntOrNull()
        if (servings == null || servings <= 0) {
            errors += "Indica o número de porções (mínimo 1)."
        }
        if (state.ingredients.none { it.name.isNotBlank() }) {
            errors += "Adiciona pelo menos um ingrediente."
        }
        if (state.preparationSteps.none { it.instruction.isNotBlank() }) {
            errors += "Descreve pelo menos um passo de preparação."
        }
        return errors
    }

    // --- Modo Visitante (Sem Conta) ---
    fun setGuestMode(isGuest: Boolean) {
        _uiState.update { it.copy(isGuest = isGuest) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
    fun onLocationTagChange(value: String) { _uiState.update { it.copy(locationTag = value) } }

}