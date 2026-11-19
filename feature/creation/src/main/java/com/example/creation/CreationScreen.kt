package com.example.creation
import android.os.Build
import androidx.compose.material.icons.filled.Share
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.data.notifications.CookTokNotificationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreationScreen(
    viewModel: CreationViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToAuth: () -> Unit = {},
    onRecipePublished: (NewRecipePayload) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // URI temporário gerado via FileProvider antes de abrir a Câmara
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingVideoUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // --- 1. Galeria via Android Photo Picker (NÃO requer permissão de armazenamento - "Ask for the least") ---
    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onMediaSelected(uri.toString(), MediaType.PHOTO, MediaSource.GALLERY)
        }
    }

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onMediaSelected(uri.toString(), MediaType.VIDEO, MediaSource.GALLERY)
        }
    }

    // --- 2. Câmara via FileProvider (content:// URI) ---
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingPhotoUri
        if (success && uri != null) {
            viewModel.onMediaSelected(uri.toString(), MediaType.PHOTO, MediaSource.CAMERA)
        }
        pendingPhotoUri = null
    }

    val captureVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        val uri = pendingVideoUri
        if (success && uri != null) {
            viewModel.onMediaSelected(uri.toString(), MediaType.VIDEO, MediaSource.CAMERA)
        }
        pendingVideoUri = null
    }

    fun launchCamera(mediaType: MediaType) {
        try {
            val uri = CreationMediaFileHelper.createTempMediaUri(context, mediaType)
            if (mediaType == MediaType.PHOTO) {
                pendingPhotoUri = uri
                takePictureLauncher.launch(uri)
            } else {
                pendingVideoUri = uri
                captureVideoLauncher.launch(uri)
            }
        } catch (_: Exception) {
            viewModel.onCameraPermissionDenied()
        }
    }

    // --- 3. Runtime Permission Launcher para Manifest.permission.CAMERA ---
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val targetMedia = state.pendingCameraMediaType ?: MediaType.PHOTO
        if (isGranted) {
            viewModel.onCameraPermissionGranted()
            viewModel.clearPendingCameraAction()
            launchCamera(targetMedia)
        } else {
            viewModel.onCameraPermissionDenied()
        }
    }

    var pendingNotificationPayload by remember { mutableStateOf<NewRecipePayload?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val payload = pendingNotificationPayload
        if (isGranted && payload != null) {
            CookTokNotificationHelper.notifyRecipePublished(
                context = context,
                recipeId = payload.id,
                recipeTitle = payload.title,
                locationTag = payload.locationTag
            )
        }
        pendingNotificationPayload = null
    }

    fun triggerPublishedNotification(payload: NewRecipePayload) {
        if (CookTokNotificationHelper.hasNotificationPermission(context)) {
            CookTokNotificationHelper.notifyRecipePublished(
                context = context,
                recipeId = payload.id,
                recipeTitle = payload.title,
                locationTag = payload.locationTag
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingNotificationPayload = payload
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Fluxo completo da Semana 8: 01 Check -> 02 Explain -> 03 Request -> 04 Handle Result
    fun handleCameraAction(mediaType: MediaType) {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (alreadyGranted) {
            viewModel.closeMediaPickerSheet()
            viewModel.onCameraPermissionGranted()
            launchCamera(mediaType)
        } else {
            val activity = context.findActivity()
            val shouldExplain = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.CAMERA
            )
            // Mostra rationale contextual tanto na primeira explicação como quando shouldShowRequestPermissionRationale é true
            if (shouldExplain || state.cameraPermissionState == CameraPermissionState.NOT_REQUESTED) {
                viewModel.requestCameraWithRationale(mediaType)
            } else {
                viewModel.requestCameraWithRationale(mediaType)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    // Botão Back: volta atrás para a secção anteriormente aberta
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar Atrás"
                        )
                    }
                },
                title = { Text("Nova Receita", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { viewModel.setGuestMode(!state.isGuest) }) {
                        Text(if (state.isGuest) "Simular Login" else "Modo Visitante")
                    }
                }
            )
        },
        bottomBar = {
            if (!state.isGuest) {
                Surface(tonalElevation = 4.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.weight(0.38f)
                        ) {
                            Text("Back")
                        }
                        Button(
    onClick = {
        viewModel.postRecipe(
            onPublishSuccess = { payload ->
                triggerPublishedNotification(payload)
                onRecipePublished(payload)
            }
        )
    },
    enabled = !state.isPosting,
    modifier = Modifier.weight(0.62f))
                            if (state.isPosting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("A publicar...")
                            } else {
                                Icon(Icons.Default.Publish, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Post")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (state.isGuest) {
            GuestCreationGate(
                onCreateAccountClick = {
                    viewModel.setGuestMode(false)
                    onNavigateToAuth()
                },
                onBackClick = onNavigateBack,
                modifier = Modifier.padding(paddingValues)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // 1. Cartão de Captura Multimédia (Foto ou Vídeo Curto via Galeria / Câmara)
            item {
                MediaCaptureCard(
                    selectedMedia = state.selectedMedia,
                    onSelectMediaClick = { viewModel.openMediaPickerSheet() },
                    onRemoveMediaClick = { viewModel.removeSelectedMedia() }
                )
            }

            // 2. Fallback visível caso o utilizador tenha recusado a permissão de Câmara
            if (state.cameraPermissionState == CameraPermissionState.DENIED_WITH_FALLBACK) {
                item {
                    CameraPermissionDeniedFallbackCard(
                        onUseGalleryFallbackClick = {
                            pickPhotoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        onOpenAppSettingsClick = {
                            val settingsIntent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(settingsIntent)
                        }
                    )
                }
            }

            // 3. Lista de erros de validação (se o utilizador tentar fazer Post incompleto)
            if (state.validationErrors.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Corrige os seguintes campos:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            state.validationErrors.forEach { err ->
                                Text(
                                    text = "• $err",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // 4. Formulário Completo de Detalhes da Receita
            item {
                RecipeDetailsFormSection(
                    state = state,
                    onTitleChange = { viewModel.onTitleChange(it) },
                    onDescriptionChange = { viewModel.onDescriptionChange(it) },
                    onCuriositiesChange = { viewModel.onCuriositiesChange(it) },
                    onCategorySelected = { viewModel.onCategorySelected(it) },
                    onPrepTimeChange = { viewModel.onPrepTimeChange(it) },
                    onServingsChange = { viewModel.onServingsChange(it) },
                    onDifficultySelected = { viewModel.onDifficultySelected(it) },
                    onAddIngredient = { viewModel.addIngredientRow() },
                    onUpdateIngredient = { id, name, qty -> viewModel.updateIngredient(id, name, qty) },
                    onRemoveIngredient = { viewModel.removeIngredientRow(it) },
                    onAddStep = { viewModel.addPreparationStep() },
                    onUpdateStep = { id, text -> viewModel.updatePreparationStep(id, text) },
                    onRemoveStep = { viewModel.removePreparationStep(it) }
                )
            }
            item{
            RecipeLocationTagCard(locationTag = state.locationTag, onLocationTagChange = { viewModel.onLocationTagChange(it) })
            }
            // 5. Confirmação das receitas publicadas nesta sessão ("Receitas Criadas")
            if (state.publishedRecipesSession.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Adicionadas a \"Receitas Criadas\" (${state.publishedRecipesSession.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            state.publishedRecipesSession.forEach { published ->
                                Text(
                                    text = "• ${published.title} (${published.prepTimeMinutes} min • ${published.difficulty.label})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        state.publishedRecipesSession.forEach { published ->
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "• ${published.title} (${published.prepTimeMinutes} min • ${published.difficulty.label})",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = {
                ExternalShareHelper.shareRecipeExternally(
                    context = context,
                    title = published.title,
                    description = published.description,
                    locationTag = published.locationTag,
                    mediaUriString = published.media.uriString,
                    mediaType = published.media.type
                )
            }
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Partilhar Receita Externamente"
            )
        }
    }
}
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // --- BottomSheet de Escolha: Galeria vs. Câmara (Foto e Vídeo Curto) ---
    if (state.isMediaPickerSheetOpen) {
        MediaSourcePickerBottomSheet(
            onPickPhotoFromGallery = {
                viewModel.closeMediaPickerSheet()
                pickPhotoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onTakePhotoWithCamera = {
                handleCameraAction(MediaType.PHOTO)
            },
            onPickVideoFromGallery = {
                viewModel.closeMediaPickerSheet()
                pickVideoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                )
            },
            onRecordVideoWithCamera = {
                handleCameraAction(MediaType.VIDEO)
            },
            onDismiss = { viewModel.closeMediaPickerSheet() }
        )
    }

    // --- Diálogo de Rationale antes do pedido de sistema de permissão CAMERA ---
    if (state.showCameraRationaleDialog) {
        val pendingType = state.pendingCameraMediaType ?: MediaType.PHOTO
        CameraPermissionRationaleDialog(
            mediaType = pendingType,
            onConfirmRequest = {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onFallbackToGallery = {
                viewModel.dismissCameraRationale()
                val visualType = if (pendingType == MediaType.PHOTO) {
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                } else {
                    ActivityResultContracts.PickVisualMedia.VideoOnly
                }
                if (pendingType == MediaType.PHOTO) {
                    pickPhotoLauncher.launch(PickVisualMediaRequest(visualType))
                } else {
                    pickVideoLauncher.launch(PickVisualMediaRequest(visualType))
                }
            },
            onDismiss = { viewModel.dismissCameraRationale() }
        )
    }
}