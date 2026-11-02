package com.example.stockup.domain

import java.text.Normalizer
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Uma vez que o produto foi consumido.
 *
 * Sai de dois lugares: do "Acabou"/"Joguei fora" (quando o produto inteiro sai da
 * despensa) e do "Atualizar quantidade" (quando a pessoa diz quanto sobrou).
 */
data class ObservacaoConsumo(
    /** Nome normalizado + unidade. É a "identidade" do produto entre compras. */
    val chave: String,
    /** Quanto foi consumido (na unidade do produto). */
    val quantidade: Double,
    /** Em quantos dias isso aconteceu. */
    val dias: Double,
    /** Quando foi registrado (ms). */
    val quando: Long
)

/** De onde veio a previsão. A tela pode (e deve) dizer isso em palavras. */
enum class OrigemEstimativa {
    /** Já existe histórico suficiente deste produto nesta casa. */
    HISTORICO,

    /** Ainda não há histórico: usa o padrão da categoria e os moradores. */
    CATEGORIA
}

data class Estimativa(
    /** Quantos dias ainda devem durar, a partir de agora. */
    val diasRestantes: Double,
    /** Litros/quilos/unidades por dia. */
    val mediaDiaria: Double,
    /** Quantas observações entraram na conta. */
    val amostras: Int,
    val origem: OrigemEstimativa
)

/**
 * O motor de consumo: matemática simples, sem IA, como o dono pediu.
 *
 * Regras que valem aqui dentro (e estão testadas):
 * 1. **Histórico manda.** Com duas ou mais observações, a previsão é só histórico.
 * 2. **Média ponderada, não o último registro.** Um uso atípico (a visita que bebeu
 *    meio litro de leite) não pode definir a próxima previsão. As observações mais
 *    recentes pesam mais, mas as antigas continuam contando.
 * 3. **Sem histórico, usa categoria + moradores.** Carne dura menos que arroz, e
 *    três moradores gastam mais que um. É só o começo: o histórico substitui.
 * 4. **Nada de "acabou" automático.** O motor só estima; quem decide é a pessoa.
 */
object ConsumptionEngine {

    /** Quantas observações entram na média (as mais recentes). */
    const val MAX_AMOSTRAS = 5

    /**
     * Padrão de duração por categoria, para UMA pessoa, em dias.
     *
     * Números escolhidos pela vida real de uma despensa brasileira: carne e peixe
     * saem rápido, laticínio dura uma semana, grão dura meses. Servem só como
     * ponto de partida — o primeiro "acabou" já começa a corrigir isso.
     */
    fun duracaoPadraoPorCategoria(categoria: String): Double = when (normalizar(categoria)) {
        "carnes", "peixes" -> 3.0
        "laticinios" -> 7.0
        "verduras", "legumes", "frutas", "hortifruti" -> 5.0
        "bebidas" -> 10.0
        "padaria", "congelados" -> 7.0
        "limpeza", "higiene", "papelaria" -> 60.0
        "graos", "despensa", "mercearia" -> 30.0
        else -> 14.0
    }

    /**
     * Média diária de consumo a partir das observações.
     *
     * Peso exponencial: a mais recente pesa 1, a anterior 0,5, depois 0,25…
     * Média simples daria o mesmo peso para o mês passado e para ontem.
     */
    fun mediaDiaria(observacoes: List<ObservacaoConsumo>): Double {
        val validas = observacoes
            .filter { it.quantidade > 0.0 && it.dias > 0.0 }
            .sortedByDescending { it.quando }
            .take(MAX_AMOSTRAS)

        if (validas.isEmpty()) return 0.0

        var somaPesos = 0.0
        var somaConsumoPorDia = 0.0
        validas.forEachIndexed { indice, obs ->
            val peso = Math.pow(0.5, indice.toDouble())
            somaPesos += peso
            somaConsumoPorDia += peso * (obs.quantidade / obs.dias)
        }
        return if (somaPesos > 0.0) somaConsumoPorDia / somaPesos else 0.0
    }

