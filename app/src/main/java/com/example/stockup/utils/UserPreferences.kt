package com.example.stockup.utils

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("stockup_prefs", Context.MODE_PRIVATE)

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING, value).apply()

    /** Já pedimos a permissão de notificação uma vez? Serve para não ficar insistindo. */
    var hasAskedNotificationPermission: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_ASKED, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_ASKED, value).apply()

    /** O usuário já viu o tour da tela inicial? */
    var hasSeenTour: Boolean
        get() = prefs.getBoolean(KEY_TOUR, false)
        set(value) = prefs.edit().putBoolean(KEY_TOUR, value).apply()

    /**
     * Modo de teste: mostra o tour e a introdução.
     *
     * Por que existe: essas duas telas são ferramenta de desenvolvimento e de
     * demonstração, não recurso de quem usa o app. Ficam escondidas por padrão e
     * aparecem com 5 toques na linha da versão, em Configurações — sem criar uma
     * segunda variante do aplicativo.
     */
    var modoDeTeste: Boolean
        get() = prefs.getBoolean(KEY_MODO_TESTE, false)
        set(value) = prefs.edit().putBoolean(KEY_MODO_TESTE, value).apply()

    // ─────────────── Avisos inteligentes ───────────────
    //
    // O app estima quando cada produto vai acabar (média de consumo da casa) e só
    // avisa perto da hora. Nada de intervalo fixo. E existe UM botão para desligar
    // tudo, porque aviso que não se desliga é aviso que se desinstala.
    //
    // A HORA do aviso não é configurável de propósito: o app observa a hora em que
    // a pessoa costuma abrir a despensa e usa essa mesma hora — nunca de madrugada.

    /** Quantas pessoas moram na casa. Só serve de ponto de partida para a estimativa. */
    var moradores: Int
        get() = prefs.getInt(KEY_MORADORES, 1).coerceIn(1, 10)
        set(value) = prefs.edit().putInt(KEY_MORADORES, value.coerceIn(1, 10)).apply()

    /** O usuário quer receber avisos no celular? */
    var avisosAtivos: Boolean
        get() = prefs.getBoolean(KEY_AVISOS_ATIVOS, true)
        set(value) = prefs.edit().putBoolean(KEY_AVISOS_ATIVOS, value).apply()

    /**
     * Conta quantas vezes o app foi aberto em cada hora do dia.
     *
     * É assim que o app descobre "a melhor hora" para falar com a pessoa, sem
     * perguntar nada. Guardado como texto ("0,0,0,1,0…") para não criar 24 chaves.
     */
    private var usosPorHora: IntArray
        get() = prefs.getString(KEY_USOS, null)
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.takeIf { it.size == 24 }
            ?.toIntArray()
            ?: IntArray(24)
        set(value) = prefs.edit().putString(KEY_USOS, value.joinToString(",")).apply()

    /** Registra uma abertura do app na hora atual. Chamado em MainActivity. */
    fun registrarUso(agora: Long = System.currentTimeMillis()) {
        val hora = java.util.Calendar.getInstance().apply { timeInMillis = agora }
            .get(java.util.Calendar.HOUR_OF_DAY)
        val contagem = usosPorHora
        contagem[hora] = contagem[hora] + 1
        usosPorHora = contagem
    }

    /** A hora que o app escolheu para avisar (6h–23h). */
    fun horaDoAviso(): Int = com.example.stockup.domain.EscolhaDeHorario.melhorHora(usosPorHora)

    /** Dia (AAAAMMDD) do último aviso de consumo — garante no máximo um por dia. */
    var ultimoAvisoConsumoDia: Int
        get() = prefs.getInt(KEY_ULTIMO_AVISO_CONSUMO, 0)
        set(value) = prefs.edit().putInt(KEY_ULTIMO_AVISO_CONSUMO, value).apply()

    /** Dia do último aviso de validade, para não somar dois avisos no mesmo dia. */
    var ultimoAvisoValidadeDia: Int
        get() = prefs.getInt(KEY_ULTIMO_AVISO_VALIDADE, 0)
        set(value) = prefs.edit().putInt(KEY_ULTIMO_AVISO_VALIDADE, value).apply()

    companion object {
        private const val KEY_ONBOARDING = "completed_onboarding"
        private const val KEY_NOTIF_ASKED = "asked_notification_permission"
        private const val KEY_TOUR = "seen_home_tour"
        private const val KEY_MODO_TESTE = "modo_de_teste"

        private const val KEY_MORADORES = "moradores"
        private const val KEY_AVISOS_ATIVOS = "avisos_ativos"
        private const val KEY_USOS = "usos_por_hora"
        private const val KEY_ULTIMO_AVISO_CONSUMO = "ultimo_aviso_consumo_dia"
        private const val KEY_ULTIMO_AVISO_VALIDADE = "ultimo_aviso_validade_dia"
    }
}
