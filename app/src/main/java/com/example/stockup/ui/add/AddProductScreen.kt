package com.example.stockup.ui.add

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AssistChip
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.stockup.utils.CurrencyAmountInputVisualTransformation
import com.example.stockup.utils.DateUtils
import com.example.stockup.utils.ImageFileUtils
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
private fun RequiredLabel(text: String, isRequired: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isRequired) {
            Text(
                text = " *",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    viewModel: AddProductViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showDatePicker by remember { mutableStateOf(false) }
    var showNewLocationDialog by remember { mutableStateOf(false) }
    var showPhotoOptionsDialog by remember { mutableStateOf(false) }
    var newLocationInput by remember { mutableStateOf("") }
    var isProcessingPhoto by remember { mutableStateOf(false) }

    // Salva a URI da câmera de forma persistente para sobreviver caso o Android mate o processo por falta de RAM no Samsung
    var currentCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }

    // Launcher para Tirar Foto com a Câmera nativa de forma segura
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            val uriToProcess = currentCameraUriString?.let { Uri.parse(it) } ?: state.tempCameraUri
            if (uriToProcess != null) {
                isProcessingPhoto = true
                scope.launch(Dispatchers.IO) {
                    val optimized = ImageFileUtils.compressAndSaveOptimizedImage(context, uriToProcess)
                    withContext(Dispatchers.Main) {
                        viewModel.updatePhotoUri(optimized)
                        isProcessingPhoto = false
                    }
                }
            }
        }
    }

    // Launcher para Escolher da Galeria
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessingPhoto = true
            scope.launch(Dispatchers.IO) {
                val optimized = ImageFileUtils.compressAndSaveOptimizedImage(context, uri)
                withContext(Dispatchers.Main) {
                    viewModel.updatePhotoUri(optimized)
                    isProcessingPhoto = false
                }
            }
        }
    }

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            onSaved()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Editar Produto" else "Novo Produto", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Imagem do Produto (Câmera ou Galeria em Formato Retrato / Em Pé)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .clickable { showPhotoOptionsDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (isProcessingPhoto) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Otimizando foto...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (state.photoUri != null) {
                    AsyncImage(
                        model = state.photoUri,
                        contentDescription = "Foto do Produto",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Badge para trocar foto sobreposta
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Trocar Foto",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AddAPhoto,
                            contentDescription = "Adicionar Foto",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Tirar foto ou escolher da galeria",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Formato em pé ideal para garrafas, caixas e frascos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Nome do Produto com Botão de Código de Barras
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RequiredLabel("Nome do Produto", isRequired = true)
                    Text(
                        text = "${state.name.length}/60",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { viewModel.updateName(it) },
                    placeholder = { Text("Ex: Leite Integral, Sabão em Pó...") },
                    singleLine = true,
                    isError = state.errorMessage?.contains("Nome", ignoreCase = true) == true ||
                            state.errorMessage?.contains("palavra", ignoreCase = true) == true ||
                            state.errorMessage?.contains("símbolos", ignoreCase = true) == true ||
                            state.errorMessage?.contains("sequência", ignoreCase = true) == true,
                    trailingIcon = {
                        if (state.isSearchingBarcode) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            IconButton(onClick = {
                                try {
                                    val options = GmsBarcodeScannerOptions.Builder()
                                        .setBarcodeFormats(
                                            Barcode.FORMAT_EAN_13,
                                            Barcode.FORMAT_EAN_8,
                                            Barcode.FORMAT_UPC_A,
                                            Barcode.FORMAT_UPC_E,
                                            Barcode.FORMAT_CODE_128
                                        )
                                        .enableAutoZoom()
                                        .build()
                                    val scanner = GmsBarcodeScanning.getClient(context, options)
                                    scanner.startScan()
                                        .addOnSuccessListener { barcode ->
                                            barcode.rawValue?.let { code ->
                                                viewModel.onBarcodeScanned(code)
                                            }
                                        }
                                } catch (e: Exception) {
                                    // Fallback seguro caso o Google Play Services não esteja disponível
                                }
                            }) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = "Escanear Código de Barras",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Feedback visual inteligente do código de barras
                if (state.barcode.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Código: ${state.barcode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearBarcode() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remover código",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Mensagem de status do código de barras
                if (state.barcodeMessage != null) {
                    Text(
                        text = state.barcodeMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.barcodeMessage!!.contains("encontrado", ignoreCase = true))
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quantidade e Preço em Linha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quantidade
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RequiredLabel("Quantidade", isRequired = true)
                    OutlinedTextField(
                        value = state.quantity,
                        onValueChange = { viewModel.updateQuantity(it) },
                        placeholder = { Text("Ex: 1, 2, 500") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = state.errorMessage?.contains("Quantidade", ignoreCase = true) == true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Preço
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RequiredLabel("Preço", isRequired = true)
                    OutlinedTextField(
                        value = state.priceDigits,
                        onValueChange = { viewModel.updatePriceDigits(it) },
                        placeholder = { Text("R$ 0,00") },
                        visualTransformation = CurrencyAmountInputVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = state.errorMessage?.contains("Preço", ignoreCase = true) == true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Unidade (Botões de seleção rápida prontos)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RequiredLabel("Unidade", isRequired = false)
                val units = listOf("un", "kg", "g", "L", "ml", "cx", "pc")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(units) { u ->
                        val isSelected = state.unit.equals(u, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateUnit(u) },
                            label = { Text(u, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            // O card "Avisar estoque baixo quando restar" saiu daqui.
            //
            // Por que: produto sem validade agora é acompanhado pela PERGUNTA DO DIA
            // ("usou?"), que desconta da quantidade com um toque. O estoque mínimo
            // exigia que a pessoa configurasse um número em cada produto — e quase
            // ninguém configurava. Duas formas de vigiar a mesma coisa só confundem;
            // ficou a que funciona sem configurar nada.

            // Localização (Botões de seleção rápida + Novo Local)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RequiredLabel("Localização", isRequired = false)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(state.availableLocations) { loc ->
                        val isSelected = state.location.equals(loc, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateLocation(loc) },
                            label = { Text(loc, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = {
                                newLocationInput = ""
                                showNewLocationDialog = true
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Novo")
                                }
                            },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                labelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // Categoria
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RequiredLabel("Categoria", isRequired = false)
                val categories = listOf("Despensa", "Laticínios", "Bebidas", "Limpeza", "Higiene", "Carnes", "Outros")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = state.category == cat,
                            onClick = { viewModel.updateCategory(cat) },
                            label = { Text(cat) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            // Controle de Validade Unificado (Perecível vs Não Perecível)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Controle de Validade",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Configure para receber alertas antes do vencimento",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Seletor em Pills elegantes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = state.hasExpiration,
                            onClick = { viewModel.toggleHasExpiration(true) },
                            label = {
                                Text(
                                    "🥗 Perecível",
                                    fontWeight = if (state.hasExpiration) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = !state.hasExpiration,
                            onClick = { viewModel.toggleHasExpiration(false) },
                            label = {
                                Text(
                                    "♾️ Não Perecível",
                                    fontWeight = if (!state.hasExpiration) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (state.hasExpiration) {
                        // Card de Seleção de Data
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker = true }
                                .border(
                                    1.dp,
                                    if (state.errorMessage?.contains("validade", ignoreCase = true) == true) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "Data de Vencimento:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (state.expirationDate != null) DateUtils.formatDate(state.expirationDate!!) else "Toque para selecionar a data",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.expirationDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    // A validade é OPCIONAL. Sem data, o app não inventa
                                    // vencimento: acompanha pela quantidade.
                                    if (state.expirationDate == null) {
                                        Text(
                                            "Sem data eu acompanho pela quantidade.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = "Selecionar data",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Atalhos Rápidos (+7d, +15d, +30d)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val now = System.currentTimeMillis()
                            listOf(
                                7 to "+7 dias",
                                15 to "+15 dias",
                                30 to "+30 dias"
                            ).forEach { (days, label) ->
                                AssistChip(
                                    onClick = {
                                        viewModel.updateExpirationDate(now + days * 24L * 60 * 60 * 1000)
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    shape = CircleShape
                                )
                            }
                        }
                    } else {
                        // Banner explicativo para itens não perecíveis
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AllInclusive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Ideal para arroz, feijão, óleo, macarrão: dura meses. Não emitirá alertas de vencimento.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Anotações
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RequiredLabel("Anotações", isRequired = false)
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { viewModel.updateNotes(it) },
                    placeholder = { Text("Ex: Embalagem aberta, comprar mais logo...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            // Mensagem de Erro
            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Botão Salvar
            Button(
                onClick = { viewModel.saveProduct() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                enabled = !state.isSaving
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (state.isEditMode) "Salvar Alterações" else "Cadastrar Produto",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Diálogo para escolha da Foto (Câmera com URI seguro vs Galeria vs Remover)
        if (showPhotoOptionsDialog) {
            AlertDialog(
                onDismissRequest = { showPhotoOptionsDialog = false },
                title = { Text("Foto do Produto", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showPhotoOptionsDialog = false
                                    try {
                                        val uri = ImageFileUtils.createSafeCameraUri(context)
                                        currentCameraUriString = uri.toString()
                                        viewModel.setTempCameraUri(uri)
                                        takePictureLauncher.launch(uri)
                                    } catch (e: Exception) {
                                        // Tratamento de fallback seguro
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Tirar Foto com a Câmera", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showPhotoOptionsDialog = false
                                    photoPickerLauncher.launch("image/*")
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Escolher da Galeria", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (state.photoUri != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showPhotoOptionsDialog = false
                                        viewModel.updatePhotoUri(null)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Remover Foto Atual", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showPhotoOptionsDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Date Picker Modal
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = state.expirationDate ?: System.currentTimeMillis()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                // O picker devolve meia-noite UTC; convertemos para o dia local
                                // antes de guardar (ver DateUtils.utcPickerMillisToLocalDay).
                                viewModel.updateExpirationDate(DateUtils.utcPickerMillisToLocalDay(it))
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancelar")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Dialog Novo Local
        if (showNewLocationDialog) {
            AlertDialog(
                onDismissRequest = { showNewLocationDialog = false },
                title = { Text("Adicionar Novo Local", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newLocationInput,
                        onValueChange = { newLocationInput = it },
                        placeholder = { Text("Ex: Lavanderia, Banheiro...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newLocationInput.isNotBlank()) {
                                viewModel.addQuickLocation(newLocationInput)
                            }
                            showNewLocationDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text("Adicionar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewLocationDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
