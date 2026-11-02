package com.example.stockup.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.stockup.theme.StatusOk
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Botão para a última ação de um fluxo — a que faz o trabalho acontecer.
 *
 * Por que existe: nestes dois lugares (guardar a compra na despensa) a ação
 * muda a tela inteira e o que estava escrito no botão desaparece junto. Quem
 * tocou fica sem saber se funcionou e tende a tocar de novo.
 *
 * O que ele faz, nesta ordem, e só nesta ordem:
 *
 *   1. **pulso** — o botão cresce ~7% e volta com mola. É o "eu senti".
 *   2. **✓ com palavras** — o texto troca para a confirmação e o botão fica
 *      verde. Fica parado tempo suficiente para ser lido (a Carta proíbe
 *      coisa que aparece e some antes de dar tempo de ler).
 *   3. **só então** o trabalho acontece (`aoConcluir`). Assim a lista de
 *      compras só muda depois que a pessoa já viu que deu certo.
 *
 * Nunca fecha nem muda nada antes do ✓. Se `validar()` devolver `false`, o
 * pulso não começa e o botão continua exatamente como estava.
 *
 * @param texto o rótulo normal do botão.
 * @param textoConcluido o que aparece junto do ✓ (frase curta, em palavras).
 * @param icone ícone opcional à esquerda do rótulo normal.
 * @param validar checagem rápida antes de animar (campos obrigatórios, etc.).
 * @param aoConcluir o trabalho de verdade. Roda depois do ✓.
 */
@Composable
fun BotaoConcluir(
    texto: String,
    textoConcluido: String,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    cores: ButtonColors = ButtonDefaults.buttonColors(),
    icone: ImageVector? = null,
    validar: () -> Boolean = { true },
    aoConcluir: () -> Unit
) {
    val interacao = remember { MutableInteractionSource() }
    val pressionado by interacao.collectIsPressedAsState()
    val haptico = LocalHapticFeedback.current
    val escopo = rememberCoroutineScope()

    val escala = remember { Animatable(1f) }
    var concluido by remember { mutableStateOf(false) }
    var trabalhando by remember { mutableStateOf(false) }

    // Encolhe no toque e volta — a mesma resposta dos outros botões do app.
    val escalaDoToque = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressionado) 0.955f else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessHigh),
        label = "escalaDoToqueConcluir"
    )

    Button(
        onClick = {
            if (trabalhando) return@Button
            if (!validar()) return@Button

            trabalhando = true
            escopo.launch {
                // 1. pulso
                // 4%, não 7%: este botão ocupa a largura toda e, com 7%, o topo
                // do pulso encostava na borda da tela — parecia corte, não pulso.
                escala.animateTo(1.04f, tween(durationMillis = 130, easing = LinearOutSlowInEasing))
                escala.animateTo(1f, spring(dampingRatio = 0.34f, stiffness = Spring.StiffnessMediumLow))

                // 2. ✓ — e o toque vira uma confirmação que se sente no dedo
                concluido = true
                haptico.performHapticFeedback(HapticFeedbackType.LongPress)

                // Tempo de leitura. A Carta pede "sem pressa".
                delay(900)

                // 3. agora sim
                aoConcluir()

                // Se o botão continuar na tela, ele volta ao normal em vez de
                // ficar travado no ✓ para sempre.
                delay(120)
                concluido = false
                trabalhando = false
            }
        },
        modifier = modifier.scale(escalaDoToque.value * escala.value),
        enabled = habilitado,
        shape = RoundedCornerShape(14.dp),
        colors = if (concluido) {
            ButtonDefaults.buttonColors(
                containerColor = StatusOk,
                contentColor = Color.White,
                disabledContainerColor = StatusOk,
                disabledContentColor = Color.White
            )
        } else cores,
        interactionSource = interacao,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
    ) {
        AnimatedContent(
            targetState = concluido,
            transitionSpec = {
                (fadeIn(tween(140)) + scaleIn(initialScale = 0.9f, animationSpec = tween(200)))
                    .togetherWith(fadeOut(tween(90)))
            },
            label = "conteudoDoBotaoConcluir"
        ) { pronto ->
            Row {
                if (pronto) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        textoConcluido,
                        fontWeight = FontWeight.Bold,
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall
                    )
                } else {
                    if (icone != null) {
                        Icon(
                            icone,
                            contentDescription = null,
                            tint = LocalContentColor.current,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        texto,
                        fontWeight = FontWeight.Bold,
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}
