package com.example.stockup.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stockup.R

data class TutorialStep(
    val stepNumber: Int,
    val titlePrefix: String,
    val highlightWord: String,
    val description: String,
    val icon: ImageVector,
    val tipBadge: String,
    val demonstrationText: String
)

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = remember {
        listOf(
            TutorialStep(
                stepNumber = 1,
                titlePrefix = "Cadastrar é ",
                highlightWord = "fácil",
                description = "Toque no \"+\" e aponte a câmera para o código de barras. Se o app " +
                    "não conhecer o produto, é só escrever o nome uma vez — ele aprende e não pede " +
                    "de novo. Formulário comprido não existe: é o nome e pronto.",
                icon = Icons.Default.QrCodeScanner,
                tipBadge = "Pilar 1 · Fácil",
                demonstrationText = "Ninguém precisa de manual para cadastrar o primeiro produto."
            ),
            TutorialStep(
                stepNumber = 2,
                titlePrefix = "O que vence e o que ",
                highlightWord = "não vence",
                description = "Comida, bebida e remédio têm data de validade. Arroz, feijão, " +
                    "óleo e macarrão duram meses — marque \"Não vence\" e o app acompanha só a " +
                    "quantidade. Você não precisa configurar nada: é só dizer o que é.",
                icon = Icons.Default.AllInclusive,
                tipBadge = "Sem configuração",
                demonstrationText = "Uma escolha de cada produto. Nada de ajustar números."
            ),
            TutorialStep(
                stepNumber = 3,
                titlePrefix = "Quando acaba, é ",
                highlightWord = "um toque",
                description = "Cada produto tem o botão \"Acabou\". Um toque e ele sai da lista — " +
                    "sem abrir menu, sem procurar o produto. Se tocar sem querer, aparece um " +
                    "\"Desfazer\" logo embaixo. E quando o aviso chega no celular, você responde " +
                    "ali mesmo.",
                icon = Icons.Default.NotificationsActive,
                tipBadge = "Pilar 3 · Simples",
                demonstrationText = "Um toque resolve. Tocar errado tem conserto."
            ),
            TutorialStep(
                stepNumber = 4,
                titlePrefix = "Seus dados ficam ",
                highlightWord = "com vocês",
                description = "Funciona sem internet: a despensa, a lista de compras e o histórico " +
                    "ficam no seu celular. Não tem conta, não tem senha e nada é enviado para " +
                    "fora. Em Configurações você guarda uma cópia dentro do próprio app.",
                icon = Icons.Default.Lock,
                tipBadge = "Pilar 2 · Offline e seguro",
                demonstrationText = "Sem cadastro, sem login, sem depender de wi-fi."
            )
        )
    }

    var currentStep by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_stockup_logo),
                        contentDescription = "StockUp",
                        modifier = Modifier.height(28.dp)
                    )
                }

                TextButton(onClick = onFinishOnboarding) {
                    Text(
                        if (currentStep < steps.size - 1) "Pular Tutorial" else "Concluir",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Conteúdo animado do slide do Tutorial
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "tutorial_slide"
            ) { stepIndex ->
                val step = steps[stepIndex]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Badge do Passo
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "PASSO ${step.stepNumber} DE 4 • ${step.tipBadge}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Círculo Ilustrativo Glassmorphic
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = step.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = buildAnnotatedString {
                            append(step.titlePrefix)
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                                append(step.highlightWord)
                            }
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Card de Demonstração / Dica Prática
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = step.demonstrationText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rodapé: Indicadores de página + Botão Próximo / Começar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicadores (Pills)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.indices.forEach { index ->
                        val isSelected = index == currentStep
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (isSelected) 32.dp else 10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (currentStep < steps.size - 1) {
                            currentStep++
                        } else {
                            onFinishOnboarding()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            if (currentStep < steps.size - 1) "Avançar" else "Começar a Usar o StockUp",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (currentStep < steps.size - 1) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}
