package com.example.creation

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.launch

@Composable
fun RecipeLocationTagCard(
    locationTag: String,
    onLocationTagChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isFetchingLocation by remember { mutableStateOf(false) }
    var showRationaleDialog by remember { mutableStateOf(false) }
    var isPermissionDenied by remember { mutableStateOf(false) }
    var statusNote by remember { mutableStateOf<String?>(null) }

    fun fetchGpsLocation() {
        scope.launch {
            isFetchingLocation = true
            statusNote = null
            val resolved = RecipeLocationHelper.fetchCurrentRecipeLocationLabel(context)
            isFetchingLocation = false
            if (resolved != null) {
                onLocationTagChange(resolved)
                statusNote = "Localização obtida via GPS."
            } else {
                statusNote = "Sem sinal GPS de momento. Podes escrever a cidade manualmente."
            }
        }
    }

    // Android 12+ exige pedir FINE e COARSE em conjunto
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            isPermissionDenied = false
            fetchGpsLocation()
        } else {
            isPermissionDenied = true
            statusNote = "Permissão de localização recusada. Usa o campo manual abaixo."
        }
    }

    fun onDetectLocationClicked() {
        // 1. Check (verifica sempre no momento de uso)
        if (RecipeLocationHelper.hasLocationPermission(context)) {
            isPermissionDenied = false
            fetchGpsLocation()
            return
        }

        // 2. Explain (Rationale se já tiver sido recusada antes)
        val activity = context.findActivity()
        val shouldShowRationale = activity != null && (
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        if (shouldShowRationale) {
            showRationaleDialog = true
        } else {
            // 3. Request
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Localização da Receita",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                FilledTonalButton(
                    onClick = { onDetectLocationClicked() },
                    enabled = !isFetchingLocation
                ) {
                    if (isFetchingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("A obter...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Usar GPS")
                    }
                }
            }

            // Campo editável (serve também de fallback imediato caso o GPS seja recusado)
            OutlinedTextField(
                value = locationTag,
                onValueChange = onLocationTagChange,
                label = { Text("Cidade / Região (ex: Porto, Portugal)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 4. Handle Result: Fallback visível na UI quando a permissão é negada
            if (isPermissionDenied) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GPS desativado. Escreve a região manualmente acima ou ativa nas Definições.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                                context.startActivity(intent)
                            }
                        ) {
                            Text("Definições")
                        }
                    }
                }
            }

            statusNote?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            title = { Text("Identificar Região do Prato") },
            text = {
                Text(
                    "O CookTok usa a tua localização apenas uma vez para associar a cidade ou região típica à receita que estás a publicar. Se recusares, podes escrevê-la manualmente."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRationaleDialog = false
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                ) {
                    Text("Permitir Localização")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationaleDialog = false }) {
                    Text("Escrever Manualmente")
                }
            }
        )
    }
}