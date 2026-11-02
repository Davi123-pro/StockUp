package com.example.stockup.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.example.stockup.R

/**
 * Símbolo do app: uma caixa de despensa.
 *
 * Usa o MESMO desenho do ícone da notificação (`ic_notification`), para o app ter
 * um símbolo só. Antes a barra de baixo usava `Icons.Default.Inventory2` do Material,
 * uma caixa genérica sem relação com a marca, enquanto a notificação já usava o
 * desenho certo — dois desenhos para a mesma ideia.
 *
 * O drawable é uma silhueta branca com furos (a fenda da gaveta), então tingir
 * funciona em qualquer cor do tema.
 */
@Composable
fun IconeDespensa(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    Icon(
        painter = painterResource(R.drawable.ic_notification),
        contentDescription = null,
        modifier = modifier,
        tint = tint
    )
}