    /**
     * Estima quantos dias ainda duram [quantidadeAtual].
     *
     * @param moradores só é usado quando ainda não há histórico (é referência
     * inicial, como o dono pediu: "o histórico deve ter maior influência").
     */
    fun estimar(
        quantidadeAtual: Double,
        categoria: String,
        moradores: Int,
        observacoes: List<ObservacaoConsumo>
    ): Estimativa {
        if (quantidadeAtual <= 0.0) {
            return Estimativa(diasRestantes = 0.0, mediaDiaria = 0.0, amostras = 0, origem = OrigemEstimativa.CATEGORIA)
        }

        // Quantas observações realmente entram na conta — não quantas existem.
        // O texto da notificação diz "pelo seu consumo (Nx)", então o N precisa
        // ser o mesmo número que gerou a média.
        val validas = observacoes.count { it.quantidade > 0.0 && it.dias > 0.0 }
        val amostras = minOf(MAX_AMOSTRAS, validas)
        val mediaHistorico = mediaDiaria(observacoes)

        if (amostras > 0 && mediaHistorico > 0.0) {
            return Estimativa(
                diasRestantes = quantidadeAtual / mediaHistorico,
                mediaDiaria = mediaHistorico,
                amostras = amostras,
                origem = OrigemEstimativa.HISTORICO
            )
        }

        // Sem histórico: divide o padrão da categoria pelo número de moradores.
        // Uma casa de 3 pessoas queima o leite em ~1/3 do tempo de uma de 1.
        val pessoas = max(1, moradores)
        val duracaoInicial = max(1.0, duracaoPadraoPorCategoria(categoria) / pessoas)
        val mediaInicial = quantidadeAtual / duracaoInicial
        return Estimativa(
            diasRestantes = duracaoInicial,
            mediaDiaria = mediaInicial,
            amostras = 0,
            origem = OrigemEstimativa.CATEGORIA
        )
    }

    /**
     * Chave que identifica o produto entre uma compra e outra.
     *
     * Nome sem acento, minúsculo, + unidade. "Leite Cemil 1L" e "leite cemil 1 l"
     * são o mesmo produto; "Leite Cemil" em un e em L são coisas diferentes.
     */
    fun chaveDoProduto(nome: String, unidade: String): String =
        "${normalizar(nome)}|${normalizar(unidade).ifBlank { "un" }}"

    /** Minúsculas, sem acento e sem espaço dobrado. */
    fun normalizar(texto: String): String =
        Normalizer.normalize(texto.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("\\s+"), " ")

    /**
     * Quando avisar.
     *
     * Avisa quando falta ~1 dia para acabar (ou quando já passou da previsão).
     * O "1 dia antes" existe porque avisar depois é tarde: a pessoa já está sem.
     */
    fun diasParaAvisar(diasRestantes: Double): Double = 1.0

    /** Ela já deveria ter sido avisada? */
    fun deveAvisar(diasRestantes: Double): Boolean = diasRestantes <= diasParaAvisar(diasRestantes)

    /**
     * Intervalo mínimo (em dias) entre dois avisos do MESMO produto.
     *
     * Cresce com o número de avisos ignorados: 2, 4, 8… até 14. É o que impede o
     * app de virar aquele alarme que a pessoa aprende a ignorar.
     */
    fun intervaloMinimoDias(avisosIgnorados: Int): Int =
        when {
            avisosIgnorados <= 0 -> 1
            avisosIgnorados == 1 -> 2
            avisosIgnorados == 2 -> 4
            avisosIgnorados == 3 -> 8
            else -> 14
        }

    /** Quantos dias inteiros faltam — é o que a notificação escreve. */
    fun diasInteiros(dias: Double): Long = max(0L, dias.roundToLong())
}
