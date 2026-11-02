package com.example.stockup.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockup.data.ProductRepository
import com.example.stockup.data.db.ConsumptionHistory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

// ─────────────── Enum de período ───────────────
enum class ReportPeriod(val label: String, val months: Int) {
    // "Geral" era abstrato: não se sabe se é o ano, a vida toda ou o que sobrou.
    // "Todo o período" diz exatamente o que é. E "Últimos 3 meses" deixa claro que
    // a janela anda com o calendário.
    THIS_MONTH("Este mês", 1),
    THREE_MONTHS("Últimos 3 meses", 3),
    ALL_TIME("Todo o período", -1)
}

// ─────────────── Models de dados ───────────────
data class MonthlyTrend(
    val monthLabel: String,
    val consumedAmount: Double,
    val wastedAmount: Double,
    val isCurrentMonth: Boolean
)

data class CategoryReportItem(
    val category: String,
    val count: Int,
    val totalAmount: Double,
    val percentage: Int
)

data class EfficiencyScore(
    /** Percentual calculado na janela dos últimos 30 dias com dados suficientes */
    val percentage: Int,
    /** Quantos itens foram registrados nos últimos 30 dias */
    val sampleSize: Int,
    /** true quando ainda não há dados suficientes (< 3 eventos) */
    val isInsufficient: Boolean,
    /** Direção da tendência vs. janela anterior (30-60 dias atrás) */
    val trend: ScoreTrend
)

enum class ScoreTrend { UP, DOWN, STABLE }

data class ReportsUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.THIS_MONTH,
    val totalConsumedCount: Int = 0,
    val totalWastedCount: Int = 0,
    val totalWastedValue: Double = 0.0,
    val totalSavedValue: Double = 0.0,
    val efficiencyScore: EfficiencyScore = EfficiencyScore(100, 0, true, ScoreTrend.STABLE),
    val monthlyTrends: List<MonthlyTrend> = emptyList(),
    val topCategoriesConsumed: List<CategoryReportItem> = emptyList(),
    val topCategoriesWasted: List<CategoryReportItem> = emptyList(),
    val historyList: List<ConsumptionHistory> = emptyList()
)

