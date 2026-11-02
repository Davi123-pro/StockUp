package com.example.stockup.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.stockup.utils.EmailDeFeedback

data class FaqItem(
    val id: Int,
    val category: String,
    val question: String,
    val answer: String
)

/**
 * Normaliza para busca: minúsculo, sem acento e sem pontuação.
 *
 * Existe porque quem digita "validade" com pressa escreve "validade", "vence",
 * "vencimento" — e a busca antiga exigia a palavra exata com o acento certo.
 */
private fun normalizar(texto: String): String =
    java.text.Normalizer.normalize(texto.lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("[^a-z0-9\\s]"), " ")

/**
 * Sinônimos que a pessoa usa e o app não escreve.
 *
 * Se ela procura "apagar", tem de achar a pergunta que diz "excluir". Sem isso, a
 * busca parece burra justamente em quem mais precisa dela.
 */
private val sinonimos = mapOf(
    "apagar" to listOf("excluir", "deletar", "remover", "limpar"),
    "deletar" to listOf("excluir", "apagar", "remover"),
    "excluir" to listOf("apagar", "deletar", "remover"),
    "remover" to listOf("excluir", "apagar", "deletar"),
    "vence" to listOf("validade", "vencimento", "vencer"),
    "vencimento" to listOf("validade", "vence", "vencer"),
    "acabou" to listOf("consumir", "usar", "acaba"),
    "aviso" to listOf("notificacao", "alerta", "avisos"),
    "notificacao" to listOf("aviso", "alerta"),
    "foto" to listOf("imagem", "camera", "galeria"),
    "codigo" to listOf("barras", "barcode", "leitor"),
    "internet" to listOf("offline", "wifi", "sinal"),
    "copiar" to listOf("copia", "backup", "restaurar", "salvar"),
    "preco" to listOf("valor", "gasto", "custo", "dinheiro")
)

/** Palavras que não ajudam a buscar. */
private val palavrasVazias = setOf(
    "a", "o", "e", "de", "da", "do", "das", "dos", "em", "no", "na", "nos", "nas",
    "um", "uma", "para", "pra", "por", "com", "que", "se", "meu", "minha", "meus",
    "como", "onde", "quando", "qual", "quais", "eu", "voce", "ele", "ela", "isso"
)

/** Quebra a frase em termos de busca, já com sinônimos. */
private fun termos(texto: String): List<String> {
    val base = normalizar(texto).split(" ").filter { it.length >= 2 && it !in palavrasVazias }
    return base + base.flatMap { sinonimos[it].orEmpty() }
}

/**
 * Quanto esta pergunta tem a ver com a busca.
 *
 * Peso maior para o título do que para o corpo: quem digita "apagar produto"
 * quer a pergunta cujo título fala disso, não a que cita de passagem.
 */
