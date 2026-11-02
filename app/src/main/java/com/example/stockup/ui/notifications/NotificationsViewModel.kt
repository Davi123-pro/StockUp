package com.example.stockup.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockup.data.ProductRepository
import com.example.stockup.data.db.Product
import com.example.stockup.data.db.ShoppingItem
import com.example.stockup.utils.ReadAlertsStore
import com.example.stockup.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AlertType {
    EXPIRED,
    EXPIRING_URGENT,
    EXPIRING_SOON,
    SUGGESTION
}

data class NotificationAlert(
    val id: String,
    val product: Product,
    val title: String,
    val message: String,
    val type: AlertType,
    val timestampFormatted: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class NotificationsUiState(
    val alerts: List<NotificationAlert> = emptyList(),
    val selectedFilter: Int = 0,
    val readIds: Set<String> = emptySet()
) {
    val hasUnread: Boolean get() = alerts.any { it.id !in readIds }
}

class NotificationsViewModel(
    private val repository: ProductRepository,
    private val readStore: ReadAlertsStore? = null
) : ViewModel() {

    // Começa com o que já foi lido nas sessões anteriores. Sem isso, o app esquecia
    // tudo a cada abertura e o sininho voltava a ficar vermelho do zero.
    private val _readIds = MutableStateFlow(readStore?.load() ?: emptySet())
    private val _appOpenTime = System.currentTimeMillis()

    private val _alerts: StateFlow<List<NotificationAlert>> = repository.allProducts
        .map { products ->
            val timeStr = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(_appOpenTime))
            val alerts = mutableListOf<NotificationAlert>()

            val perishableProducts = products.filter { it.hasExpiration && it.expirationDate > 0L }
            val expiredCount = perishableProducts.count { DateUtils.daysUntilExpiration(it.expirationDate) < 0L }

            products.forEach { p ->
                // Alertas de Validade (apenas para produtos com validade ativa)
                if (p.hasExpiration && p.expirationDate > 0L) {
                    val days: Long = DateUtils.daysUntilExpiration(p.expirationDate)

                    // Produto VENCIDO
                    if (days < 0L) {
                        val daysAgo = -days
                        // Tom calmo, sem alarme. O app não sabe se o produto ainda
                        // está bom — quem sabe é quem está com ele na mão. Dizer
                        // "descarte imediatamente para evitar contaminação" assusta
                        // sem informar, e a Carta não permite dar medo.
                        val msg = if (daysAgo == 1L) {
                            "Venceu ontem."
                        } else {
                            "Venceu há $daysAgo dias."
                        }
                        alerts.add(NotificationAlert(
                            id = "expired_${p.id}",
                            product = p,
                            title = p.name,
                            message = msg,
                            type = AlertType.EXPIRED,
                            timestampFormatted = "Hoje às $timeStr",
                            timestampMillis = _appOpenTime
                        ))
                    }

                    // Vence HOJE ou AMANHÃ (urgente)
                    else if (days <= 1L) {
                        val urgMsg = if (days == 0L) "Vence hoje." else "Vence amanhã."
                        alerts.add(NotificationAlert(
                            // O sufixo com os dias é de propósito: é o que faz
                            // "vence amanhã" virar novidade de novo no dia seguinte.
                            id = "expiring_urgent_${p.id}_$days",
                            product = p,
                            title = p.name,
                            message = urgMsg,
                            type = AlertType.EXPIRING_URGENT,
                            timestampFormatted = "Hoje às $timeStr",
                            timestampMillis = _appOpenTime
                        ))
                    }

                    // Vence em 2 a 7 dias
                    else if (days in 2L..7L) {
                        // Sem "Dica:". A Carta diz que o app não ensina a vida para
                        // ninguém — ele informa o que sabe e devolve a decisão.
                        alerts.add(NotificationAlert(
                            id = "expiring_soon_${p.id}",
                            product = p,
                            title = p.name,
                            message = "Vence em $days dias.",
                            type = AlertType.EXPIRING_SOON,
                            timestampFormatted = "Hoje às $timeStr",
                            timestampMillis = _appOpenTime
                        ))
                    }
                }

                // O alerta de "estoque baixo" saiu junto com o campo de estoque
                // mínimo. Produto sem validade agora é acompanhado pela pergunta do
                // dia ("usou?"), que desconta da quantidade sem passar por aqui —
                // pergunta não é aviso, e não deve encher o sino.
            }

            // Sugestões inteligentes
            if (expiredCount >= 3) {
                alerts.add(0, NotificationAlert(
                    id = "suggestion_cleanup",
                    product = products.firstOrNull() ?: return@map alerts,
                    title = "Vários produtos vencidos",
                    message = "São $expiredCount. Vale dar uma olhada.",
                    type = AlertType.SUGGESTION,
                    timestampFormatted = "Sugestão de hoje",
                    timestampMillis = _appOpenTime - 1L
                ))
            }

            if (products.isNotEmpty() && expiredCount == 0) {
                alerts.add(NotificationAlert(
                    id = "suggestion_all_good",
                    product = products.first(),
                    title = "Nada vencendo",
                    // Sem "Continue assim!": o app não dá nota nem elogia.
                    message = "Nenhum produto vencendo por aqui.",
                    type = AlertType.SUGGESTION,
                    timestampFormatted = "Hoje às $timeStr",
                    timestampMillis = _appOpenTime
                ))
            }

            alerts.sortWith(compareBy({ it.type.ordinal }, { it.timestampMillis }))
            alerts
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val uiState: StateFlow<NotificationsUiState> = combine(_alerts, _readIds) { alerts, readIds ->
        NotificationsUiState(alerts = alerts, readIds = readIds)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotificationsUiState()
    )

    fun markAsRead(id: String) {
        _readIds.value = _readIds.value + id
        readStore?.save(_readIds.value)
    }

    fun markAllAsRead() {
        _readIds.value = _alerts.value.map { it.id }.toSet()
        readStore?.save(_readIds.value)
    }

    /**
     * Põe o produto na lista de compras — ou soma na quantidade, se ele já estiver
     * lá. Devolve o total que ficou, para o botão poder mostrar "Na lista · 3".
     */
    fun addProductToShopping(product: Product, aoSomar: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val total = repository.adicionarNaListaSomando(
                name = product.name,
                unit = product.unit,
                category = product.category,
                price = product.price
            )
            aoSomar(total)
        }
    }
}
