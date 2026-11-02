package com.example.stockup.ui.reports

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stockup.data.db.ConsumptionHistory
import com.example.stockup.theme.StatusExpired
import com.example.stockup.theme.StatusOk
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

// ─── Paleta de desperdício ───
private val WasteColor = Color(0xFFE53935)        // Vermelho
private val WasteContainerColor = Color(0xFFFFEBEE) // Fundo do card "desperdiçado"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showHelpDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Relatórios de Consumo", fontWeight = FontWeight.Bold)
                        Text(
                            "Acompanhe seu aproveitamento",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Default.HelpOutline,
                            contentDescription = "Como funciona",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Seletor de Período ──
            item {
                PeriodSelector(
                    selected = state.selectedPeriod,
                    onSelect = { viewModel.selectPeriod(it) }
                )
            }

            // ── Cards de Resumo ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card Consumido
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        // "Consumido" + "0 itens aproveitados" soava como nota da casa.
                        // Agora diz o que aconteceu: quantos alimentos foram usados.
                        label = "Consumidos",
                        value = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(state.totalSavedValue),
                        subtitle = "${state.totalConsumedCount} " +
                            if (state.totalConsumedCount == 1) "alimento consumido" else "alimentos consumidos",
                        icon = Icons.Default.TrendingUp,
                        accentColor = StatusOk
                    )
                    // Card Desperdiçado
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        label = "Perdidos",
                        value = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(state.totalWastedValue),
                        subtitle = "${state.totalWastedCount} " +
                            if (state.totalWastedCount == 1) "alimento perdido" else "alimentos perdidos",
                        icon = Icons.Default.TrendingDown,
                        accentColor = WasteColor
                    )
                }
            }

            // ── Score de Aproveitamento ──
            item {
                EfficiencyScoreCard(score = state.efficiencyScore)
            }

            // ── Gráfico de Barras ──
            if (state.monthlyTrends.isNotEmpty()) {
                item {
                    MonthlyBarChart(trends = state.monthlyTrends)
                }
            }

            // ── Categorias Consumidas ──
            if (state.topCategoriesConsumed.isNotEmpty()) {
                item {
                    Text(
                        "Categorias Mais Consumidas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(state.topCategoriesConsumed) { item ->
                    CategoryItemCard(
                        category = item.category,
                        count = item.count,
                        totalAmount = item.totalAmount,
                        percentage = item.percentage,
                        isWasted = false
                    )
                }
            }

            // ── Categorias Desperdiçadas ──
            if (state.topCategoriesWasted.isNotEmpty()) {
                item {
                    Text(
                        "Categorias com Mais Desperdício",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(state.topCategoriesWasted) { item ->
                    CategoryItemCard(
                        category = item.category,
                        count = item.count,
                        totalAmount = item.totalAmount,
                        percentage = item.percentage,
                        isWasted = true
                    )
                }
            }

            // ── Histórico Detalhado ──
            if (state.historyList.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Histórico Detalhado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(state.historyList) { item ->
                    HistoryCard(item)
                }
            } else if (state.totalConsumedCount == 0 && state.totalWastedCount == 0) {
                item {
                    EmptyReportsState(modifier = Modifier.fillMaxWidth().height(160.dp))
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                title = { Text("Como Funciona o Relatório", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("ℹ️ Toda vez que você exclui um produto na Despensa, o app pergunta se foi consumido ou jogado fora.")
                        Text("✅ 'Consumido': adiciona o valor aos produtos aproveitados.")
                        Text("❌ 'Jogado Fora': calcula o valor desperdiçado e orienta para as próximas compras.")
                        Text("⚡ 'Apenas Remover': remove o item sem alterar as métricas.")
                        Text("📊 O número do aproveitamento olha só os últimos 30 dias: de cada 100 itens que saíram da despensa, quantos foram usados. Ele se atualiza sozinho — não precisa fazer nada.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHelpDialog = false }) {
                        Text("Entendi", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

// ─────────────── Seletor de Período ───────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSelector(
    selected: ReportPeriod,
    onSelect: (ReportPeriod) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReportPeriod.entries.forEach { period ->
            FilterChip(
                selected = period == selected,
                onClick = { onSelect(period) },
                label = { Text(period.label, fontWeight = if (period == selected) FontWeight.Bold else FontWeight.Normal) },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

// ─────────────── Card de Resumo ───────────────
@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color
) {
    Card(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─────────────── Score de Aproveitamento ───────────────
@Composable
private fun EfficiencyScoreCard(score: EfficiencyScore) {
    val scoreColor by animateColorAsState(
        targetValue = when {
            score.isInsufficient -> MaterialTheme.colorScheme.onSurfaceVariant
            score.percentage >= 80 -> StatusOk
            score.percentage >= 50 -> Color(0xFFFFA000) // Âmbar
            else -> WasteColor
        },
        animationSpec = tween(600),
        label = "score_color"
    )

    // O número conta de 0 até o valor e a barra cresce junto, com a MESMA
    // variável — por isso nunca ficam fora de sincronia. Sem isso a tela abria
    // com "87%" e a barra já cheia: um número grande aparecendo do nada.
    val subida = remember { Animatable(0f) }
    LaunchedEffect(score.percentage, score.isInsufficient) {
        if (score.isInsufficient) {
            subida.snapTo(0f)
            return@LaunchedEffect
        }
        subida.snapTo(0f)
        subida.animateTo(1f, tween(durationMillis = 900, easing = LinearOutSlowInEasing))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    // "Score" era palavra de banco, não de casa. O título agora
                    // diz o que o número responde — e o número só fica porque o
                    // dono do produto decidiu assim (RETOMADA §8.2).
                    "Quanto você aproveitou",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                val trendIcon = when (score.trend) {
                    ScoreTrend.UP -> Icons.Default.ArrowDropUp
                    ScoreTrend.DOWN -> Icons.Default.ArrowDropDown
                    ScoreTrend.STABLE -> Icons.Default.TrendingFlat
                }
                val trendColor = when (score.trend) {
                    ScoreTrend.UP -> StatusOk
                    ScoreTrend.DOWN -> WasteColor
                    ScoreTrend.STABLE -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                if (!score.isInsufficient) {
                    Icon(trendIcon, contentDescription = null, tint = trendColor, modifier = Modifier.size(22.dp))
                }
            }

            if (score.isInsufficient) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(18.dp))
                    Text(
                        // Nada de cobrança: não é a pessoa que está devendo
                        // registros ao app. O app é que ainda não tem o que dizer.
                        "Ainda tenho poucos registros para falar disso com honestidade (${score.sampleSize} de 3). Não precisa fazer nada — isso se completa com o tempo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${(score.percentage * subida.value).roundToInt()}%",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = scoreColor
                    )
                    Text(
                        "nos últimos 30 dias",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Barra de progresso — cresce do zero junto com o número
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(subida.value * score.percentage / 100f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(scoreColor.copy(alpha = 0.7f), scoreColor)
                                )
                            )
                    )
                }

                // A Carta não aceita número sem explicação: o % só aparece
                // acompanhado da frase que diz o que ele quer dizer na vida.
                Text(
                    "De cada 100 itens que saíram da despensa, ${(score.percentage * subida.value).roundToInt()} foram aproveitados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Comentário — sem culpa. Se algo se perdeu, o erro é meu, que
                // avisei tarde; nunca um defeito de quem usa o app.
                val tip = when {
                    score.percentage >= 80 -> "Quase nada se perdeu. Muito bem."
                    score.percentage >= 60 -> "A maior parte foi aproveitada."
                    score.percentage >= 40 -> "Parte se perdeu. Nada que você tenha feito errado."
                    // Antes: "A culpa foi minha, que avisei tarde". A ideia era tirar a
                    // culpa de quem usa, mas o resultado ainda soava a repreensão.
                    // Agora é um convite, no plural, sem apontar dedo.
                    else -> "Alguns alimentos acabaram se perdendo. " +
                        "Vamos tentar aproveitar melhor da próxima vez?"
                }
                Text(
                    tip,
                    style = MaterialTheme.typography.bodySmall,
                    color = scoreColor.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ─────────────── Gráfico de Barras ───────────────
@Composable
private fun MonthlyBarChart(trends: List<MonthlyTrend>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Histórico Mensal",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Legenda
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(color = MaterialTheme.colorScheme.primary, label = "Consumido")
                LegendDot(color = WasteColor, label = "Desperdiçado")
            }

            val maxVal = trends.maxOf { max(it.consumedAmount, it.wastedAmount) }.takeIf { it > 0.0 } ?: 1.0
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

            // Toque numa barra para ver os valores daquele mês. Sem isso, a barra
            // só mostra "aconteceu alguma coisa" — não quanto.
            var mesEscolhido by remember(trends) { mutableStateOf<Int?>(null) }

            // As barras sobem do chão, uma depois da outra. A do mês atual sobe
            // por último, que é onde o olho deve terminar.
            var apareceu by remember { mutableStateOf(false) }
            LaunchedEffect(trends) {
                apareceu = false
                apareceu = true
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Mais altura útil: com 160dp as barras dos meses pequenos
                    // viravam um risco e "outubro cortado" era o que se via.
                    .height(200.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                trends.forEachIndexed { indice, trend ->
                    val consumedFraction = (trend.consumedAmount / maxVal).toFloat()
                    val wastedFraction = (trend.wastedAmount / maxVal).toFloat()
                    val isCurrent = trend.isCurrentMonth
                    val totalMonth = trend.consumedAmount + trend.wastedAmount
                    val escolhido = mesEscolhido == indice

                    val subiu by animateFloatAsState(
                        targetValue = if (apareceu) 1f else 0f,
                        animationSpec = tween(
                            durationMillis = 520,
                            delayMillis = indice * 70,
                            easing = LinearOutSlowInEasing
                        ),
                        label = "barraMes$indice"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { mesEscolhido = if (escolhido) null else indice }
                    ) {
                        // Valor no topo (R$)
                        if (totalMonth > 0.0) {
                            Text(
                                text = "R$${totalMonth.toInt()}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }

                        // Barras lado a lado: consumido + desperdiçado
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Barra consumido
                            val cHeight = alturaDaBarra(150f * consumedFraction, subiu)
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .height(cHeight.dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (isCurrent) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                    )
                            )
                            // Barra desperdiçado
                            val wHeight = alturaDaBarra(150f * wastedFraction, subiu)
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .height(wHeight.dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (isCurrent) WasteColor
                                        else WasteColor.copy(alpha = 0.45f)
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = trend.monthLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Detalhe do mês tocado (ou a dica de que dá para tocar).
            val detalhe = mesEscolhido?.let { indice ->
                val mes = trends.getOrNull(indice)
                if (mes == null) null else {
                    "${mes.monthLabel}: " +
                        "${currencyFormat.format(mes.consumedAmount)} consumido · " +
                        "${currencyFormat.format(mes.wastedAmount)} perdido"
                }
            }
            Text(
                text = detalhe ?: "Toque numa barra para ver os valores do mês. " +
                    "Verde é o que foi consumido; vermelho é o que se perdeu.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Altura de uma barra do gráfico enquanto ela sobe.
 *
 * Em progresso 0 a barra tem 0dp (o mês começa vazio, não com um risco). No fim
 * do movimento, mês sem valor ganha os 2dp de sempre, senão a coluna some e
 * parece que aquele mês não existe no gráfico.
 */
private fun alturaDaBarra(alturaFinal: Float, progresso: Float): Float =
    max(2f * progresso, alturaFinal * progresso)

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─────────────── Card de Categoria ───────────────
@Composable
private fun CategoryItemCard(
    category: String,
    count: Int,
    totalAmount: Double,
    percentage: Int,
    isWasted: Boolean
) {
    val accentColor = if (isWasted) WasteColor else StatusOk
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(category),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "$count ${if (isWasted) (if (count == 1) "item perdido" else "itens perdidos") else (if (count == 1) "item consumido" else "itens consumidos")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(totalAmount),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "$percentage% do total",
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun getCategoryIcon(category: String): ImageVector = when (category.lowercase().trim()) {
    "laticínios", "laticinios" -> Icons.Default.Icecream
    "bebidas" -> Icons.Default.LocalDrink
    "limpeza" -> Icons.Default.CleaningServices
    "higiene" -> Icons.Default.Spa
    "carnes" -> Icons.Default.Restaurant
    "despensa" -> Icons.Default.Inventory2
    else -> Icons.Default.Fastfood
}

// ─────────────── Card do Histórico ───────────────
@Composable
private fun HistoryCard(item: ConsumptionHistory) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (item.wasWasted) Icons.Default.DeleteOutline else Icons.Default.Restaurant,
                contentDescription = null,
                tint = if (item.wasWasted) WasteColor else StatusOk,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.productName, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val dateFormatted = SimpleDateFormat("dd/MM", Locale("pt", "BR")).format(Date(item.timestamp))
                Text(
                    "${if (item.wasWasted) "Desperdiçado" else "Consumido"} • $dateFormatted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (item.price > 0.0) {
                Text(
                    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(item.price),
                    fontWeight = FontWeight.Bold,
                    color = if (item.wasWasted) WasteColor else StatusOk
                )
            }
        }
    }
}

// ─────────────── Estado Vazio ───────────────
@Composable
private fun EmptyReportsState(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Sem registros de consumo", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        Text("Ao consumir ou descartar produtos, eles aparecerão aqui.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}

