package com.example.stockup.utils

import android.content.Context

/**
 * Lembra quais avisos já foram lidos.
 *
 * Antes isso vivia só na memória do ViewModel: a cada abertura do app, todos os
 * avisos voltavam como "não lidos" e o sininho ficava vermelho de novo. É o que o
 * usuário descreveu como "as notificações voltam de novo" — e ficava pior depois de
 * restaurar uma cópia, porque os produtos voltam com os mesmos ids e os mesmos avisos.
 *
 * O estado de leitura é do APP, não de uma tela.
 */
class ReadAlertsStore(context: Context) {

    private val prefs = context.getSharedPreferences("stockup_alerts", Context.MODE_PRIVATE)

    fun load(): Set<String> =
        prefs.getStringSet(KEY, emptySet())?.toSet() ?: emptySet()

    fun save(ids: Set<String>) {
        prefs.edit().putStringSet(KEY, ids).apply()
    }

    companion object {
        // v2: os ids dos avisos agora mudam quando a situação muda (ex.:
        // "expiring_urgent_3_1" ao passar a vencer amanhã). Sem trocar a chave, tudo
        // que já tinha sido lido continuaria lido para sempre — e o sininho nunca
        // mais acenderia.
        private const val KEY = "read_alert_ids_v2"
    }
}
