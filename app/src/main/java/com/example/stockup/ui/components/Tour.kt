package com.example.stockup.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Um ponto do tour: uma área da tela e o que dizer sobre ela. */
data class PassoTour(
    /** Chave da área medida na tela (`onGloballyPositioned`), em coordenadas da raiz. */
    val chave: String,
    /** Em qual aba este passo acontece: 0 Despensa, 1 Compras, 2 Relatórios, 3 Config. */
    val aba: Int,
    val titulo: String,
    val texto: String
)

/**
 * Onde cada alvo está na tela, em coordenadas da RAIZ.
 *
 * Fica num CompositionLocal para qualquer tela registrar o seu alvo sem precisar
 * passar mapas por parâmetro em quatro assinaturas diferentes.
 */
val LocalAreasDoTour = compositionLocalOf<MutableMap<String, Rect>?> { null }

/**
 * Chave do passo do tour que está na tela agora (null quando o tour está parado).
 *
 * Existe para a tela poder se ajustar ao passo: rolar até o alvo, por exemplo.
 * Sem isso, um passo que aponta para um card lá embaixo destacava uma área fora
 * da parte visível — o tour "apontava para o nada".
 */
val LocalPassoDoTour = compositionLocalOf<String?> { null }

object AreasTour {
    const val RESUMO = "resumo"
    const val SINO = "sino"
    const val ACABOU = "acabou"
    const val MAIS = "mais"
    const val ABA_COMPRAS = "aba_compras"
    const val ABA_RELATORIOS = "aba_relatorios"
    const val ABA_CONFIG = "aba_config"
    const val AJUDA = "ajuda"

    /** O card "Avisos no celular" — onde ficam a chave dos avisos e os moradores. */
    const val AVISOS = "avisos"
}

/**
 * Tour guiado sobre a TELA REAL.
 *
 * Por que não cartões de texto: quem tem 70 anos lê o cartão e esquece. Apontar para
 * o botão de verdade, na posição de verdade, ensina o gesto — não a descrição dele.
 *
 * O escuro é desenhado em quatro faixas ao redor do alvo, deixando um buraco
 * iluminado. Sem BlendMode (que se comporta diferente entre versões do Android) e
 * sem risco de apagar a tela inteira por erro de conta.
 */
@Composable
fun TourOverlay(
    passos: List<PassoTour>,
    indice: Int,
    areas: Map<String, Rect>,
    onProximo: () -> Unit,
    onPular: () -> Unit
) {
    if (passos.isEmpty() || indice !in passos.indices) return
    val passo = passos[indice]
    val area = areas[passo.chave]

    val corDoBuraco = MaterialTheme.colorScheme.primary
    val escuro = Color.Black.copy(alpha = 0.68f)
    val ultimo = indice == passos.lastIndex

    // Posição desta sobreposição na raiz — necessária para converter as medidas.
    var origemNaRaiz by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { origemNaRaiz = it.localToRoot(Offset.Zero) }
    ) {
        val alturaTela = maxHeight

        // As áreas vêm em coordenadas da RAIZ, mas esta Canvas começa mais abaixo
        // (o Scaffold recorta a barra de status). Sem descontar essa origem, o buraco
        // aparecia deslocado para baixo — e apontava para o lugar errado.
        val deslocamento = origemNaRaiz

        Canvas(modifier = Modifier.fillMaxSize()) {
            val m = 6.dp.toPx()
            if (area == null) {
                drawRect(escuro)
            } else {
                val esq = (area.left - deslocamento.x - m).coerceAtLeast(0f)
                val topo = (area.top - deslocamento.y - m).coerceAtLeast(0f)
                val dir = (area.right - deslocamento.x + m).coerceAtMost(size.width)
                val base = (area.bottom - deslocamento.y + m).coerceAtMost(size.height)

                // Quatro faixas em volta do alvo. Nada é apagado, só coberto.
                drawRect(escuro, Offset.Zero, Size(size.width, topo))
                drawRect(escuro, Offset(0f, base), Size(size.width, size.height - base))
                drawRect(escuro, Offset(0f, topo), Size(esq, base - topo))
                drawRect(escuro, Offset(dir, topo), Size(size.width - dir, base - topo))

                drawRoundRect(
                    color = corDoBuraco,
                    topLeft = Offset(esq, topo),
                    size = Size(dir - esq, base - topo),
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        // O cartão vai para o lado OPOSTO ao alvo, para nunca cobri-lo.
        //
        // Cuidado com as unidades: a área vem em PIXELS (onGloballyPositioned) e
        // maxHeight vem em DP. Comparar os dois direto dava sempre "alvo embaixo",
        // e o cartão ia para cima justamente quando o alvo estava lá.
        val alturaEmPixels = with(LocalDensity.current) { alturaTela.toPx() }
        val alvoEmbaixo = area != null && area.center.y > alturaEmPixels * 0.55f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = if (alvoEmbaixo) Arrangement.Top else Arrangement.Bottom
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "${indice + 1} de ${passos.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = passo.titulo,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = passo.texto,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sempre há saída. Um tour do qual não se sai é uma armadilha —
                        // e para este público, armadilha é motivo para fechar o app.
                        TextButton(onClick = onPular) {
                            Text("Pular", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        BotaoAmigo(onClick = onProximo) {
                            Text(
                                text = if (ultimo) "Entendi" else "Próximo",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
