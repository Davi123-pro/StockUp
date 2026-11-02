package com.example.stockup.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Aviso simples, sem botão nenhum. Mesma aparência da barra de desfazer.
 *
 * Existe para o app não usar o Snackbar padrão do Material, que no tema claro é um
 * retângulo escuro destoando de todo o resto — era o "retângulo escuro" que aparecia
 * ao restaurar uma cópia e ao adicionar item à lista de compras.
 */
@Composable
fun BarraAviso(
    mensagem: String,
    modifier: Modifier = Modifier
) {
    BarraDesfazer(mensagem = mensagem, modifier = modifier)
}

/**
 * Barra de aviso com ação de desfazer.
 *
 * Esta barra NÃO flutua por cima da lista: quem a coloca na tela é o `Column` da
 * Home, e ela ocupa espaço de verdade — a lista encolhe para abrir lugar.
 *
 * Por que isso importa: flutuando, a área dela virava um botão "Acabou" vivo assim
 * que os 8 segundos expiravam. Um toque atrasado no "Desfazer" caía no produto de
 * trás e apagava o errado. Empurrando, o que está embaixo nunca fica debaixo dela.
 *
 * A animação de entrada/saída também é de quem usa (`AnimatedVisibility` com
 * `expandVertically`). Aqui a barra é só a aparência.
 */
@Composable
fun BarraDesfazer(
    mensagem: String,
    modifier: Modifier = Modifier,
    /** Nulo esconde o botão: usado quando não há mais nada para desfazer. */
    onAcao: (() -> Unit)? = null,
    rotuloAcao: String = "Desfazer",
    /** Atalho opcional de correção, ex.: "Foi jogado fora". */
    rotuloExtra: String? = null,
    onExtra: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            // Antes era `inverseSurface`: no tema escuro isso dava um cartão claro
            // (bom), mas no tema claro dava um cartão escuro — destoava de tudo.
            // Superfície com variante acompanha os dois temas.
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Sem ícone. A Carta diz "palavras, não desenhos": uma setinha de 24dp
            // não ensinava nada que o botão "Desfazer" já não diga — e ela roubava
            // a atenção do que importa.
            Text(
                text = mensagem,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // A ação em botão DE VERDADE, largo e alto: é o que a pessoa precisa
            // achar rápido, e um text-button pequeno no canto não é alvo de dedo.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (rotuloExtra != null && onExtra != null) {
                    TextButton(
                        onClick = onExtra,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = rotuloExtra,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (onAcao != null) {
                    BotaoAmigo(
                        onClick = onAcao,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = rotuloAcao,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