class ReportsViewModel(private val repository: ProductRepository) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.THIS_MONTH)

    val uiState: StateFlow<ReportsUiState> = combine(
        repository.allHistory,
        _selectedPeriod
    ) { allHistory, period ->
        buildUiState(allHistory, period)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ReportsUiState()
    )

    fun selectPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    // ─────────────── Score de Aproveitamento (janela móvel 30 dias) ───────────────
    private fun buildEfficiencyScore(allHistory: List<ConsumptionHistory>): EfficiencyScore {
        val now = System.currentTimeMillis()
        val ms30 = 30L * 24 * 60 * 60 * 1000
        val ms60 = 60L * 24 * 60 * 60 * 1000

        // Janela atual: últimos 30 dias
        val window30 = allHistory.filter { it.timestamp >= now - ms30 }
        // Janela anterior: 30-60 dias atrás
        val window60to30 = allHistory.filter { it.timestamp in (now - ms60)..(now - ms30) }

        fun scoreOf(items: List<ConsumptionHistory>): Int {
            if (items.isEmpty()) return 100
            val consumed = items.count { !it.wasWasted }
            return ((consumed.toDouble() / items.size) * 100).toInt()
        }

        val currentScore = scoreOf(window30)
        val previousScore = scoreOf(window60to30)

        val trend = when {
            window60to30.size < 3 -> ScoreTrend.STABLE
            currentScore > previousScore + 3 -> ScoreTrend.UP
            currentScore < previousScore - 3 -> ScoreTrend.DOWN
            else -> ScoreTrend.STABLE
        }

        return EfficiencyScore(
            percentage = currentScore,
            sampleSize = window30.size,
            isInsufficient = window30.size < 3,
            trend = trend
        )
    }

    // ─────────────── Builder principal do UiState ───────────────
    private fun buildUiState(allHistory: List<ConsumptionHistory>, period: ReportPeriod): ReportsUiState {
        val cal = Calendar.getInstance()

        // Filtro por período selecionado
        val filteredHistory: List<ConsumptionHistory> = when (period) {
            ReportPeriod.THIS_MONTH -> {
                val y = cal.get(Calendar.YEAR)
                val m = cal.get(Calendar.MONTH)
                allHistory.filter { item ->
                    val c = Calendar.getInstance().apply { timeInMillis = item.timestamp }
                    c.get(Calendar.YEAR) == y && c.get(Calendar.MONTH) == m
                }
            }
            ReportPeriod.THREE_MONTHS -> {
                val cutoff = Calendar.getInstance().apply { add(Calendar.MONTH, -3) }.timeInMillis
                allHistory.filter { it.timestamp >= cutoff }
            }
            ReportPeriod.ALL_TIME -> allHistory
        }

        val consumed = filteredHistory.filter { !it.wasWasted }
        val wasted = filteredHistory.filter { it.wasWasted }
        val totalSavedVal = consumed.sumOf { it.price }
        val totalWastedVal = wasted.sumOf { it.price }

        // Gráfico de tendência (6 meses, independente do filtro)
        val shortMonths = DateFormatSymbols.getInstance(Locale("pt", "BR")).shortMonths
        val barCount = when (period) {
            ReportPeriod.THIS_MONTH -> 6
            ReportPeriod.THREE_MONTHS -> 6
            ReportPeriod.ALL_TIME -> 6
        }
        val trends = (barCount - 1 downTo 0).map { monthsAgo ->
            val monthCal = Calendar.getInstance().apply { add(Calendar.MONTH, -monthsAgo) }
            val y = monthCal.get(Calendar.YEAR)
            val m = monthCal.get(Calendar.MONTH)
            val rawLabel = shortMonths.getOrNull(m)?.replace(".", "") ?: ""
            val label = rawLabel.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString()
            }
            val itemsInMonth = allHistory.filter { item ->
                val c = Calendar.getInstance().apply { timeInMillis = item.timestamp }
                c.get(Calendar.YEAR) == y && c.get(Calendar.MONTH) == m
            }
            MonthlyTrend(
                monthLabel = label,
                consumedAmount = itemsInMonth.filter { !it.wasWasted }.sumOf { it.price },
                wastedAmount = itemsInMonth.filter { it.wasWasted }.sumOf { it.price },
                isCurrentMonth = monthsAgo == 0
            )
        }

        // Categorias
        val topConsumed = consumed
            .groupBy { it.category.ifBlank { "Despensa" } }
            .map { (cat, list) ->
                val catTotal = list.sumOf { it.price }
                val perc = if (totalSavedVal > 0.0) ((catTotal / totalSavedVal) * 100).toInt() else 0
                CategoryReportItem(cat, list.size, catTotal, perc)
            }
            .sortedByDescending { it.totalAmount }

        val topWasted = wasted
            .groupBy { it.category.ifBlank { "Despensa" } }
            .map { (cat, list) ->
                val catTotal = list.sumOf { it.price }
                val perc = if (totalWastedVal > 0.0) ((catTotal / totalWastedVal) * 100).toInt() else 0
                CategoryReportItem(cat, list.size, catTotal, perc)
            }
            .sortedByDescending { it.totalAmount }

        return ReportsUiState(
            selectedPeriod = period,
            totalConsumedCount = consumed.size,
            totalWastedCount = wasted.size,
            totalWastedValue = totalWastedVal,
            totalSavedValue = totalSavedVal,
            efficiencyScore = buildEfficiencyScore(allHistory),
            monthlyTrends = trends,
            topCategoriesConsumed = topConsumed,
            topCategoriesWasted = topWasted,
            historyList = filteredHistory.sortedByDescending { it.timestamp }
        )
    }
}
