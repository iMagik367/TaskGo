package com.taskgoapp.taskgo

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.taskgoapp.taskgo.core.navigation.BottomNavigationBar
import com.taskgoapp.taskgo.core.theme.TaskGoTheme
import com.taskgoapp.taskgo.core.model.AccountType
import com.taskgoapp.taskgo.navigation.TaskGoNavGraph
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.taskgoapp.taskgo.core.sync.SyncWorker
import com.taskgoapp.taskgo.core.work.AccountChangeProcessorWorker

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainContent()
        }
        // Agendar sincronização periódica (a cada 15 minutos)
        // Aguardar para garantir que o HiltWorkerFactory está pronto
        scheduleWorkers()
    }
    
    override fun onResume() {
        super.onResume()
        // Login apenas no primeiro acesso - removida lógica de biometria
    }
    
    private fun scheduleWorkers() {
        // Aguardar para garantir que Hilt está totalmente inicializado
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                val workManager = WorkManager.getInstance(this)
                
                // Verificar se WorkManager está configurado corretamente
                android.util.Log.d("MainActivity", "Agendando Workers...")
                
                // Agendar sincronização periódica (a cada 15 minutos)
                val syncWork = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                    .addTag("taskgo_periodic_sync")
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()
                
                workManager.enqueueUniquePeriodicWork(
                    "taskgo_periodic_sync",
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncWork
                )
                android.util.Log.d("MainActivity", "✅ SyncWorker agendado com sucesso")
                
                // Agendar processamento de mudanças de conta (diariamente)
                val accountChangeWork = PeriodicWorkRequestBuilder<AccountChangeProcessorWorker>(1, TimeUnit.DAYS)
                    .addTag("account_change_processor")
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()
                
                workManager.enqueueUniquePeriodicWork(
                    "account_change_processor",
                    ExistingPeriodicWorkPolicy.KEEP,
                    accountChangeWork
                )
                android.util.Log.d("MainActivity", "✅ AccountChangeProcessorWorker agendado com sucesso")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "❌ Falha ao agendar Workers: ${e.message}", e)
                android.util.Log.e("MainActivity", "Tipo de erro: ${e.javaClass.simpleName}")
                android.util.Log.e("MainActivity", "Stack trace completo: ${e.stackTraceToString()}")
                
                // Tentar novamente após mais tempo se for erro de inicialização
                if (e is java.lang.NoSuchMethodException || 
                    e.message?.contains("Worker", ignoreCase = true) == true) {
                    android.util.Log.w("MainActivity", "Tentando agendar novamente após 5 segundos...")
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        scheduleWorkers()
                    }, 5000)
                }
            }
        }, 5000) // Aguardar 5 segundos para garantir que Hilt está pronto
    }
}

@Composable
private fun MainContent() {
    val viewModel: MainActivityViewModel = hiltViewModel()
    val authRepository = viewModel.authRepository
    val preferencesManager = viewModel.preferencesManager
    
    // Observar idioma selecionado
    val languageCode by preferencesManager.language.collectAsState(initial = "pt")
    
    TaskGoTheme(languageCode = languageCode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: "splash"
            
            // Processar deep links quando o app é aberto via link
            val activity = androidx.compose.ui.platform.LocalContext.current as? MainActivity
            LaunchedEffect(Unit) {
                activity?.intent?.data?.let { uri ->
                    android.util.Log.d("MainActivity", "Processando deep link: $uri")
                    
                    // Extrair postId do URI
                    when {
                        uri.scheme == "https" &&
                                (uri.host == "taskgo.app" || uri.host == "taskgoapps.com") &&
                                uri.path?.startsWith("/post/") == true -> {
                            val postId = uri.pathSegments.getOrNull(1)
                            if (postId != null) {
                                android.util.Log.d("MainActivity", "Navegando para post: $postId (host=${uri.host})")
                                navController.navigate("post/$postId") {
                                    // Garantir que o usuário esteja autenticado antes de navegar
                                    popUpTo("splash") { inclusive = false }
                                }
                            }
                        }
                        uri.scheme == "taskgo" && uri.host == "post" -> {
                            val postId = uri.pathSegments.firstOrNull()
                            if (postId != null) {
                                android.util.Log.d("MainActivity", "Navegando para post (scheme customizado): $postId")
                                navController.navigate("post/$postId") {
                                    popUpTo("splash") { inclusive = false }
                                }
                            }
                        }
                    }
                }
            }
            
            // Observar mudanças no estado de autenticação do Firebase
            val authState by authRepository.observeAuthState().collectAsState(initial = authRepository.getCurrentUser())
            val isAuthenticated = authState != null
            
            // Rotas principais onde a barra DEVE aparecer
            val mainRoutes = listOf(
                "home",
                "services",
                "products",
                "feed",
                "profile"
            )
            
            // Rotas onde a barra NUNCA deve aparecer (autenticação e splash)
            val authRoutes = listOf(
                "splash",
                "login_person",
                "login_store",
                "forgot_password",
                "signup",
                "signup_success",
                "identity_verification",
                "facial_verification",
                "two_factor_auth"
            )
            
            // Estado local para controlar quando mostrar a barra
            // Só mostrar quando estiver em uma rota principal E autenticado E não estiver em splash
            var showBottomBar by remember { mutableStateOf(false) }
            
            // Observar mudanças na rota e autenticação
            LaunchedEffect(currentRoute, isAuthenticated) {
                // Mostrar barra quando estiver em uma rota principal, autenticado, e não estiver em splash/login
                showBottomBar = mainRoutes.contains(currentRoute) && 
                               isAuthenticated &&
                               !authRoutes.contains(currentRoute) &&
                               !currentRoute.startsWith("signup") &&
                               !currentRoute.startsWith("login") &&
                               !currentRoute.startsWith("identity") &&
                               !currentRoute.startsWith("facial") &&
                               !currentRoute.startsWith("two_factor")
            }
            
            // Garantir que a barra só apareça quando todas as condições forem atendidas
            val shouldShowBottomBar = showBottomBar && 
                                     mainRoutes.contains(currentRoute) && 
                                     isAuthenticated &&
                                     currentRoute != "splash" &&
                                     !authRoutes.contains(currentRoute) &&
                                     !currentRoute.startsWith("signup") &&
                                     !currentRoute.startsWith("login") &&
                                     !currentRoute.startsWith("identity") &&
                                     !currentRoute.startsWith("facial") &&
                                     !currentRoute.startsWith("two_factor")
            
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Conteúdo principal
                    TaskGoNavGraph(
                        navController = navController,
                        modifier = Modifier.weight(1f)
                    )

                    // Barra de navegação inferior - aparece em todas as telas principais (home, services, products, messages, profile)
                    // mas NUNCA durante splash ou telas de autenticação
                    // Obter accountType para usar label dinâmico "Loja" vs "Produtos"
                    val accountType by viewModel.accountType.collectAsState()
                    
                    if (shouldShowBottomBar) {
                        BottomNavigationBar(
                            currentRoute = currentRoute,
                            accountType = accountType, // Passar accountType para label dinâmico
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
