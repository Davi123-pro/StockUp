package com.example.stockup.ui.notifications

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.stockup.ui.components.BarraAviso
import com.example.stockup.theme.StatusExpired
import com.example.stockup.theme.StatusWarning
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onBack: () -> Unit,
    onProductClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedFilterTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Marca como lido ao SAIR da tela, não ao entrar.
    //
    // Antes marcava ao entrar: o usuário nunca via quais avisos eram novos, porque
    // no primeiro desenho tudo já estava "lido" — a tela parecia morta e sempre
    // igual. Agora ele entra, os novos aparecem destacados com "Novo", e o sininho
    // só apaga quando ele sai.
    DisposableEffect(Unit) {
        onDispose { viewModel.markAllAsRead() }
    }

    val filteredAlerts = remember(state.alerts, selectedFilterTab) {
        when (selectedFilterTab) {
            1 -> state.alerts.filter {
                it.type == AlertType.EXPIRING_URGENT ||
                it.type == AlertType.EXPIRING_SOON ||
                it.type == AlertType.EXPIRED
            }
            2 -> state.alerts.filter { it.type == AlertType.SUGGESTION }
            else -> state.alerts
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { dados ->
                BarraAviso(
                    mensagem = dados.visuals.message,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Avisos", fontWeight = FontWeight.Bold) },
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
        ) {
            // Só aparece quando há algo NOVO.
            //
            // Antes esta frase aparecia sempre ("Você tem 5 avisos."), mesmo depois de
            // o usuário ler tudo e sair. Dava a impressão de que nada tinha sido lido.
            // Se não há novidade, não há frase: a lista abaixo já mostra o que existe.
            val total = filteredAlerts.size
            val novos = filteredAlerts.count { it.id !in state.readIds }
            if (novos > 0) {
                Text(
                    text = if (novos == 1) "1 aviso novo." else "$novos avisos novos.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
                )
            }

            // Filtros por tipo
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // A aba "Estoque" saiu com o estoque mínimo: produto sem validade
                // agora é tratado pela pergunta do dia, fora da tela de Avisos.
                val filters = listOf("Todos", "Validade", "Dicas")
                items(filters.size) { index ->
                    FilterChip(
                        selected = selectedFilterTab == index,
                        onClick = { selectedFilterTab = index },
                        label = { Text(filters[index]) },
                        shape = CircleShape,
                        // Chip do Material tem ~32dp: abaixo do alvo mínimo de toque.
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            if (filteredAlerts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Tudo em dia!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Não há nada precisando da sua atenção agora.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredAlerts, key = { it.id }) { alert ->
                        val isRead = alert.id in state.readIds
                        NotificationCard(
                            alert = alert,
                            isRead = isRead,
                            onCardClick = {
                                viewModel.markAsRead(alert.id)
                                if (alert.type != AlertType.SUGGESTION) {
                                    onProductClick(alert.product.id)
                                }
                            },
                            onAddShoppingClick = { aoSomar ->
                                viewModel.markAsRead(alert.id)
                                // O botão NÃO trava: tocar de novo soma mais um na
                                // lista. Antes, cada toque criava uma linha repetida.
                                viewModel.addProductToShopping(alert.product, aoSomar)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    alert: NotificationAlert,
    isRead: Boolean = false,
    onCardClick: () -> Unit,
    onAddShoppingClick: (aoSomar: (Int) -> Unit) -> Unit
) {
    val stripColor = when (alert.type) {
        AlertType.EXPIRED -> StatusExpired
        AlertType.EXPIRING_URGENT -> Color(0xFFFF6B35)
        AlertType.EXPIRING_SOON -> StatusWarning
        AlertType.SUGGESTION -> MaterialTheme.colorScheme.tertiary
    }

    val iconVector = when (alert.type) {
        AlertType.EXPIRED -> Icons.Default.Warning
        AlertType.EXPIRING_URGENT -> Icons.Default.NotificationsActive
        AlertType.EXPIRING_SOON -> Icons.Default.Notifications
        AlertType.SUGGESTION -> Icons.Default.Info
    }

    // Quantas unidades deste produto já foram para a lista. Zero = ainda não tocou.
    var vezes by remember(alert.id) { mutableIntStateOf(0) }

    // Sugestoes nao precisam do botao "Comprar Mais"
    val showShoppingButton = alert.type != AlertType.SUGGESTION

    val jaNaLista = vezes > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (!isRead) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxSize()
                    .background(stripColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(stripColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = stripColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            if (!isRead) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = alert.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (!isRead) FontWeight.Bold else FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = alert.timestampFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = alert.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (showShoppingButton) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onAddShoppingClick { total -> vezes = total } },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (jaNaLista) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                },
                                contentColor = if (jaNaLista) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AnimatedContent(
                                    targetState = jaNaLista,
                                    transitionSpec = {
                                        // O ícone troca crescendo e girando de leve: o
                                        // olho pega a mudança mesmo sem ler o texto.
                                        (scaleIn(initialScale = 0.4f) + fadeIn()) togetherWith
                                            (scaleOut(targetScale = 0.4f) + fadeOut())
                                    },
                                    label = "iconeDoCarrinho"
                                ) { naLista ->
                                    Icon(
                                        imageVector = if (naLista) {
                                            Icons.Default.Check
                                        } else {
                                            Icons.Default.AddShoppingCart
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                AnimatedContent(
                                    targetState = vezes,
                                    transitionSpec = {
                                        (slideInVertically { it / 2 } + fadeIn()) togetherWith
                                            (slideOutVertically { -it / 2 } + fadeOut())
                                    },
                                    label = "textoDoCarrinho"
                                ) { quantos ->
                                    Text(
                                        text = when {
                                            quantos <= 0 -> "Comprar mais"
                                            quantos == 1 -> "Na lista"
                                            else -> "Na lista · $quantos"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
