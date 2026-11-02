package com.example.stockup.ui.shopping

import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import java.io.File
import android.os.Environment
import androidx.core.content.FileProvider
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.AddAPhoto
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AssistChip
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.semantics.Role
import com.example.stockup.ui.components.BotaoAmigo
import com.example.stockup.ui.components.BotaoConcluir
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.stockup.data.db.Product
import com.example.stockup.data.db.ShoppingItem
import com.example.stockup.utils.CurrencyAmountInputVisualTransformation
import com.example.stockup.utils.CurrencyUtils
import com.example.stockup.utils.DateUtils
import java.util.Locale

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
fun ShoppingScreen(
    viewModel: ShoppingViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val itemsToMove by viewModel.itemsToMove.collectAsState()

    var showMoveDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<ShoppingItem?>(null) }
    var itemToEdit by remember { mutableStateOf<ShoppingItem?>(null) }
    var showDetailedAddDialog by remember { mutableStateOf(false) }

    val totalChecked = remember(state.items) {
        state.items.filter { it.isCompleted }.sumOf { item ->
            CurrencyUtils.calculateItemTotal(item.quantity, item.unit, item.price)
        }
    }
    val totalPending = remember(state.items) {
        state.items.filter { !it.isCompleted }.sumOf { item ->
            CurrencyUtils.calculateItemTotal(item.quantity, item.unit, item.price)
        }
    }
    val grandTotal = totalChecked + totalPending

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Lista de Compras", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showDetailedAddDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Adicionar detalhado",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (state.items.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Resumo da Compra",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format(Locale("pt", "BR"), "No carrinho: R$ %.2f", totalChecked),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "Total",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format(Locale("pt", "BR"), "R$ %.2f", grandTotal),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (state.items.isEmpty()) {
                    EmptyShoppingState(modifier = Modifier.fillMaxSize().weight(1f))
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(state.items, key = { it.id }) { item ->
                            // animateItem: quando o "Mover Rápido" leva os itens para
                            // a despensa, eles deslizam para fora em vez de piscar.
                            // O gesto passa a ter consequência visível.
                            Box(modifier = Modifier.animateItem()) {
                                ShoppingItemCard(
                                    item = item,
                                    onToggle = { viewModel.toggleItemCompleted(item) },
                                    onDelete = { itemToDelete = item },
                                    onEdit = { itemToEdit = item }
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(100.dp)) }
                    }
                }
            }

            val hasCompleted = state.items.any { it.isCompleted }
            if (hasCompleted) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // O pulso e o ✓ vêm ANTES de mover: os itens só saem da
                        // lista depois que a pessoa viu que deu certo. Sem isso o
                        // botão desaparecia junto com a lista e ficava a dúvida.
                        BotaoConcluir(
                            texto = "Mover Rápido para a Despensa (Pendente)",
                            textoConcluido = "Na despensa!",
                            icone = Icons.Default.Bolt,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            cores = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            aoConcluir = { viewModel.moveCheckedItemsQuicklyToPantry() }
                        )

                        OutlinedButton(
                            onClick = {
                                viewModel.prepareSelectedItemsToMove()
                                showMoveDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Personalizar detalhes antes de mover", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        itemToDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text("Confirmar Exclusão", fontWeight = FontWeight.Bold) },
                text = { Text("Deseja realmente remover '${item.name}' da sua lista de compras?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteItem(item)
                            itemToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Excluir", color = MaterialTheme.colorScheme.onError)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDelete = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        itemToEdit?.let { item ->
            EditShoppingItemDialog(
                item = item,
                onDismiss = { itemToEdit = null },
                onSave = { updatedItem ->
                    viewModel.updateItem(updatedItem)
                    itemToEdit = null
                }
            )
        }

        if (showDetailedAddDialog) {
            AddDetailedShoppingItemDialog(
                onDismiss = { showDetailedAddDialog = false },
                onSave = { name, qty, unit, cat, price ->
                    viewModel.addItemWithDetails(name, qty, unit, cat, price)
                    showDetailedAddDialog = false
                }
            )
        }

        if (showMoveDialog && itemsToMove.isNotEmpty()) {
            MoveToPantryDialog(
                items = itemsToMove,
                onDismiss = {
                    showMoveDialog = false
                    viewModel.clearItemsToMove()
                },
                onConfirm = { productsToInsert ->
                    viewModel.moveItemsToPantry(productsToInsert)
                    showMoveDialog = false
                }
            )
        }
    }
}

@Composable
private fun ShoppingItemCard(
    item: ShoppingItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val totalItemPrice = CurrencyUtils.calculateItemTotal(item.quantity, item.unit, item.price)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompleted) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.isCompleted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        2.dp,
                        if (item.isCompleted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape
                    )
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (item.isCompleted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Concluído",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.quantity} ${item.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.price != null && item.price > 0.0) {
                        Text(
                            text = "• R$ " + String.format(Locale("pt", "BR"), "%.2f", totalItemPrice),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Excluir",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyShoppingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ShoppingCart,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Lista de compras vazia",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Adicione produtos tocando no botão + no topo",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EditShoppingItemDialog(
    item: ShoppingItem,
    onDismiss: () -> Unit,
    onSave: (ShoppingItem) -> Unit
) {
    var name by remember { mutableStateOf(item.name) }
    var quantity by remember { mutableStateOf(item.quantity) }
    var unit by remember { mutableStateOf(item.unit) }
    var priceDigits by remember { mutableStateOf(CurrencyUtils.doubleToCalculatorDigits(item.price)) }
    var category by remember { mutableStateOf(item.category) }

    val calculatedTotal = remember(quantity, unit, priceDigits) {
        val parsedPrice = CurrencyUtils.parseCalculatorPriceToDouble(priceDigits)
        CurrencyUtils.calculateItemTotal(quantity, unit, parsedPrice)
    }

    val priceLabel = when (unit.lowercase().trim()) {
        "g", "kg" -> "Preço por kg"
        "ml", "l" -> "Preço por Litro"
        else -> "Preço unitário"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar item", fontWeight = FontWeight.Bold) },
        text = {
            // Rolável: com o teclado aberto, os últimos campos ficavam escondidos.
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                RequiredLabel("Nome do produto", isRequired = true)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Ex: Arroz 5kg") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                RequiredLabel("Quantidade", isRequired = false)
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    placeholder = { Text("1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                RequiredLabel("Unidade", isRequired = false)
                val units = listOf("un", "kg", "g", "L", "ml", "cx", "pc")
                // Antes era um Row fixo: com 7 opções, "cx" e "pc" ficavam fora da
                // tela e eram inalcançáveis. Agora rola na horizontal, e cada opção
                // tem os 48dp mínimos de toque.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    units.forEach { u ->
                        val selected = unit == u
                        Box(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .widthIn(min = 56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .clickable(role = Role.RadioButton) { unit = u },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = u,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                RequiredLabel(priceLabel, isRequired = false)
                OutlinedTextField(
                    value = priceDigits,
                    onValueChange = { input ->
                        val clean = input.filter { it.isDigit() }
                        priceDigits = if (clean.length > 10) clean.take(10) else clean
                    },
                    visualTransformation = CurrencyAmountInputVisualTransformation(zeroPrefix = true),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Total sempre visível: antes ele aparecia e desaparecia, e o
                // conteúdo do diálogo pulava a cada dígito digitado no preço.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (calculatedTotal > 0.0) {
                            "R$ " + String.format(Locale("pt", "BR"), "%.2f", calculatedTotal)
                        } else {
                            "—"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            BotaoAmigo(
                onClick = {
                    if (name.isNotBlank()) {
                        val parsedPrice = CurrencyUtils.parseCalculatorPriceToDouble(priceDigits)
                        onSave(
                            item.copy(
                                name = name.trim(),
                                quantity = quantity.ifBlank { "1" },
                                unit = unit.ifBlank { "un" },
                                price = parsedPrice,
                                category = category
                            )
                        )
                    }
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun AddDetailedShoppingItemDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Double?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("un") }
    var priceDigits by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Despensa") }

    val calculatedTotal = remember(quantity, unit, priceDigits) {
        val parsedPrice = CurrencyUtils.parseCalculatorPriceToDouble(priceDigits)
        CurrencyUtils.calculateItemTotal(quantity, unit, parsedPrice)
    }

    val priceLabel = when (unit.lowercase().trim()) {
        "g", "kg" -> "Preço por kg"
        "ml", "l" -> "Preço por Litro"
        else -> "Preço unitário"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo item com detalhes", fontWeight = FontWeight.Bold) },
        text = {
            // Rolável: com o teclado aberto, os últimos campos ficavam escondidos.
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                RequiredLabel("Nome do Produto", isRequired = true)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Ex: Leite Caixa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                RequiredLabel("Quantidade", isRequired = false)
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    placeholder = { Text("1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                RequiredLabel("Unidade", isRequired = false)
                val units = listOf("un", "kg", "g", "L", "ml", "cx", "pc")
                // Antes era um Row fixo: com 7 opções, "cx" e "pc" ficavam fora da
                // tela e eram inalcançáveis. Agora rola na horizontal, e cada opção
                // tem os 48dp mínimos de toque.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    units.forEach { u ->
                        val selected = unit == u
                        Box(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .widthIn(min = 56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .clickable(role = Role.RadioButton) { unit = u },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = u,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                RequiredLabel(priceLabel, isRequired = false)
                OutlinedTextField(
                    value = priceDigits,
                    onValueChange = { input ->
                        val clean = input.filter { it.isDigit() }
                        priceDigits = if (clean.length > 10) clean.take(10) else clean
                    },
                    visualTransformation = CurrencyAmountInputVisualTransformation(zeroPrefix = true),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Total sempre visível: antes ele aparecia e desaparecia, e o
                // conteúdo do diálogo pulava a cada dígito digitado no preço.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (calculatedTotal > 0.0) {
                            "R$ " + String.format(Locale("pt", "BR"), "%.2f", calculatedTotal)
                        } else {
                            "—"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            BotaoAmigo(
                onClick = {
                    if (name.isNotBlank()) {
                        val parsedPrice = CurrencyUtils.parseCalculatorPriceToDouble(priceDigits)
                        onSave(name.trim(), quantity, unit, category, parsedPrice)
                    }
                }
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}


// Cria arquivo temporário seguro no Pictures para a câmera
private fun createSafeShoppingCameraUri(context: android.content.Context): Uri {
    val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        ?: File(context.filesDir, "pictures").apply { mkdirs() }
    val tempFile = File(picturesDir, "stockup_shop_${System.currentTimeMillis()}.jpg").apply {
        if (!exists()) {
            parentFile?.mkdirs()
            createNewFile()
        }
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveToPantryDialog(
    items: List<ShoppingItem>,
    onDismiss: () -> Unit,
    onConfirm: (List<Product>) -> Unit
) {
    if (items.isEmpty()) {
        onDismiss()
        return
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    var currentIndex by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val currentItem = items.getOrNull(currentIndex) ?: items.first()

    // Mapas em memória para manter o que o usuário preencheu caso volte ao item anterior
    val configuredProducts = remember { mutableStateMapOf<Long, Product>() }

    // Estados locais do item atual
    var name by remember(currentIndex) { mutableStateOf(configuredProducts[currentItem.id]?.name ?: currentItem.name) }
    var quantity by remember(currentIndex) { mutableStateOf(configuredProducts[currentItem.id]?.quantity ?: currentItem.quantity.ifBlank { "1" }) }
    var unit by remember(currentIndex) { mutableStateOf(configuredProducts[currentItem.id]?.unit ?: currentItem.unit.ifBlank { "un" }) }
    var priceDigits by remember(currentIndex) {
        val initialDigits = CurrencyUtils.doubleToCalculatorDigits(configuredProducts[currentItem.id]?.price ?: currentItem.price)
        mutableStateOf(initialDigits)
    }
    var category by remember(currentIndex) { mutableStateOf(configuredProducts[currentItem.id]?.category ?: currentItem.category.ifBlank { "Despensa" }) }
    var location by remember(currentIndex) { mutableStateOf(configuredProducts[currentItem.id]?.location ?: "Geral") }
    var minStock by remember(currentIndex) { mutableStateOf(configuredProducts[currentItem.id]?.minStock?.toInt()?.toString() ?: "0") }
    var notes by remember(currentIndex) { mutableStateOf(configuredProducts[currentItem.id]?.notes ?: "") }
    var barcode by remember(currentIndex) { mutableStateOf(configuredProducts[currentItem.id]?.barcode ?: "") }
    var photoUri by remember(currentIndex) { mutableStateOf(configuredProducts[currentItem.id]?.photoUri?.let { Uri.parse(it) }) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    var hasExpiration by remember(currentIndex) { mutableStateOf(configuredProducts[currentItem.id]?.hasExpiration ?: true) }
    var expirationDate by remember(currentIndex) {
        mutableStateOf(configuredProducts[currentItem.id]?.let { if (it.hasExpiration) it.expirationDate else null } ?: (System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000))
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showPhotoOptionsDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Launchers de Câmera e Galeria
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraUri != null) {
            photoUri = tempCameraUri
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUri = uri
        }
    }

    // Função interna para montar o Product do item atual
    fun buildCurrentProduct(): Product {
        val priceVal = CurrencyUtils.parseCalculatorPriceToDouble(priceDigits)
        val minStockVal = minStock.toDoubleOrNull() ?: 0.0
        return Product(
            id = 0L,
            name = name.trim().ifBlank { currentItem.name },
            quantity = quantity.trim().ifBlank { "1" },
            unit = unit.trim().ifBlank { "un" },
            price = priceVal,
            category = category.trim().ifBlank { "Despensa" },
            location = location.trim().ifBlank { "Geral" },
            expirationDate = if (hasExpiration) (expirationDate ?: 0L) else 0L,
            hasExpiration = hasExpiration,
            minStock = minStockVal,
            photoUri = photoUri?.toString(),
            notes = notes.trim(),
            barcode = barcode.trim().ifBlank { null }
        )
    }

    /**
     * Validação num lugar só. O botão "Concluir" pergunta isto ANTES de animar:
     * se faltar algo, ele não pulsa e não mostra ✓ — e o aviso aparece junto do
     * botão, não lá embaixo no fim do formulário, onde ninguém veria.
     */
    fun validarCampos(): Boolean {
        if (name.isBlank()) {
            errorMessage = "Nome do produto é obrigatório"
            return false
        }
        if (hasExpiration && expirationDate == null) {
            errorMessage = "Selecione a data de validade"
            return false
        }
        errorMessage = null
        return true
    }

    /** Fecha o fluxo: salva o item atual e manda todos para a despensa. */
    fun concluir(): Unit {
        configuredProducts[currentItem.id] = buildCurrentProduct()
        val finalProducts = items.map { item ->
            configuredProducts[item.id] ?: Product(
                id = 0L,
                name = item.name,
                quantity = item.quantity.ifBlank { "1" },
                unit = item.unit.ifBlank { "un" },
                price = item.price ?: 0.0,
                category = item.category.ifBlank { "Despensa" },
                location = "Geral",
                hasExpiration = false,
                expirationDate = 0L,
                minStock = 0.0
            )
        }
        onConfirm(finalProducts)
    }

    // Exibição em Tela Cheia (Dialog que ocupa 100% da tela)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Guardar na Despensa", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Produto ${currentIndex + 1} de ${items.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cancelar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 12.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        // O aviso de campo faltando fica AQUI, colado no botão.
                        // Antes ele nascia no fim do formulário: quem tocava em
                        // "Concluir" com o nome vazio não via nada acontecer.
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (currentIndex > 0) {
                                OutlinedButton(
                                    onClick = {
                                        configuredProducts[currentItem.id] = buildCurrentProduct()
                                        currentIndex--
                                        errorMessage = null
                                    },
                                    modifier = Modifier
                                        .weight(0.7f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Anterior")
                                }
                            }

                            if (currentIndex >= items.size - 1) {
                                // Último item: pulso → ✓ → só então guarda e fecha.
                                BotaoConcluir(
                                    texto = "Concluir e Guardar (${items.size})",
                                    textoConcluido = "Na despensa!",
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(52.dp),
                                    cores = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    validar = { validarCampos() },
                                    aoConcluir = { concluir() }
                                )
                            } else {
                                Button(
                                    onClick = {
                                        if (!validarCampos()) return@Button
                                        // Salva produto atual no mapa e vai para o próximo
                                        configuredProducts[currentItem.id] = buildCurrentProduct()
                                        currentIndex++
                                    },
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(
                                        "Próximo (${currentIndex + 2} de ${items.size}) ➔",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                            }
                        }
                    }
                }
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
                // Foto do Produto em Retângulo em Pé (260dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .clickable { showPhotoOptionsDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Foto do Produto",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Trocar Foto", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tirar foto ou escolher da galeria", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text("Formato em pé ideal para o estoque", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Nome do Produto com Botão de Código de Barras
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Nome do Produto *", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(60) },
                        placeholder = { Text("Ex: Leite Integral, Sabão em Pó...") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                try {
                                    val options = GmsBarcodeScannerOptions.Builder()
                                        .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E, Barcode.FORMAT_CODE_128)
                                        .enableAutoZoom()
                                        .build()
                                    val scanner = GmsBarcodeScanning.getClient(context, options)
                                    scanner.startScan().addOnSuccessListener { bCode ->
                                        bCode.rawValue?.let { code -> barcode = code }
                                    }
                                } catch (e: Exception) {}
                            }) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear Código", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (barcode.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Código: $barcode", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                }
                                IconButton(onClick = { barcode = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                // Quantidade e Preço
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Quantidade *", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            placeholder = { Text("1") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Preço *", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = priceDigits,
                            onValueChange = { clean -> priceDigits = clean.filter { it.isDigit() }.take(10) },
                            visualTransformation = com.example.stockup.utils.CurrencyAmountInputVisualTransformation(zeroPrefix = true),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Unidade (Pills)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Unidade", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val units = listOf("un", "kg", "g", "L", "ml", "cx", "pc")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(units) { u ->
                            val isSelected = unit.equals(u, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { unit = u },
                                label = { Text(u, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // Localização
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Localização na Casa", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val locations = listOf("Geral", "Geladeira", "Armário", "Despensa", "Freezer")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(locations) { loc ->
                            FilterChip(
                                selected = location.equals(loc, ignoreCase = true),
                                onClick = { location = loc },
                                label = { Text(loc) },
                                shape = CircleShape
                            )
                        }
                    }
                }

                // Categoria
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Categoria", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val categories = listOf("Despensa", "Laticínios", "Bebidas", "Limpeza", "Higiene", "Carnes", "Outros")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) },
                                shape = CircleShape
                            )
                        }
                    }
                }

                // Controle de Validade Unificado
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Controle de Validade", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Avisar antes de vencer ou marcar como não perecível", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = hasExpiration,
                                onClick = { hasExpiration = true },
                                label = { Text("🥗 Perecível", fontWeight = if (hasExpiration) FontWeight.Bold else FontWeight.Normal) },
                                shape = CircleShape,
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = !hasExpiration,
                                onClick = { hasExpiration = false },
                                label = { Text("♾️ Não Perecível", fontWeight = if (!hasExpiration) FontWeight.Bold else FontWeight.Normal) },
                                shape = CircleShape,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (hasExpiration) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDatePicker = true }
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Data de Vencimento:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (expirationDate != null) DateUtils.formatDate(expirationDate!!) else "Toque para selecionar",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val n = System.currentTimeMillis()
                                listOf(7 to "+7 dias", 15 to "+15 dias", 30 to "+30 dias").forEach { (d, lbl) ->
                                    AssistChip(
                                        onClick = { expirationDate = n + d * 24L * 60 * 60 * 1000 },
                                        label = { Text(lbl, style = MaterialTheme.typography.labelSmall) },
                                        shape = CircleShape
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AllInclusive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Ideal para arroz, feijão, óleo, macarrão. Sem alertas de vencimento.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // Estoque Mínimo
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Avisar estoque baixo quando restar:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("0 = Desativado (ideal se você compra apenas 1 por vez)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        OutlinedTextField(
                            value = minStock,
                            onValueChange = { minStock = it.filter { c -> c.isDigit() }.take(3) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.width(100.dp)
                        )
                    }
                }

                // Anotações
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Anotações", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("Ex: Embalagem fechada...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }

                // O aviso de campo obrigatório mora no rodapé, junto do botão —
                // aqui embaixo ele ficava escondido fora da rolagem.
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Diálogo de Foto
        if (showPhotoOptionsDialog) {
            AlertDialog(
                onDismissRequest = { showPhotoOptionsDialog = false },
                title = { Text("Foto do Produto", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                showPhotoOptionsDialog = false
                                val uri = createSafeShoppingCameraUri(context)
                                tempCameraUri = uri
                                takePictureLauncher.launch(uri)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Tirar Foto com a Câmera", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                showPhotoOptionsDialog = false
                                photoPickerLauncher.launch("image/*")
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Escolher da Galeria", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (photoUri != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    showPhotoOptionsDialog = false
                                    photoUri = null
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Remover Foto", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showPhotoOptionsDialog = false }) { Text("Cancelar") }
                }
            )
        }

        // Date Picker Modal
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = expirationDate ?: System.currentTimeMillis()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { expirationDate = it }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}
