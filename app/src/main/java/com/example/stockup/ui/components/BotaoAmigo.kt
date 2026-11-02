package com.example.stockup.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

/**
 * Botão com resposta ao toque.
 *
 * Por que existe: quem tem tremor ou pouca sensibilidade no dedo não tem certeza de
 * que o toque entrou — e aí toca de novo por ansiedade. O botão encolhe ~4,5%
 * enquanto o dedo está pressionado e volta com uma mola curta. É a confirmação de
 * "eu senti", não enfeite.
 *
 * A altura mínima de 48dp vem do contentPadding (16dp em cima + 16dp embaixo + o
 * texto), respeitando o alvo mínimo de toque.
 */
@Composable
fun BotaoAmigo(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cores: ButtonColors = ButtonDefaults.buttonColors(),
    conteudo: @Composable RowScope.() -> Unit
) {
    val interacao = remember { MutableInteractionSource() }
    val pressionado by interacao.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (pressionado) 0.955f else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessHigh),
        label = "escalaDoToque"
    )

    Button(
        onClick = onClick,
        modifier = modifier.scale(escala),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = cores,
        interactionSource = interacao,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        content = conteudo
    )
}

/** Versão de contorno, com a mesma resposta ao toque. */
@Composable
fun BotaoAmigoContorno(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cores: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    conteudo: @Composable RowScope.() -> Unit
) {
    val interacao = remember { MutableInteractionSource() }
    val pressionado by interacao.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (pressionado) 0.955f else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessHigh),
        label = "escalaDoToqueContorno"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.scale(escala),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = cores,
        interactionSource = interacao,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        content = conteudo
    )
}
