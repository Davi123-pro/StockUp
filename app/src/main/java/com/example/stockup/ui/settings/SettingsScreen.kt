package com.example.stockup.ui.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.stockup.data.backup.BackupManager
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import com.example.stockup.ui.components.AreasTour
import com.example.stockup.ui.components.BarraAviso
import com.example.stockup.ui.components.LocalAreasDoTour
import com.example.stockup.ui.components.LocalPassoDoTour
import com.example.stockup.theme.StatusExpired
import com.example.stockup.utils.EmailDeFeedback
import com.example.stockup.utils.UserPreferences
import com.example.stockup.worker.AvisoDeConsumo
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToLocations: () -> Unit,
    onShowOnboarding: () -> Unit = {},
    onShowTour: () -> Unit = {},
    onNavigateToFaq: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showClearPantryDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showCopyList by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val prefsApp = remember(context) { UserPreferences(context) }

    // Modo de teste: libera tour e introdução (5 toques na linha da versão, no fim).
    var modoDeTeste by remember { mutableStateOf(prefsApp.modoDeTeste) }
    var toquesSobre by remember { mutableIntStateOf(0) }

    // Rolagem controlada: o tour precisa levar a tela até o card dos moradores
    // (passo 8), senão o destaque fica fora da parte visível.
    val rolagem = rememberScrollState()
    var alturaDoCardDeAvisos by remember { mutableStateOf(0) }
    val passoDoTour = LocalPassoDoTour.current

    LaunchedEffect(passoDoTour) {
        if (passoDoTour == AreasTour.AVISOS && alturaDoCardDeAvisos > 0) {
            // Uma respiração para o layout do passo anterior assentar antes de rolar.
            kotlinx.coroutines.delay(250)
            rolagem.animateScrollTo(alturaDoCardDeAvisos.coerceAtLeast(0))
        }
    }

    val backupState by viewModel.backupState.collectAsStateWithLifecycle()

    // Escolher um arquivo de cópia que já existe (WhatsApp, Drive, Downloads...).
    // Só para RESTAURAR: o app não grava mais nada fora dele.
    val restoreFromFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { origem ->
        if (origem != null) {
            showCopyList = false
            viewModel.restoreFromUri(context.contentResolver, origem)
        }
    }

    LaunchedEffect(backupState.message) {
        backupState.message?.let { aviso ->
            snackbarHostState.showSnackbar(aviso)
            viewModel.clearBackupMessage()
        }
    }

    if (showCopyList) {
        val opcoes = backupState.localFiles.map { copia ->
            CopyOption(copia.label, "Guardada no aplicativo") {
                showCopyList = false
                viewModel.restoreFromLocal(copia)
            }
        }
        CopyListDialog(
            opcoes = opcoes,
            isScanning = backupState.isScanning,
            onBrowseOther = { restoreFromFileLauncher.launch(arrayOf("*/*")) },
            onDismiss = { showCopyList = false }
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            // Barra própria com a cara do app. O Snackbar padrão é escuro no tema
            // claro e aparecia como um retângulo destoando ao restaurar uma cópia.
            SnackbarHost(snackbarHostState) { dados ->
                BarraAviso(
                    mensagem = dados.visuals.message,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Configurações", fontWeight = FontWeight.Bold) },
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
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rolagem),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Seção de Gerenciamento
            Text(
                "Dados do Aplicativo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Despensa Compartilhada & Locais Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToLocations() }
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.House,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Onde guardo as coisas",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Geladeira, armário, freezer. Você escolhe os lugares.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Tour e Introdução: ferramenta de teste, escondida por padrão.
            //
            // Antes eram dois cards fixos para qualquer usuário. Como o app vai ser
            // publicado, eles ficam atrás do "modo de teste" (5 toques na linha da
            // versão, no fim desta tela) — sem precisar de uma segunda variante do
            // aplicativo só para isso.
            if (modoDeTeste) {
                // Tour guiado sobre a tela inicial
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowTour() }
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Ver o tour da tela inicial (teste)",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Aponta para cada botão na própria tela, um por um.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Apresentação / Onboarding Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowOnboarding() }
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Ver Introdução do App (teste)",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Rever o tutorial e telas de boas-vindas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Feedback Card — abre o e-mail já endereçado, com assunto pronto.
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { EmailDeFeedback.abrir(context) }
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Feedback",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Conte o que você achou do app: elogio, problema ou ideia. Abre o seu e-mail.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Central de Ajuda & Dúvidas Card
            val areasDoTour = LocalAreasDoTour.current
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToFaq() }
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .onGloballyPositioned {
                        areasDoTour?.put(AreasTour.AJUDA, it.boundsInRoot())
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Central de Ajuda & Dúvidas",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Tire dúvidas sobre o app, validade, avisos e compras.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Cópia de Segurança Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Guardar uma cópia dos meus dados",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                // A Carta proíbe prometer o que o app não faz. Com a cópia
                                // morando só dentro do app, "se o celular quebrar" deixou de
                                // ser verdade — então o texto conta o que é de fato.
                                "Guarda os produtos, as compras, o histórico e as fotos " +
                                    "aqui dentro. Se você desinstalar o app, a cópia vai junto.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.salvarCopiaNoApp() },
                            enabled = !backupState.isWorking,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Salvar cópia")
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.refreshLocalFiles()
                                showCopyList = true
                            },
                            enabled = !backupState.isWorking,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Restaurar")
                        }
                    }

                    if (backupState.isWorking) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Um instante...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Avisos inteligentes Card
            //
            // O app estima quando cada produto vai acabar (média de consumo da casa)
            // e só avisa perto da hora. Aqui ficam o botão de desligar tudo e a
            // quantidade de moradores — a única referência que ele tem antes de
            // acumular histórico.
            val prefsAvisos = remember { UserPreferences(context) }
            var avisosAtivos by remember { mutableStateOf(prefsAvisos.avisosAtivos) }
            var moradores by remember { mutableIntStateOf(prefsAvisos.moradores) }
            // Lido aqui (contexto composable); o lambda de posição não é composable.
            val areasDoTourAvisos = LocalAreasDoTour.current

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .onGloballyPositioned {
                        // O tour aponta para cá no passo que pede quantas pessoas
                        // moram na casa — quem pular no começo preenche depois.
                        areasDoTourAvisos?.put(AreasTour.AVISOS, it.boundsInRoot())
                        // Posição dentro da coluna rolável: é o alvo do scroll
                        // automático quando o tour chega neste passo.
                        alturaDoCardDeAvisos = it.positionInParent().y.toInt()
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Avisos no celular",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                // A hora não é escolhida pela pessoa: o app usa a hora
                                // em que ela costuma abrir a despensa, e nunca de madrugada.
                                "Um aviso por dia, e só quando um produto está perto de " +
                                    "acabar. Eu escolho a hora: a mesma em que você costuma " +
                                    "abrir o app, nunca de madrugada.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (avisosAtivos) "Receber avisos" else "Avisos desligados",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = avisosAtivos,
                            onCheckedChange = { ligado ->
                                avisosAtivos = ligado
                                prefsAvisos.avisosAtivos = ligado
                                if (ligado) {
                                    AvisoDeConsumo.reagendar(context)
                                } else {
                                    AvisoDeConsumo.cancelar(context)
                                }
                            }
                        )
                    }

                    // Quantas pessoas moram aqui fica SEMPRE visível: desligar os
                    // avisos não pode esconder uma informação da casa (e some com o
                    // formulário era o defeito relatado pelo dono).
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Quantas pessoas moram aqui?", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Serve só de ponto de partida. Quem manda depois é " +
                                    "o seu consumo de verdade.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    if (moradores > 1) {
                                        moradores -= 1
                                        prefsAvisos.moradores = moradores
                                    }
                                }
                            ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                            Text(
                                "$moradores",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = {
                                    if (moradores < 10) {
                                        moradores += 1
                                        prefsAvisos.moradores = moradores
                                    }
                                }
                            ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                        }
                    }
                }
            }

            // Limpar Despensa Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showClearPantryDialog = true }
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = StatusExpired,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Limpar Despensa",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Excluir todos os produtos atualmente cadastrados.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Zerar Histórico Financeiro Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showClearHistoryDialog = true }
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.RestartAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Zerar Histórico Financeiro",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Apagar todos os registros de consumo e desperdício.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Seção de Informações
            Text(
                "Sobre",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        // Feedback discreto de que o modo de teste está ligado.
                        if (modoDeTeste) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        // Cinco toques na versão ligam/desligam o modo de teste. É o
                        // gesto clássico de "sobre" e não atrapalha ninguém: quem não
                        // sabe, não descobre por acidente.
                        toquesSobre += 1
                        if (toquesSobre >= 5) {
                            toquesSobre = 0
                            modoDeTeste = !modoDeTeste
                            prefsApp.modoDeTeste = modoDeTeste
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            if (modoDeTeste) "StockUp v1.0 · modo de teste" else "StockUp v1.0",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (modoDeTeste) {
                                "Tour e introdução liberados. Cinco toques aqui desligam."
                            } else {
                                "App pessoal e offline de despensa inteligente."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Diálogo Limpar Despensa
        if (showClearPantryDialog) {
            AlertDialog(
                onDismissRequest = { showClearPantryDialog = false },
                title = { Text("Limpar Despensa?", fontWeight = FontWeight.Bold) },
                text = { Text("Esta ação apagará permanentemente todos os produtos salvos no seu estoque.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllProducts()
                            showClearPantryDialog = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Todos os produtos foram removidos.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusExpired)
                    ) {
                        Text("Sim, Limpar", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearPantryDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Diálogo Zerar Histórico
        if (showClearHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showClearHistoryDialog = false },
                title = { Text("Zerar Histórico?", fontWeight = FontWeight.Bold) },
                text = { Text("Esta ação apagará todas as métricas acumuladas de produtos consumidos e jogados fora nos relatórios.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllHistory()
                            showClearHistoryDialog = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Histórico de relatórios resetado.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusExpired)
                    ) {
                        Text("Sim, Zerar", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearHistoryDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }

}

/** Nome sugerido do arquivo de cópia, ex.: stockup-copia-2026-10-13.zip */
private fun nomeDaCopia(): String {
    val data = SimpleDateFormat("yyyy-MM-dd", Locale("pt", "BR")).format(Date())
    return "${BackupManager.SUGGESTED_FILE_NAME}-$data.zip"
}

/**
 * Uma cópia disponível para restaurar.
 *
 * O [hint] existe porque a mesma data pode aparecer duas vezes (a cópia guardada
 * dentro do aplicativo e a que está numa pasta do celular) — duas linhas idênticas
 * seriam indistinguíveis, o mesmo problema dos dois chips "Todos" na Home.
 */
private data class CopyOption(val label: String, val hint: String, val onPick: () -> Unit)

/**
 * Lista simples das cópias disponíveis.
 *
 * Existe para tirar o seletor de arquivos do caminho principal: o usuário vê
 * "Cópia de 13/10/2026", toca, e pronto. O seletor nativo fica só atrás de
 * "Procurar um arquivo", para quem mandou a cópia para o WhatsApp ou o Drive.
 */
@Composable
private fun CopyListDialog(
    opcoes: List<CopyOption>,
    isScanning: Boolean,
    onBrowseOther: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Qual cópia você quer?", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    isScanning && opcoes.isEmpty() -> {
                        Text(
                            "Procurando as cópias...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    opcoes.isEmpty() -> {
                        Text(
                            "Ainda não há nenhuma cópia guardada.\n\n" +
                                "Toque em \"Salvar cópia\" para fazer a primeira.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    else -> {
                        opcoes.forEach { opcao ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { opcao.onPick() },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Restore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            opcao.label,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            opcao.hint,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onBrowseOther) { Text("Procurar um arquivo") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

/** "540" → "09:00". Minuto do dia vira hora escrita, do jeito que se lê no relógio. */
private fun horarioEscrito(minutoDoDia: Int): String =
    String.format(Locale("pt", "BR"), "%02d:%02d", minutoDoDia / 60, minutoDoDia % 60)

/** Uma linha "Primeira pergunta ......... 09:00", que abre o relógio ao tocar. */
@Composable
private fun HorarioDaLinha(
    rotulo: String,
    minutoDoDia: Int,
    aoTocar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { aoTocar() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(rotulo, style = MaterialTheme.typography.bodyMedium)
        Text(
            horarioEscrito(minutoDoDia),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Relógio do sistema para escolher a hora da pergunta.
 *
 * Devolve "minuto do dia" (9:00 = 540) para não guardar fuso nenhum em preferência.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DialogoDeHorario(
    titulo: String,
    minutoDoDia: Int,
    aoConfirmar: (Int) -> Unit,
    aoFechar: () -> Unit
) {
    val estado = rememberTimePickerState(
        initialHour = minutoDoDia / 60,
        initialMinute = minutoDoDia % 60,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(titulo, fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = estado)
            }
        },
        confirmButton = {
            TextButton(onClick = { aoConfirmar(estado.hour * 60 + estado.minute) }) {
                Text("Pronto", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = aoFechar) { Text("Cancelar") }
        }
    )
}

