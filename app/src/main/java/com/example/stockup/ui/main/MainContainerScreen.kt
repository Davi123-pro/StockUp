package com.example.stockup.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockup.R
import com.example.stockup.data.ConsumoRepository
import com.example.stockup.data.ProductRepository
import com.example.stockup.data.db.StockUpDatabase
import com.example.stockup.ui.components.AreasTour
import com.example.stockup.ui.components.IconeDespensa
import com.example.stockup.ui.components.LocalAreasDoTour
import com.example.stockup.ui.components.LocalPassoDoTour
import com.example.stockup.ui.components.PassoTour
import com.example.stockup.ui.components.TourOverlay
import com.example.stockup.utils.UserPreferences
import com.example.stockup.ui.home.HomeScreen
import com.example.stockup.ui.home.HomeViewModel
import com.example.stockup.ui.reports.ReportsScreen
import com.example.stockup.ui.reports.ReportsViewModel
import com.example.stockup.ui.settings.SettingsScreen
import com.example.stockup.ui.settings.SettingsViewModel
import com.example.stockup.ui.shopping.ShoppingScreen
import com.example.stockup.ui.shopping.ShoppingViewModel

@Composable
fun MainContainerScreen(
    repository: ProductRepository,
    onAddProduct: () -> Unit,
    onProductClick: (Long) -> Unit,
    onEditProduct: (Long) -> Unit,
    onNavigateToLocations: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onShowOnboarding: () -> Unit = {},
    onNavigateToFaq: () -> Unit = {},
    hasUnreadNotifications: Boolean = false,
    /**
     * Aba selecionada — vive FORA daqui (ver Navigation.kt).
     *
     * Antes era um `remember` local: ao abrir "Onde guardo as coisas" esta tela
     * saía da composição, o estado morria junto e, ao voltar pelo botão do sistema,
     * a aba renascia na Despensa em vez de Configurações. Quem abriu as
     * configurações espera voltar para elas.
     */
    selectedTab: Int = 0,
    onSelectTab: (Int) -> Unit = {},
    /** O tutorial acabou de terminar: começa o tour mesmo fora do modo de teste. */
    pedirTourDeFora: Boolean = false,
    onTourPedidoConsumido: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // viewModel {} (e não remember {}) para que sobrevivam à rotação de tela e
    // tenham onCleared() de verdade. Com remember, girar o aparelho descartava
    // busca, filtros e seleção — e o viewModelScope nunca era cancelado.
    // O HomeViewModel recebe o ConsumoStore porque é ele quem registra "usei este
    // produto" — e é desse registro que sai a média que estima quando vai acabar.
    val homeViewModel: HomeViewModel = viewModel {
        val banco = StockUpDatabase.getInstance(context.applicationContext)
        HomeViewModel(repository, ConsumoRepository(banco.consumoDao()))
    }
    val shoppingViewModel: ShoppingViewModel = viewModel { ShoppingViewModel(repository) }
    val reportsViewModel: ReportsViewModel = viewModel { ReportsViewModel(repository) }

    // Tour da tela inicial: é ferramenta de teste/demonstração. Só aparece sozinho
    // no primeiro uso quando o modo de teste está ligado (5 toques na versão, em
    // Configurações). Quem instala o app para usar não vê passeio nenhum.
    val prefs = remember(context) { UserPreferences(context) }
    var pedirTour by remember { mutableStateOf(prefs.modoDeTeste && !prefs.hasSeenTour) }

    // Quem acabou de ver o tutorial entra no tour na hora, independente do modo de
    // teste: é a continuação natural da introdução, não uma ferramenta escondida.
    LaunchedEffect(pedirTourDeFora) {
        if (pedirTourDeFora) pedirTour = true
    }

    // --- Tour guiado -------------------------------------------------------
    // Comandado daqui porque percorre VÁRIAS abas: o passo 5 troca para Compras,
    // o 6 para Relatórios, o 7 para Config. Cada tela registra onde estão os seus
    // alvos (via LocalAreasDoTour); aqui só se decide a ordem e a aba de cada passo.
    val areasDoTour = remember { mutableStateMapOf<String, Rect>() }
    var indiceTour by remember { mutableIntStateOf(0) }
    val passosDoTour = remember {
        listOf(
            PassoTour(
                AreasTour.RESUMO, 0,
                "Como está a sua despensa",
                "Estes três números mudam sozinhos: quantos produtos você tem, quantos estão perto de vencer e quantos já venceram."
            ),
            PassoTour(
                AreasTour.SINO, 0,
                "Os avisos de validade",
                "Quando algo está perto de vencer, aparece um aviso aqui. A bolinha vermelha quer dizer que tem coisa nova para ver."
            ),
            PassoTour(
                AreasTour.ACABOU, 0,
                "Quando o produto acaba",
                "É só tocar em \"Acabou\". Ele sai da lista, e a pergunta se foi usado ou jogado fora vem logo depois — com \"Desfazer\" se você tocar errado."
            ),
            PassoTour(
                AreasTour.MAIS, 0,
                "Cadastrar um produto novo",
                "Toque aqui, aponte a câmera para o código de barras e pronto. Se o app não conhecer, é só escrever o nome uma vez."
            ),
            PassoTour(
                AreasTour.ABA_COMPRAS, 1,
                "A lista de compras",
                "Aqui fica o que você precisa comprar. No mercado, marque o que pegou — e um toque leva tudo para a despensa."
            ),
            PassoTour(
                AreasTour.ABA_RELATORIOS, 2,
                "Os relatórios",
                // O tour prometia "sem julgamento", mas o card mostra um número.
                // Agora ele conta a verdade: o número existe e a culpa é do app
                // quando algo vence sem aviso.
                "Aqui você vê os gastos e o que foi para o lixo. O número mostra quanto se aproveitou de cada 100 itens — e, se algo se perdeu, a culpa é minha, que avisei tarde."
            ),
            PassoTour(
                AreasTour.ABA_CONFIG, 3,
                "As configurações",
                "Aqui você ajusta o app, guarda uma cópia dos seus dados e encontra a ajuda."
            ),
            PassoTour(
                AreasTour.AVISOS, 3,
                "Quantas pessoas moram aqui?",
                "Este número é o ponto de partida para eu estimar quando cada produto vai " +
                    "acabar. Pode preencher depois, com calma — e se desligar os avisos, o " +
                    "número continua aqui."
            ),
            PassoTour(
                AreasTour.AJUDA, 3,
                "Se precisar de ajuda",
                "Toque aqui. A Central de Ajuda responde as dúvidas mais comuns em linguagem simples."
            )
        )
    }

    // Cada passo acontece na sua aba: troca sozinho para o usuário não se perder.
    LaunchedEffect(indiceTour, pedirTour) {
        if (pedirTour) onSelectTab(passosDoTour[indiceTour].aba)
    }

    // Produto de exemplo: existe só durante o tour, para o passo do "Acabou" ter o
    // que apontar — quem abre o app pela primeira vez não tem nada na despensa.
    // Some quando o tour termina, e também numa limpeza de segurança a cada
    // abertura, caso o app tenha sido fechado no meio do passeio.
    LaunchedEffect(pedirTour) {
        if (pedirTour) {
            repository.criarProdutoDeExemplo(
                "android.resource://${context.packageName}/${R.drawable.exemplo_produto}"
            )
        } else {
            repository.removerProdutoDeExemplo()
        }
    }
    val settingsViewModel: SettingsViewModel = viewModel {
        SettingsViewModel(repository, context.applicationContext)
    }

    Box(modifier = Modifier.fillMaxSize()) {
    CompositionLocalProvider(
        LocalAreasDoTour provides areasDoTour,
        // Qual passo do tour está na tela agora. As telas usam isso para se
        // posicionar: o passo dos "moradores", por exemplo, rola até o card,
        // senão o destaque cai fora da parte visível e o tour aponta para o nada.
        LocalPassoDoTour provides if (pedirTour) passosDoTour.getOrNull(indiceTour)?.chave else null
    ) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { onSelectTab(0) },
                    icon = { IconeDespensa() },
                    label = { Text("Despensa") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { onSelectTab(1) },
                    modifier = Modifier.onGloballyPositioned {
                        areasDoTour[AreasTour.ABA_COMPRAS] = it.boundsInRoot()
                    },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                    label = { Text("Compras") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { onSelectTab(2) },
                    modifier = Modifier.onGloballyPositioned {
                        areasDoTour[AreasTour.ABA_RELATORIOS] = it.boundsInRoot()
                    },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                    label = { Text("Relatórios") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { onSelectTab(3) },
                    modifier = Modifier.onGloballyPositioned {
                        areasDoTour[AreasTour.ABA_CONFIG] = it.boundsInRoot()
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Config") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    viewModel = homeViewModel,
                    onAddProduct = onAddProduct,
                    onProductClick = onProductClick,
                    onEditProduct = onEditProduct,
                    onNotificationsClick = onNavigateToNotifications,
                    hasUnreadNotifications = hasUnreadNotifications,
                    modifier = Modifier.fillMaxSize()
                )
                1 -> ShoppingScreen(
                    viewModel = shoppingViewModel,
                    modifier = Modifier.fillMaxSize()
                )
                2 -> ReportsScreen(
                    viewModel = reportsViewModel,
                    modifier = Modifier.fillMaxSize()
                )
                3 -> SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateToLocations = onNavigateToLocations,
                    onShowOnboarding = onShowOnboarding,
                    onShowTour = {
                        indiceTour = 0
                        pedirTour = true
                    },
                    onNavigateToFaq = onNavigateToFaq,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
    }

    // Desenhado por ÚLTIMO para ficar por cima de tudo — inclusive da barra de abas,
    // que é um dos alvos do tour.
    if (pedirTour) {
        TourOverlay(
            passos = passosDoTour,
            indice = indiceTour,
            areas = areasDoTour,
            onProximo = {
                if (indiceTour < passosDoTour.lastIndex) {
                    indiceTour += 1
                } else {
                    indiceTour = 0
                    pedirTour = false
                    prefs.hasSeenTour = true
                    onTourPedidoConsumido()
                    // O tour termina nas Configurações; volta para a tela inicial
                    // para o usuário não ficar "perdido" onde o passeio acabou.
                    onSelectTab(0)
                }
            },
            onPular = {
                indiceTour = 0
                pedirTour = false
                prefs.hasSeenTour = true
                onTourPedidoConsumido()
                onSelectTab(0)
            }
        )
    }
    }
}

