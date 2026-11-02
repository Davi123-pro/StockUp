package com.example.stockup.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.stockup.data.db.temDetalhesPendentes
import com.example.stockup.ui.components.AreasTour
import com.example.stockup.ui.components.BarraDesfazer
import com.example.stockup.ui.components.BotaoAmigo
import com.example.stockup.ui.components.IconeDespensa
import com.example.stockup.ui.components.LocalAreasDoTour
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.stockup.R
import com.example.stockup.data.db.Product
import com.example.stockup.theme.StatusExpired
import com.example.stockup.theme.StatusOk
import com.example.stockup.theme.StatusWarning
import com.example.stockup.utils.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Chaves das áreas do tour guiado — ver [AreasTour]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddProduct: () -> Unit,
    onProductClick: (Long) -> Unit,
    onEditProduct: (Long) -> Unit,
    onNotificationsClick: () -> Unit,
    hasUnreadNotifications: Boolean = false,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val exclusao by viewModel.exclusao.collectAsStateWithLifecycle()

    // O tour é comandado pela MainContainerScreen — ele troca de aba entre os
    // passos. Aqui só registramos ONDE cada alvo nosso está.
    val areasDoTour = LocalAreasDoTour.current
    // A Home usa barra de topo COMPACTA (ver topBar abaixo): o título grande
    // "expande/colapsa" com a rolagem e comia ~500 px do topo antes do primeiro
    // produto. Sem barra grande não existe mais scrollBehavior nesta tela.
    var showSearch by rememberSaveable { mutableStateOf(false) }
    
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedProductIds by remember { mutableStateOf(setOf<Long>()) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Usados pela tecla "pesquisar" do teclado: fecham o teclado ao concluir a busca.
    val teclado = LocalSoftwareKeyboardController.current
    val foco = LocalFocusManager.current

    val totalCount = state.products.size
    val expiringCount = remember(state.products) {
        state.products.count { p ->
            p.hasExpiration && p.expirationDate > 0L && DateUtils.daysUntilExpiration(p.expirationDate) in 0..7
        }
    }
    val expiredCount = remember(state.products) {
        state.products.count { p ->
            p.hasExpiration && p.expirationDate > 0L && DateUtils.daysUntilExpiration(p.expirationDate) < 0
        }
    }

    // Aviso de exclusão com o botão "Desfazer". É isto que tira o medo de apagar:
    // se tocar errado, um toque traz de volta — inclusive o lançamento no histórico.
    // A barra de aviso se fecha sozinha depois de 8 segundos. O tempo é generoso de
    // propósito: quem tem tremor ou lê devagar precisa de folga para reagir.
    LaunchedEffect(exclusao) {
        if (exclusao == null) return@LaunchedEffect
        delay(8_000)
        viewModel.limparEventoExclusao()
    }

    // Guarda o último aviso para o conteúdo não desaparecer no meio da animação de
    // saída (quando ela começa, `exclusao` já virou null).
    var eventoNaTela by remember { mutableStateOf<EventoExclusao?>(null) }
    LaunchedEffect(exclusao) {
        if (exclusao != null) eventoNaTela = exclusao
    }

    // O tour é desenhado POR CIMA de tudo desta tela. Por isso o Scaffold fica
    // dentro de um Box — a barra de abas mora fora daqui e continua acessível.
    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {},
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            "${selectedProductIds.size} selecionado(s)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedProductIds = emptySet()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancelar seleção")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            selectedProductIds = if (selectedProductIds.size == state.products.size) {
                                emptySet()
                            } else {
                                state.products.map { it.id }.toSet()
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Selecionar todos")
                        }
                        if (selectedProductIds.isNotEmpty()) {
                            IconButton(onClick = { showBatchDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir selecionados", tint = StatusExpired)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            } else {
                // Barra COMPACTA: uma linha só (logo + ações). A LargeTopAppBar antiga
                // empurrava o título para baixo e gastava ~21% da tela em vazio.
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(R.drawable.ic_stockup_logo),
                                contentDescription = "StockUp Logo",
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            // Fechar a busca também limpa o filtro. Sem isso o usuário
                            // ficaria com uma lista filtrada e nenhuma busca visível —
                            // exatamente o que acontecia ao girar o celular no modo retrato.
                            if (showSearch) viewModel.setSearchQuery("")
                            showSearch = !showSearch
                        }) {
                            Icon(
                                if (showSearch) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (state.products.isNotEmpty()) {
                            IconButton(onClick = { isSelectionMode = true }) {
                                Icon(
                                    Icons.Default.Checklist,
                                    contentDescription = "Selecionar vários",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(
                            onClick = onNotificationsClick,
                            modifier = Modifier.onGloballyPositioned {
                                areasDoTour?.put(AreasTour.SINO, it.boundsInRoot())
                            }
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    if (hasUnreadNotifications) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                    contentDescription = "Alertas",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                if (hasUnreadNotifications) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(StatusExpired)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        floatingActionButton = {
            // O "+" sai de cena enquanto a barra está na tela: ela ocupava justamente
            // o espaço dele, e dois botões disputando o mesmo canto é o tipo de coisa
            // que faz o usuário tocar no errado. Enquanto há um "Desfazer" na tela,
            // adicionar produto não é a próxima ação.
            // O "+" cresce e encolhe em vez de aparecer/sumir de estalo. Ele some
            // enquanto a barra de desfazer está na tela (dois botões no mesmo canto
            // fazem o usuário tocar no errado).
            AnimatedVisibility(
                visible = !isSelectionMode && exclusao == null,
                enter = scaleIn(initialScale = 0.6f) + fadeIn(),
                exit = scaleOut(targetScale = 0.6f) + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = onAddProduct,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .onGloballyPositioned { areasDoTour?.put(AreasTour.MAIS, it.boundsInRoot()) }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar Produto")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedVisibility(
                visible = showSearch,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Buscar produto...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpar busca")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    // A tecla "pesquisar" do teclado FECHA o teclado.
                    //
                    // Antes a ação existia mas não fazia nada: a pessoa tocava em
                    // "pesquisar", o resultado aparecia e o teclado ficava cobrindo
                    // metade da lista. Para digitar de novo, é só tocar na barra.
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            teclado?.hide()
                            foco?.clearFocus()
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            // FILTRO 1 — por VALIDADE (Todos / Vencendo / Vencidos).
            // Linha própria + formato de pílula (CircleShape) para não se confundir
            // com o filtro de local, que fica logo abaixo com outro formato.
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.filter == ProductFilter.ALL,
                        onClick = { viewModel.setFilter(ProductFilter.ALL) },
                        label = { Text("Todos") },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = state.filter == ProductFilter.EXPIRING_SOON,
                        onClick = { viewModel.setFilter(ProductFilter.EXPIRING_SOON) },
                        label = { Text("Vencendo") },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = state.filter == ProductFilter.EXPIRED,
                        onClick = { viewModel.setFilter(ProductFilter.EXPIRED) },
                        label = { Text("Vencidos") },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // FILTRO 2 — por LOCAL ("Onde está:"), que é outra coisa completamente
            // diferente do filtro de validade acima. Três diferenças visíveis ao mesmo
            // tempo, porque cor sozinha não resolve para quem enxerga pouco:
            //   1) tem a palavra escrita "Onde está:" antes dos botões;
            //   2) os botões têm cantos retos (RoundedCornerShape(8.dp)) em vez de pílula;
            //   3) fica numa linha separada, então a lista de locais nunca empurra
            //      "Vencendo"/"Vencidos" para fora da tela.
            // Se o usuário não cadastrou nenhum local, a linha inteira NÃO aparece:
            // melhor nada do que um "Todos" sozinho que não filtra coisa nenhuma.
            if (state.hasMultipleLocations) {
                LazyRow(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Text(
                            text = "Onde está:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(state.locations) { loc ->
                        FilterChip(
                            selected = state.selectedLocation == loc,
                            onClick = { viewModel.setLocation(loc) },
                            label = { Text(loc) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .onGloballyPositioned { areasDoTour?.put(AreasTour.RESUMO, it.boundsInRoot()) },
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "Total",
                    count = totalCount,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Atenção",
                    count = expiringCount,
                    color = StatusWarning,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Vencidos",
                    count = expiredCount,
                    color = StatusExpired,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (state.products.isEmpty() && state.searchQuery.isNotBlank()) {
                SearchEmptyState(
                    query = state.searchQuery,
                    onAddProduct = onAddProduct,
                    modifier = Modifier.fillMaxSize().weight(1f)
                )
            } else if (state.products.isEmpty()) {
                EmptyState(modifier = Modifier.fillMaxSize().weight(1f))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(
                        items = state.products,
                        key = { _, product -> product.id }
                    ) { _, product ->
                        val isSelected = selectedProductIds.contains(product.id)
                        
                        // animateItem: quando um produto sai — ou volta pelo
                        // "Desfazer" — a lista desliza em vez de piscar. Sem isso o
                        // desfazer parecia travado: o produto só reaparecia.
                        Box(
                            modifier = Modifier
                                .animateItem()
                                .then(
                                    // Só o primeiro produto serve de alvo do tour.
                                    if (product.id == state.products.firstOrNull()?.id) {
                                        Modifier.onGloballyPositioned {
                                            areasDoTour?.put(AreasTour.ACABOU, it.boundsInRoot())
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                        if (isSelectionMode) {
                            SelectableProductCard(
                                product = product,
                                isSelected = isSelected,
                                onToggle = {
                                    selectedProductIds = if (isSelected) {
                                        selectedProductIds - product.id
                                    } else {
                                        selectedProductIds + product.id
                                    }
                                }
                            )
                        } else {
                            // O arrastar-para-excluir foi removido: é gesto escondido,
                            // fácil de disparar sem querer e invisível para quem usa
                            // leitor de tela. A ação principal agora é o botão "Acabou".
                            ProductCard(
                                product = product,
                                onClick = { onProductClick(product.id) },
                                onAcabou = { viewModel.deleteProduct(product, wasWasted = false) }
                            )
                        }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            // A barra faz parte do layout: EMPURRA a lista para cima em vez de
            // flutuar por cima dela. Flutuando, a região dela virava um botão
            // "Acabou" vivo quando os 8 segundos expiravam — e um toque atrasado no
            // "Desfazer" apagava o produto de trás. Ficando aqui, o que está embaixo
            // nunca fica debaixo dela.
            AnimatedVisibility(
                visible = exclusao != null,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
            ) {
                BarraDesfazer(
                    mensagem = eventoNaTela?.mensagem.orEmpty(),
                    onAcao = if (eventoNaTela?.podeDesfazer == true) {
                        { viewModel.desfazerExclusao() }
                    } else {
                        null
                    },
                    rotuloExtra = if (eventoNaTela?.podeMarcarDesperdicio == true) {
                        "Foi jogado fora"
                    } else {
                        null
                    },
                    onExtra = if (eventoNaTela?.podeMarcarDesperdicio == true) {
                        { viewModel.marcarUltimaExclusaoComoDesperdicio() }
                    } else {
                        null
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // O diálogo de exclusão individual saiu de cena. Ele existia porque o card
        // tinha o lápis e o arrastar; agora o próprio card traz o botão "Acabou",
        // que já apaga direto e deixa a barra de aviso como rede de proteção.
        // O diálogo em lote continua abaixo, para limpar vários de uma vez.

        if (showBatchDeleteDialog && selectedProductIds.isNotEmpty()) {
            val selectedProducts = state.products.filter { selectedProductIds.contains(it.id) }
            AlertDialog(
                onDismissRequest = { showBatchDeleteDialog = false },
                title = {
                    Text(
                        if (selectedProducts.size == 1) {
                            "Apagar este produto?"
                        } else {
                            "Apagar ${selectedProducts.size} produtos?"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Eles saem da sua lista. Para o seu relatório ficar certo, " +
                                "me diga o que aconteceu — e se tocar errado, dá para desfazer."
                        )
                        BotaoAmigo(
                            onClick = {
                                viewModel.deleteMultipleProducts(selectedProducts, recordHistory = true, wasWasted = false)
                                isSelectionMode = false
                                selectedProductIds = emptySet()
                                showBatchDeleteDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            cores = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        ) {
                            Text("Usei todos", fontWeight = FontWeight.Bold)
                        }
                        BotaoAmigo(
                            onClick = {
                                viewModel.deleteMultipleProducts(selectedProducts, recordHistory = true, wasWasted = true)
                                isSelectionMode = false
                                selectedProductIds = emptySet()
                                showBatchDeleteDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            cores = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) {
                            Text("Joguei todos fora", fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = {
                                viewModel.deleteMultipleProducts(selectedProducts, recordHistory = false)
                                isSelectionMode = false
                                selectedProductIds = emptySet()
                                showBatchDeleteDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Só apagar (não entra no relatório)")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showBatchDeleteDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
    }
}

@Composable
private fun SelectableProductCard(
    product: Product,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .border(
                1.5.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 2 linhas + reticências: antes o nome era cortado no meio da palavra
                // ("Azeite de") porque o padrão do Compose é TextOverflow.Clip.
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${product.quantity} ${product.unit} • ${product.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (product.temDetalhesPendentes()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "⚠️ Detalhes pendentes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }


        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    onAcabou: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasExp = product.hasExpiration && product.expirationDate > 0L
    val daysLeft = if (hasExp) DateUtils.daysUntilExpiration(product.expirationDate) else 9999L
    val statusColor = when {
        !hasExp -> MaterialTheme.colorScheme.primary
        daysLeft < 0 -> StatusExpired
        daysLeft <= 3 -> StatusWarning
        else -> StatusOk
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (product.photoUri != null) {
                    AsyncImage(
                        model = product.photoUri,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    IconeDespensa(modifier = Modifier.size(28.dp))
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Mesmo ajuste do cartão de seleção: 2 linhas + reticências.
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${product.quantity} ${product.unit} • ${product.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (product.temDetalhesPendentes()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "⚠️ Detalhes pendentes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            !hasExp -> "Sem validade"
                            daysLeft < 0 -> "Vencido"
                            daysLeft == 0L -> "Hoje!"
                            daysLeft == 1L -> "Amanhã"
                            daysLeft <= 7L -> "$daysLeft dias"
                            else -> "$daysLeft dias"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // A ação principal. Um toque diz "acabou" e registra como usado, sem
            // perguntar nada. Se foi jogado fora, a barra de aviso oferece a
            // correção logo em seguida.
            BotaoAmigo(
                onClick = onAcabou,
                modifier = Modifier.fillMaxWidth(),
                cores = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("Acabou", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SearchEmptyState(
    query: String,
    onAddProduct: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Nenhum resultado para \"$query\"",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Este produto ainda não está cadastrado.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(
            onClick = onAddProduct,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Cadastrar \"$query\"")
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconeDespensa(
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Sua despensa está vazia",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            // Aponta para o botão de verdade, não para um símbolo solto.
            "Toque no + verde aqui embaixo para colocar o primeiro produto.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = count,
                transitionSpec = {
                    // O número sobe quando cresce e desce quando diminui: o movimento
                    // já diz a direção antes de a pessoa terminar de ler o valor.
                    val subindo = targetState > initialState
                    (slideInVertically { if (subindo) it else -it } + fadeIn()) togetherWith
                        (slideOutVertically { if (subindo) -it else it } + fadeOut())
                },
                label = "numeroDoResumo"
            ) { valor ->
                Text(
                    text = valor.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
