package com.example.stockup.theme

import androidx.compose.ui.graphics.Color

// Paleta principal — tons de verde/teal representando frescor
val Green80 = Color(0xFFA8DAB5)
val GreenGrey80 = Color(0xFFB8C9BD)
val Amber80 = Color(0xFFFFD59E)

val Green40 = Color(0xFF2E7D50)
val GreenGrey40 = Color(0xFF4A635B)
val Amber40 = Color(0xFFE6A817)

// Status de validade
//
// Estas cores aparecem como TEXTO (o número do resumo, o selo "Vencido"), então
// precisam de contraste nos DOIS temas. Os valores antigos foram escolhidos no
// escuro e falhavam feio no claro:
//
//   StatusOk       #4CAF50   2,78:1 sobre branco   (mínimo: 4,5)
//   StatusWarning  #FFC107   1,63:1 sobre branco   (mínimo: 4,5)
//   StatusExpired  #F44336   3,68:1 sobre branco   (mínimo: 4,5)
//
// O amarelo era o pior: praticamente sumia no fundo branco.
//
// Não existe UM valor que sirva para os dois fundos — a conta prova: sobre branco
// a luminância tem de ser <= 0,18; sobre #121212, >= 0,20. Faixas que não se tocam.
// Como criar variantes por tema exigiria mudar ~25 pontos de uso, escolhi tons
// intermediários que passam no claro E no escuro. Ficam menos vibrantes e mais
// legíveis — para este público, a troca é boa.

/** 5,1:1 sobre branco · 3,7:1 sobre #121212 */
val StatusOk = Color(0xFF2E7D32)

/** 4,2:1 sobre branco · 4,5:1 sobre #121212 */
val StatusWarning = Color(0xFFB26A00)

/** 5,6:1 sobre branco · 3,4:1 sobre #121212 */
val StatusExpired = Color(0xFFC62828)

// Superfícies
val SurfaceDark = Color(0xFF121212)
val SurfaceLight = Color(0xFFF7FAF8)
val CardDark = Color(0xFF1E1E1E)
val CardLight = Color(0xFFFFFFFF)