private fun pontuar(item: FaqItem, termosDaBusca: List<String>): Int {
    if (termosDaBusca.isEmpty()) return 0
    val titulo = normalizar(item.question)
    val corpo = normalizar(item.answer)
    return termosDaBusca.sumOf { termo ->
        when {
            titulo.contains(termo) -> 3
            corpo.contains(termo) -> 1
            else -> 0
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todas") }
    val expandedStates = remember { mutableStateMapOf<Int, Boolean>() }

    val faqList = remember {
        listOf(
            FaqItem(
                id = 1,
                category = "Funcionamento",
                question = "O que é o StockUp e para que ele serve?",
                answer = "É uma memória a mais para a sua casa. Ele serve para três coisas: " +
                    "saber o que está para vencer antes que vença, saber o que está faltando " +
                    "para você não comprar duas vezes, e saber onde cada coisa está (geladeira, " +
                    "armário, freezer). Não é planilha, não é rede social e não vende nada."
            ),
            FaqItem(
                id = 2,
                category = "Funcionamento",
                question = "Preciso de internet para usar o StockUp?",
                answer = "Para quase tudo, não. A despensa, a lista de compras, o histórico e os " +
                    "relatórios ficam guardados no próprio celular e funcionam sem sinal. Só uma " +
                    "coisa precisa de internet: quando você bipa o código de um produto novo e o " +
                    "app vai procurar o nome dele."
            ),
            FaqItem(
                id = 3,
                category = "Funcionamento",
                question = "Preciso criar conta ou senha?",
                answer = "Não. Nada de cadastro, e-mail ou senha. Você abre o aplicativo e usa. " +
                    "Ninguém vai te pedir nada para começar."
            ),
            FaqItem(
                id = 4,
                category = "Validade",
                question = "E se eu não souber a validade do produto?",
                answer = "Pode cadastrar sem validade. Nesse caso eu uso uma estimativa pela " +
                    "categoria — carne dura menos, arroz dura meses — e vou aprendendo com o " +
                    "seu consumo de verdade: a cada produto que acaba, a previsão fica mais " +
                    "parecida com a sua casa. Se a embalagem tiver data, informe: ela sempre " +
                    "vale mais que qualquer estimativa minha."
            ),
            FaqItem(
                id = 5,
                category = "Validade",
                question = "O que é \"não perecível\"?",
                answer = "É o que dura meses na despensa: arroz, feijão, óleo, macarrão, " +
                    "enlatados. Marcando assim eu paro de vigiar validade — mas continuo " +
                    "acompanhando a quantidade, porque essas coisas também acabam (e é aí que " +
                    "eu aviso antes de faltar)."
            ),
            FaqItem(
                id = 6,
                category = "Cadastro",
                question = "De onde vem a foto do produto?",
                answer = "De dois jeitos: você tira na hora, ou o app aproveita a foto que encontra " +
                    "junto com o código de barras. Se não tiver foto, aparece o desenho de uma " +
                    "caixa no lugar."
            ),
            FaqItem(
                id = 7,
                category = "Cadastro",
                question = "Não achei o produto pelo código de barras. E agora?",
                answer = "É só digitar o nome e salvar. Da próxima vez, o app já reconhece esse " +
                    "código e preenche sozinho."
            ),
            FaqItem(
                id = 11,
                category = "Compras",
                question = "O que é o \"Mover Rápido\" da lista de compras?",
                answer = "No mercado ninguém tem tempo de preencher validade e foto. O Mover Rápido " +
                    "leva os itens comprados para a despensa com um toque. Ele marca que faltam " +
                    "detalhes, e você completa em casa com calma."
            ),
            FaqItem(
                id = 12,
                category = "Compras",
                question = "Para que serve o preço do produto?",
                answer = "Para os seus relatórios de gastos. É com ele que o app calcula quanto você " +
                    "aproveitou e quanto foi para o lixo no mês."
            ),
            FaqItem(
                id = 13,
                category = "Segurança",
                question = "Meus dados vão para algum lugar?",
                answer = "Ficam no seu celular. A única coisa que sai pela internet é o número do " +
                    "código de barras, quando você bipa um produto novo — só o número, nada sobre " +
                    "você. Não existe conta, servidor nem nuvem."
            ),
            FaqItem(
                id = 14,
                category = "Segurança",
                question = "Como faço para não perder os dados?",
                answer = "Em Configurações, toque em \"Salvar cópia\". A cópia fica guardada dentro " +
                    "do próprio app e volta com dois toques em \"Restaurar\". Importante: se você " +
                    "desinstalar o app, a cópia vai junto — por isso ainda não existe jeito de " +
                    "levar os dados para fora do celular."
            ),
            FaqItem(
                id = 15,
                category = "Celular",
                question = "A letra está pequena. Como faço para aumentar?",
                answer = "Quem aumenta é o próprio celular, e vale para tudo: WhatsApp, telefone, " +
                    "banco. Abra os Ajustes do aparelho, procure \"Tela\" e depois \"Tamanho da " +
                    "fonte\". Escolha um tamanho maior — o StockUp cresce junto, sem cortar nada."
            ),
            FaqItem(
                id = 16,
                category = "Funcionamento",
                question = "Como apagar vários produtos de uma vez?",
                answer = "Na tela da Despensa, toque no ícone de lista no topo — o mesmo que fica " +
                    "ao lado da lupa. Os produtos ganham uma caixinha: marque os que quer apagar " +
                    "e toque na lixeira. O app pergunta o que aconteceu com eles (usei, joguei " +
                    "fora, ou só apagar, sem entrar no relatório). Dá para desfazer na barra " +
                    "que aparece logo depois."
            ),
            FaqItem(
                id = 17,
                category = "Funcionamento",
                question = "Como vejo os detalhes do produto na minha despensa?",
                answer = "Toque no cartão do produto, na tela da Despensa. Abre a ficha dele com " +
                    "foto, quantidade, local, validade, preço e as suas anotações. É ali também " +
                    "que você ajusta a quantidade com os botões + e −, corrige a validade pelo " +
                    "lápis e apaga pela lixeira — e o app pergunta se foi usado, jogado fora ou " +
                    "só apagar, para o relatório ficar certo."
            )
        )
    }

    val categories = listOf("Todas", "Funcionamento", "Segurança", "Validade", "Cadastro", "Compras", "Celular")

    val filteredList = if (searchQuery.isBlank()) {
        faqList.filter { item ->
            selectedCategory == "Todas" || item.category.equals(selectedCategory, ignoreCase = true)
        }
    } else {
        // Busca com pontuação: título pesa 3, corpo pesa 1, sinônimos entram juntos.
        val termosDaBusca = termos(searchQuery).distinct()
        faqList
            .map { item -> item to pontuar(item, termosDaBusca) }
            .filter { (item, nota) ->
                nota > 0 && (selectedCategory == "Todas" || item.category.equals(selectedCategory, ignoreCase = true))
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    // Quando nada casa: as três perguntas mais próximas servem de "quis dizer...".
    val sugestoes = remember(searchQuery, selectedCategory) {
        if (filteredList.isEmpty() && searchQuery.isNotBlank()) {
            val termosDaBusca = termos(searchQuery).distinct()
            faqList
                .map { item -> item to pontuar(item, termosDaBusca) }
                .sortedByDescending { it.second }
                .filter { it.second > 0 }
                .take(3)
                .map { it.first }
        } else {
            emptyList()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Central de Ajuda & Dúvidas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Campo de Busca
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Pesquisar dúvida...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            // Filtro por Categoria em Carrossel
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = CircleShape,
                        // Chip do Material tem ~32dp: abaixo do alvo mínimo de toque.
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Text(
                // Plural de verdade: "1 perguntas" era o que aparecia antes.
                text = if (filteredList.size == 1) {
                    "1 pergunta encontrada"
                } else {
                    "${filteredList.size} perguntas encontradas"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            // Nada encontrado: em vez de deixar a pessoa no vazio, sugere o que
            // chegou perto e oferece mandar a dúvida direto para o dono do app.
            if (filteredList.isEmpty()) {
                SemRespostaCard(
                    consulta = searchQuery,
                    sugestoes = sugestoes,
                    aoAbrirSugestao = { item ->
                        searchQuery = ""
                        selectedCategory = "Todas"
                        expandedStates[item.id] = true
                    }
                )
            }

            // Lista de Cards Expansíveis (Accordions)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredList.forEach { item ->
                    val isExpanded = expandedStates[item.id] ?: false
                    val arrowRotation by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f, label = "arrowRotate")

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { expandedStates[item.id] = !isExpanded }
                            .border(
                                1.dp,
                                if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(14.dp)
                            ),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.category.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.question,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = if (isExpanded) "Recolher" else "Expandir",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .rotate(arrowRotation)
                                )
                            }

                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = item.answer,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

    }
}

/**
 * "Não achei minha dúvida": sugere o que chegou perto e oferece escrever para o dono.
 *
 * Por que existe: a Central de Ajuda cobre o que foi previsto. Quando a dúvida é
 * nova, o pior desfecho é a pessoa desistir — aqui ela manda a pergunta com um
 * toque, já com o assunto preenchido, e essa dúvida vira melhoria do próprio FAQ.
 */
@Composable
private fun SemRespostaCard(
    consulta: String,
    sugestoes: List<FaqItem>,
    aoAbrirSugestao: (FaqItem) -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Não achei essa dúvida",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                if (consulta.isBlank()) {
                    "Tente escrever com outras palavras — por exemplo: apagar, validade, foto."
                } else {
                    "Procurei por \"$consulta\" e não encontrei. Posso mandar a sua pergunta " +
                        "para quem fez o app, e ela vai virar uma pergunta daqui."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (sugestoes.isNotEmpty()) {
                Text(
                    "Talvez seja uma destas:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                sugestoes.forEach { item ->
                    Text(
                        "• ${item.question}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { aoAbrirSugestao(item) }
                            .padding(vertical = 4.dp)
                    )
                }
            }

            Button(
                onClick = {
                    val assunto = if (consulta.isBlank()) {
                        "Dúvida sobre o StockUp"
                    } else {
                        "Dúvida sobre o StockUp: $consulta"
                    }
                    EmailDeFeedback.abrir(
                        context = context,
                        assunto = assunto,
                        corpo = "Minha dúvida é sobre: " + consulta.ifBlank { "(escreva aqui)" } +
                            "\n\nO que eu tentei fazer:\n\n\n" +
                            "—\nEnviado pela Central de Ajuda do StockUp"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mandar minha dúvida", fontWeight = FontWeight.Bold)
            }
        }
    }
}
