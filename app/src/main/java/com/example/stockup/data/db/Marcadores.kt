package com.example.stockup.data.db

/**
 * Marca, dentro de `notes`, um produto criado pelo "Mover Rápido" de Compras que
 * ainda precisa de validade/local/foto.
 *
 * Fica num lugar só porque três partes do app dependem dela: o Compras (que grava),
 * a Home (que mostra o selo) e o formulário (que precisa apagar ao salvar).
 */
const val MARCADOR_DETALHES_PENDENTES = "⚠️ Pendente detalhes (vindo de Compras)"

/**
 * Marca o produto FALSO que existe só durante o tour.
 *
 * Quem abre o app pela primeira vez não tem produto nenhum — e o passo do tour que
 * ensina o "Acabou" não teria o que apontar. Este produto é criado quando o tour
 * começa e apagado quando ele termina. Se algo der errado no meio, ele é limpo na
 * próxima abertura do app, para nunca virar um produto de verdade na despensa.
 */
const val MARCADOR_EXEMPLO_TOUR = "exemplo-do-tour"

/**
 * O produto ainda está esperando o usuário completar os detalhes?
 *
 * Antes a Home fazia `notes.contains("Pendente")` direto no código, o que dava dois
 * problemas: qualquer pessoa que escrevesse "pendente" nas anotações ganhava um
 * aviso falso, e — pior — o marcador nunca saía, então o aviso continuava aparecendo
 * mesmo depois de o usuário preencher tudo.
 */
fun Product.temDetalhesPendentes(): Boolean =
    notes.contains(MARCADOR_DETALHES_PENDENTES, ignoreCase = true)
