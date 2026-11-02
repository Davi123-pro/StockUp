package com.example.stockup

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.stockup.data.ProductRepository
import com.example.stockup.data.db.StockUpDatabase
import com.example.stockup.ui.add.AddProductScreen
import com.example.stockup.ui.add.AddProductViewModel
import com.example.stockup.ui.detail.ProductDetailScreen
import com.example.stockup.ui.detail.ProductDetailViewModel
import com.example.stockup.ui.locations.LocationsScreen
import com.example.stockup.ui.locations.LocationsViewModel
import com.example.stockup.ui.main.MainContainerScreen
import com.example.stockup.ui.notifications.NotificationsScreen
import com.example.stockup.ui.notifications.NotificationsViewModel
import com.example.stockup.ui.onboarding.OnboardingScreen
import com.example.stockup.ui.onboarding.WelcomeScreen
import com.example.stockup.ui.settings.FaqScreen
import com.example.stockup.utils.ReadAlertsStore
import com.example.stockup.utils.UserPreferences

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val userPreferences = remember(context) { UserPreferences(context) }
    val initialKey: NavKey = Splash

    val backStack = rememberNavBackStack(initialKey)

    // Aba selecionada da tela principal. Fica no nível da navegação (e não dentro
    // da MainContainerScreen) porque telas secundárias — Locais, Avisos, Detalhes —
    // tiram a Main da composição. Com o estado aqui, voltar leva a pessoa de volta
    // à aba em que ela estava (Configurações, por exemplo), e não à Despensa.
    var abaSelecionada by rememberSaveable { mutableIntStateOf(0) }

    // Pedido de tour vindo do fim do tutorial. Vive aqui porque a tela principal
    // pode ainda não estar composta quando o tutorial termina.
    var pedirTour by rememberSaveable { mutableStateOf(false) }

    val database = StockUpDatabase.getInstance(context)
    val repository = remember(database) {
        ProductRepository(
            productDao = database.productDao(),
            shoppingDao = database.shoppingDao(),
            historyDao = database.historyDao(),
            locationDao = database.locationDao()
        )
    }

    // Criado uma única vez e compartilhado entre a aba Despensa (badge de não lidas)
    // e a tela de Notificações. Depois do entryDecorators abaixo cada entrada passa a
    // ter seu próprio ViewModelStore, e duas instâncias separadas fariam o badge nunca
    // refletir o que foi lido. O estado de leitura é do app, não de uma tela.
    val notificationsViewModel: NotificationsViewModel = viewModel {
        NotificationsViewModel(
            repository = repository,
            // O "lido" agora sobrevive a fechar o app: fica em SharedPreferences.
            readStore = ReadAlertsStore(context.applicationContext)
        )
    }

    // Permissão de notificação: só DEPOIS do tutorial, nunca na primeira abertura.
    // Pedir de cara é o jeito mais rápido de ganhar um "não" — e no Android 13+ sem
    // essa permissão o app não consegue avisar sobre a validade.
    val pedirPermissaoNotificacao = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Concedida ou negada, o app segue igual. Não insistimos.
    }

    fun pedirPermissaoDeNotificacaoSePreciso() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (userPreferences.hasAskedNotificationPermission) return
        val jaConcedida = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (jaConcedida) return
        userPreferences.hasAskedNotificationPermission = true
        pedirPermissaoNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
        entryDecorators = listOf(
            // Mantém o decorator padrão do Navigation 3 1.0.1 (estado de UI por entrada)…
            rememberSaveableStateHolderNavEntryDecorator(),
            // …e adiciona o escopo de ViewModel por entrada. Sem ele, todo viewModel {}
            // fica preso no ViewModelStore da Activity: nunca é limpo ao sair da pilha,
            // e o formulário reabria com os dados da vez anterior.
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Splash> {
                // Antes esta entrada desenhava uma SEGUNDA tela de abertura, com
                // gradiente escuro fixo (que ficava errado no tema claro) e 2,6
                // segundos de espera — depois do splash que o próprio Android já
                // mostra. Duas aberturas seguidas, uma delas escura no modo claro.
                //
                // O Android 12+ já desenha a abertura do app. Esta entrada passou a
                // existir só para decidir o destino, sem tela nenhuma.
                LaunchedEffect(Unit) {
                    val destination: NavKey = if (!userPreferences.hasCompletedOnboarding) Welcome else Main
                    backStack.clear()
                    backStack.add(destination)
                }
            }
            entry<Welcome> {
                WelcomeScreen(
                    onStartTutorial = {
                        backStack.add(Onboarding)
                    },
                    onSkipToApp = {
                        userPreferences.hasCompletedOnboarding = true
                        backStack.clear()
                        backStack.add(Main)
                        // Só agora, depois do tutorial, é justo pedir.
                        pedirPermissaoDeNotificacaoSePreciso()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<Onboarding> {
                OnboardingScreen(
                    onFinishOnboarding = {
                        userPreferences.hasCompletedOnboarding = true
                        // Emenda: quem acabou de ver o tutorial entra direto no tour
                        // do app. Antes ele voltava para a despensa e o tour só
                        // aparecia se o "modo de teste" já estivesse ligado.
                        pedirTour = true
                        backStack.clear()
                        backStack.add(Main)
                        // Só agora, depois do tutorial, é justo pedir.
                        pedirPermissaoDeNotificacaoSePreciso()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<Main> {
                val notifState by notificationsViewModel.uiState.collectAsStateWithLifecycle()
                MainContainerScreen(
                    repository = repository,
                    onAddProduct = { backStack.add(AddProduct(null)) },
                    onProductClick = { id -> backStack.add(ProductDetail(id)) },
                    onEditProduct = { id -> backStack.add(AddProduct(id)) },
                    onNavigateToLocations = { backStack.add(LocationsManagement) },
                    onNavigateToNotifications = { backStack.add(Notifications) },
                    onShowOnboarding = { backStack.add(Onboarding) },
                    onNavigateToFaq = { backStack.add(Faq) },
                    hasUnreadNotifications = notifState.hasUnread,
                    // A aba vive aqui: quando uma tela secundária (ex.: "Onde guardo
                    // as coisas") abre, a Main sai da composição — se o estado
                    // morasse dentro dela, voltar cairia sempre na Despensa.
                    selectedTab = abaSelecionada,
                    onSelectTab = { abaSelecionada = it },
                    pedirTourDeFora = pedirTour,
                    onTourPedidoConsumido = { pedirTour = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<AddProduct> { key ->
                val addViewModel: AddProductViewModel = viewModel(key = "add_${key.productId}") {
                    AddProductViewModel(key.productId, repository)
                }
                AddProductScreen(
                    viewModel = addViewModel,
                    onBack = { backStack.removeLastOrNull() },
                    onSaved = { backStack.removeLastOrNull() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<ProductDetail> { key ->
                val detailViewModel: ProductDetailViewModel = viewModel(key = "detail_${key.productId}") {
                    ProductDetailViewModel(key.productId, repository)
                }
                ProductDetailScreen(
                    viewModel = detailViewModel,
                    onBack = { backStack.removeLastOrNull() },
                    onEdit = { id -> backStack.add(AddProduct(id)) },
                    onDeleted = { backStack.removeLastOrNull() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<LocationsManagement> {
                val locationsViewModel: LocationsViewModel = viewModel {
                    LocationsViewModel(repository)
                }
                LocationsScreen(
                    viewModel = locationsViewModel,
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<Notifications> {
                NotificationsScreen(
                    viewModel = notificationsViewModel,
                    onBack = { backStack.removeLastOrNull() },
                    onProductClick = { id -> backStack.add(ProductDetail(id)) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<Faq> {
                FaqScreen(
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    )
}
