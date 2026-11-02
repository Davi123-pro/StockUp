package com.example.stockup.domain

/**
 * Escolhe a hora do aviso diário sem perguntar nada ao usuário.
 *
 * Por que não uma hora fixa: uma casa acorda às 6h, outra às 10h. Avisar às 9h em
 * ponto acerta uma e erra a outra. Em vez de mais uma configuração para a pessoa
 * preencher, o app observa a hora em que ela costuma abrir a despensa e usa a
 * mesma hora para falar com ela.
 *
 * Regra do dono: **nada entre meia-noite e 5 da manhã** — ninguém quer o celular
 * apitando de madrugada.
 */
object EscolhaDeHorario {

    /** Primeira hora permitida (6h). */
    const val PRIMEIRA_HORA = 6

    /** Última hora permitida (23h). */
    const val ULTIMA_HORA = 23

    /** Quando ainda não há costume observado: 9h, começo de manhã. */
    const val HORA_PADRAO = 9

    /**
     * A hora com mais uso dentro da janela permitida.
     *
     * Empate: a mais cedo (quem abre às 7h e às 20h na mesma quantidade recebe às
     * 7h — aviso cedo dá tempo de agir).
     * Fora da janela ou sem dados: [HORA_PADRAO].
     */
    fun melhorHora(usosPorHora: IntArray): Int {
        if (usosPorHora.size < 24) return HORA_PADRAO

        var melhorHora = HORA_PADRAO
        var melhorContagem = 0
        for (hora in PRIMEIRA_HORA..ULTIMA_HORA) {
            val contagem = usosPorHora[hora].coerceAtLeast(0)
            if (contagem > melhorContagem) {
                melhorContagem = contagem
                melhorHora = hora
            }
        }
        return if (melhorContagem > 0) melhorHora else HORA_PADRAO
    }

    /** A hora está dentro da janela em que o app pode avisar? */
    fun horaPermitida(hora: Int): Boolean = hora in PRIMEIRA_HORA..ULTIMA_HORA
}
