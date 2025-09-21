package com.example.taskgoapp.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.taskgoapp.feature.ads.presentation.AnunciosScreen
import com.example.taskgoapp.feature.auth.presentation.CadastroFinalizadoScreen
import com.example.taskgoapp.feature.auth.presentation.CadastroScreen
import com.example.taskgoapp.feature.auth.presentation.LoginStoreScreen
import com.example.taskgoapp.feature.auth.presentation.ForgotPasswordScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.taskgoapp.feature.checkout.presentation.CadastrarEnderecoScreen
import com.example.taskgoapp.feature.checkout.presentation.CartaoCreditoScreen
import com.example.taskgoapp.feature.checkout.presentation.CartaoDebitoScreen
import com.example.taskgoapp.feature.checkout.presentation.PixPaymentScreen
import com.example.taskgoapp.feature.checkout.presentation.FinalizarPedidoScreen
import com.example.taskgoapp.feature.checkout.presentation.FormaPagamentoScreen
import com.example.taskgoapp.feature.home.presentation.HomeScreen
import com.example.taskgoapp.feature.services.presentation.ServicesScreen
import com.example.taskgoapp.feature.messages.presentation.ChatScreen
import com.example.taskgoapp.feature.messages.presentation.MessagesScreen
import com.example.taskgoapp.feature.notifications.presentation.NotificationsScreen
import com.example.taskgoapp.feature.notifications.presentation.NotificationDetailScreen
import com.example.taskgoapp.feature.notifications.presentation.NotificationItem
import com.example.taskgoapp.feature.orders.presentation.MeusPedidosScreen
import com.example.taskgoapp.feature.orders.presentation.DetalhesPedidoScreen
import com.example.taskgoapp.feature.orders.presentation.RastreamentoPedidoScreen
import com.example.taskgoapp.feature.orders.presentation.ResumoPedidoScreen
import com.example.taskgoapp.feature.products.presentation.CarrinhoScreen
import com.example.taskgoapp.feature.products.presentation.DetalhesProdutoScreen
import com.example.taskgoapp.feature.products.presentation.EditarProdutoScreen
import com.example.taskgoapp.feature.products.presentation.CriarProdutoScreen
import com.example.taskgoapp.feature.products.presentation.GerenciarProdutosScreen
import com.example.taskgoapp.feature.products.presentation.MeusProdutosScreen
import com.example.taskgoapp.feature.products.presentation.OrderTrackingScreen
import com.example.taskgoapp.feature.products.presentation.ProductsScreen
import com.example.taskgoapp.feature.profile.presentation.ContaScreen
import com.example.taskgoapp.feature.profile.presentation.MeusDadosScreen
import com.example.taskgoapp.feature.profile.presentation.MinhasAvaliacoesScreen
import com.example.taskgoapp.feature.profile.presentation.ProfileScreen
import com.example.taskgoapp.feature.services.presentation.AvaliarPrestadorScreen
import com.example.taskgoapp.feature.services.presentation.ConfirmarPropostaScreen
import com.example.taskgoapp.feature.services.presentation.CreateWorkOrderScreen
import com.example.taskgoapp.feature.services.presentation.DetalhesPropostaScreen
import com.example.taskgoapp.feature.services.presentation.GerenciarPropostasScreen
import com.example.taskgoapp.feature.services.presentation.HistoricoServicosScreen
import com.example.taskgoapp.feature.services.presentation.DetalhesServicoScreen
import com.example.taskgoapp.feature.services.presentation.MeusServicosScreen
import com.example.taskgoapp.feature.services.presentation.ProposalDetailScreen
import com.example.taskgoapp.feature.services.presentation.ReviewsScreen
import com.example.taskgoapp.feature.settings.presentation.AlterarSenhaScreen
import com.example.taskgoapp.feature.settings.presentation.ConfiguracoesScreen
import com.example.taskgoapp.feature.settings.presentation.PrivacidadeScreen
import com.example.taskgoapp.feature.settings.presentation.SobreScreen
import com.example.taskgoapp.feature.settings.presentation.SuporteScreen
import com.example.taskgoapp.feature.settings.presentation.AccountScreen
import com.example.taskgoapp.feature.settings.presentation.PreferencesScreen
import com.example.taskgoapp.feature.settings.presentation.NotificationsSettingsScreen
import com.example.taskgoapp.feature.settings.presentation.LanguageScreen
import com.example.taskgoapp.feature.chatai.presentation.AiSupportScreen

@Composable
fun TaskGoNavGraph(
    navController: NavHostController
) {
    val context = LocalContext.current
    val prefs = remember { com.example.taskgoapp.core.data.PreferencesManager(context) }
    val hasToken = prefs.getAuthToken()?.isNotBlank() == true
    val role = prefs.getAuthRole()
    val isProviderOrAdmin = role == "PROVIDER" || role == "ADMIN"
    NavHost(
        navController = navController,
        startDestination = if (hasToken) TaskGoDestinations.HOME_ROUTE else TaskGoDestinations.LOGIN_ROUTE
    ) {
        // Home
        composable(TaskGoDestinations.HOME_ROUTE) {
            HomeScreen(
                onNavigateToService = { serviceId -> 
                    navController.navigate("${TaskGoDestinations.SERVICE_DETAIL_ROUTE}/$serviceId")
                },
                onNavigateToProduct = { productId -> 
                    navController.navigate("${TaskGoDestinations.PRODUCT_DETAIL_ROUTE}/$productId")
                },
                onNavigateToCreateWorkOrder = { navController.navigate(TaskGoDestinations.CREATE_WORK_ORDER_ROUTE) },
                onNavigateToProposals = { navController.navigate(TaskGoDestinations.PROPOSALS_INBOX_ROUTE) },
                onNavigateToBuyBanner = { navController.navigate(TaskGoDestinations.ANUNCIOS_ROUTE) }
            )
        }

        // Services tab
        composable(TaskGoDestinations.SERVICES_ROUTE) {
            ServicesScreen(
                onBackClick = null,
                onNavigateToCreateWorkOrder = { navController.navigate(TaskGoDestinations.CREATE_WORK_ORDER_ROUTE) },
                onNavigateToProposals = { navController.navigate(TaskGoDestinations.PROPOSALS_INBOX_ROUTE) },
                onNavigateToMyServices = { navController.navigate(TaskGoDestinations.MEUS_SERVICOS_ROUTE) }
            )
        }

        composable(TaskGoDestinations.CREATE_WORK_ORDER_ROUTE) {
            CreateWorkOrderScreen(
                onBackClick = { navController.popBackStack() },
                onWorkOrderCreated = { navController.popBackStack() }
            )
        }

        composable(TaskGoDestinations.PROPOSALS_INBOX_ROUTE) {
            GerenciarPropostasScreen(
                onBackClick = { navController.popBackStack() },
                onVerProposta = { proposalId -> navController.navigate("${TaskGoDestinations.PROPOSAL_DETAIL_ROUTE}/$proposalId") }
            )
        }

        composable(
            route = "${TaskGoDestinations.PROPOSAL_DETAIL_ROUTE}/{proposalId}"
        ) { backStackEntry ->
            val proposalId = backStackEntry.arguments?.getString("proposalId")?.toLongOrNull() ?: 0L
            ProposalDetailScreen(
                proposalId = proposalId,
                onBackClick = { navController.popBackStack() },
                onProposalAccepted = { _ -> navController.navigate(TaskGoDestinations.CONFIRMAR_PROPOSTA_ROUTE) }
            )
        }

        composable(TaskGoDestinations.CONFIRMAR_PROPOSTA_ROUTE) {
            ConfirmarPropostaScreen(
                onBackClick = { navController.popBackStack() },
                onConfirmar = { navController.navigate(TaskGoDestinations.CONFIRMACAO_PIX_ROUTE) }
            )
        }

        composable(TaskGoDestinations.DETALHES_PROPOSTA_ROUTE) {
            DetalhesPropostaScreen(
                onBackClick = { navController.popBackStack() },
                onConfirmar = { navController.navigate(TaskGoDestinations.CONFIRMAR_PROPOSTA_ROUTE) }
            )
        }

        composable(TaskGoDestinations.AVALIAR_PRESTADOR_ROUTE) {
            AvaliarPrestadorScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(TaskGoDestinations.HISTORICO_SERVICOS_ROUTE) {
            HistoricoServicosScreen(
                onBackClick = { navController.popBackStack() },
                onVerDetalhes = { serviceId -> navController.navigate("detalhes_servico/$serviceId") }
            )
        }

        composable(TaskGoDestinations.MEUS_SERVICOS_ROUTE) {
            MeusServicosScreen(
                onBackClick = { navController.popBackStack() },
                onCriarServico = { navController.navigate(TaskGoDestinations.CREATE_WORK_ORDER_ROUTE) },
                onEditarServico = { serviceId -> navController.navigate("detalhes_servico/$serviceId") }
            )
        }

        composable(
            route = "detalhes_servico/{serviceId}",
            arguments = listOf(navArgument("serviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId") ?: ""
            DetalhesServicoScreen(
                serviceId = serviceId,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Products
        composable(TaskGoDestinations.PRODUCTS_ROUTE) {
            ProductsScreen(
                onNavigateToProductDetail = { productId -> 
                    navController.navigate("${TaskGoDestinations.PRODUCT_DETAIL_ROUTE}/$productId")
                },
                onNavigateToCart = { navController.navigate(TaskGoDestinations.CART_ROUTE) },
                onNavigateToNotifications = { navController.navigate(TaskGoDestinations.NOTIFICATIONS_ROUTE) },
                onNavigateToMessages = { navController.navigate(TaskGoDestinations.MESSAGES_ROUTE) },
                onNavigateToCreateProduct = { navController.navigate(TaskGoDestinations.CRIAR_PRODUTO_ROUTE) }
            )
        }

        composable(
            route = "${TaskGoDestinations.PRODUCT_DETAIL_ROUTE}/{productId}"
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            com.example.taskgoapp.feature.products.presentation.ProductDetailScreen(
                productId = productId,
                onBackClick = { navController.popBackStack() },
                onAddToCart = { navController.navigate(TaskGoDestinations.CART_ROUTE) }
            )
        }

        composable(TaskGoDestinations.CART_ROUTE) {
            CarrinhoScreen(
                onBackClick = { navController.popBackStack() },
                onContinuar = { navController.navigate(TaskGoDestinations.FORMA_PAGAMENTO_ROUTE) }
            )
        }

        composable(TaskGoDestinations.FORMA_PAGAMENTO_ROUTE) {
            FormaPagamentoScreen(
                onBackClick = { navController.popBackStack() },
                onPix = { navController.navigate(TaskGoDestinations.CONFIRMACAO_PIX_ROUTE) },
                onCartaoCredito = { navController.navigate(TaskGoDestinations.CARTAO_CREDITO_ROUTE) },
                onCartaoDebito = { navController.navigate(TaskGoDestinations.CARTAO_DEBITO_ROUTE) }
            )
        }

        composable(TaskGoDestinations.CARTAO_CREDITO_ROUTE) {
            CartaoCreditoScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(TaskGoDestinations.CARTAO_DEBITO_ROUTE) {
            CartaoDebitoScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(TaskGoDestinations.FINALIZAR_PEDIDO_ROUTE) {
            FinalizarPedidoScreen(
                onBackClick = { navController.popBackStack() },
                onFinalizar = { navController.navigate(TaskGoDestinations.CONFIRMACAO_PIX_ROUTE) }
            )
        }

        composable(TaskGoDestinations.CONFIRMACAO_PIX_ROUTE) {
            PixPaymentScreen(
                onBackClick = { navController.popBackStack() },
                onContinue = { /* TODO: Implementar continuar */ },
                onPixConfirmed = { navController.navigate(TaskGoDestinations.HOME_ROUTE) }
            )
        }

        composable(TaskGoDestinations.MEUS_PEDIDOS_ROUTE) {
            MeusPedidosScreen(
                onBackClick = { navController.popBackStack() },
                onOrderClick = { orderId -> 
                    // Verificar se é pedido em andamento para ir para detalhes, senão vai direto para resumo
                    navController.navigate("${TaskGoDestinations.DETALHES_PEDIDO_ROUTE}/$orderId")
                }
            )
        }

        composable(
            route = "${TaskGoDestinations.DETALHES_PEDIDO_ROUTE}/{orderId}"
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            DetalhesPedidoScreen(
                orderId = orderId,
                onBackClick = { navController.popBackStack() },
                onRastrearPedido = { id -> 
                    navController.navigate("${TaskGoDestinations.RASTREAMENTO_PEDIDO_ROUTE}/$id")
                },
                onVerResumo = { id -> 
                    navController.navigate("${TaskGoDestinations.RESUMO_PEDIDO_ROUTE}/$id")
                }
            )
        }

        composable(
            route = "${TaskGoDestinations.RASTREAMENTO_PEDIDO_ROUTE}/{orderId}"
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            RastreamentoPedidoScreen(
                orderId = orderId,
                onBackClick = { navController.popBackStack() },
                onVerDetalhes = { id -> 
                    navController.navigate("${TaskGoDestinations.RESUMO_PEDIDO_ROUTE}/$id")
                }
            )
        }

        composable(
            route = "${TaskGoDestinations.RESUMO_PEDIDO_ROUTE}/{orderId}"
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            ResumoPedidoScreen(
                orderId = orderId,
                onBackClick = { navController.popBackStack() },
                onIrParaPedidos = { navController.navigate(TaskGoDestinations.MEUS_PEDIDOS_ROUTE) }
            )
        }

        composable(
            route = "${TaskGoDestinations.ORDER_TRACKING_ROUTE}/{orderId}"
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId")?.toLongOrNull() ?: 0L
            OrderTrackingScreen(
                orderId = orderId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(TaskGoDestinations.MEUS_PRODUTOS_ROUTE) {
            MeusProdutosScreen(
                onBackClick = { navController.popBackStack() },
                onCriarProduto = { navController.navigate(TaskGoDestinations.GERENCIAR_PRODUTOS_ROUTE) },
                onEditarProduto = { productId -> navController.navigate("${TaskGoDestinations.EDITAR_PRODUTO_ROUTE}/$productId") }
            )
        }

        if (isProviderOrAdmin) {
            composable(TaskGoDestinations.GERENCIAR_PRODUTOS_ROUTE) {
                GerenciarProdutosScreen(
                    onBackClick = { navController.popBackStack() },
                    onCriarProduto = { navController.navigate(TaskGoDestinations.CRIAR_PRODUTO_ROUTE) },
                    onEditarProduto = { productId -> navController.navigate("${TaskGoDestinations.EDITAR_PRODUTO_ROUTE}/$productId") }
                )
            }
        }

        if (isProviderOrAdmin) {
            composable("${TaskGoDestinations.EDITAR_PRODUTO_ROUTE}/{productId}") { backStackEntry ->
                EditarProdutoScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        if (isProviderOrAdmin) {
            composable(TaskGoDestinations.CRIAR_PRODUTO_ROUTE) {
                CriarProdutoScreen(
                    onBackClick = { navController.popBackStack() },
                    onProductCreated = { navController.popBackStack() }
                )
            }
        }

        // Messages
        composable(TaskGoDestinations.MESSAGES_ROUTE) {
            MessagesScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToChat = { threadId -> 
                    navController.navigate("${TaskGoDestinations.CHAT_ROUTE}/$threadId")
                },
                onNavigateToCreateWorkOrder = { navController.navigate(TaskGoDestinations.CREATE_WORK_ORDER_ROUTE) },
                onNavigateToProposals = { navController.navigate(TaskGoDestinations.PROPOSALS_INBOX_ROUTE) },
                onNavigateToNotifications = { navController.navigate(TaskGoDestinations.NOTIFICATIONS_ROUTE) },
                onNavigateToSettings = { navController.navigate(TaskGoDestinations.CONFIGURACOES_ROUTE) },
                onNavigateToCart = { navController.navigate(TaskGoDestinations.CART_ROUTE) }
            )
        }

        composable(
            route = "${TaskGoDestinations.CHAT_ROUTE}/{threadId}"
        ) { backStackEntry ->
            val threadId = backStackEntry.arguments?.getString("threadId")?.toLongOrNull() ?: 0L
            ChatScreen(
                threadId = threadId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Profile
        composable(TaskGoDestinations.PROFILE_ROUTE) {
            ProfileScreen(
                onNavigateToMyData = { navController.navigate(TaskGoDestinations.MEUS_DADOS_ROUTE) },
                onNavigateToMyServices = { navController.navigate(TaskGoDestinations.MEUS_SERVICOS_ROUTE) },
                onNavigateToMyProducts = { navController.navigate(TaskGoDestinations.MEUS_PRODUTOS_ROUTE) },
                onNavigateToMyOrders = { navController.navigate(TaskGoDestinations.MEUS_PEDIDOS_ROUTE) },
                onNavigateToMyReviews = { navController.navigate(TaskGoDestinations.MINHAS_AVALIACOES_ROUTE) },
                onNavigateToManageProposals = { navController.navigate(TaskGoDestinations.GERENCIAR_PROPOSTAS_ROUTE) },
                onNavigateToSettings = { navController.navigate(TaskGoDestinations.CONFIGURACOES_ROUTE) },
                onNavigateToNotifications = { navController.navigate(TaskGoDestinations.NOTIFICATIONS_ROUTE) },
                onNavigateToCart = { navController.navigate(TaskGoDestinations.CART_ROUTE) },
                onNavigateToMessages = { navController.navigate(TaskGoDestinations.MESSAGES_ROUTE) }
            )
        }

        composable(TaskGoDestinations.CONTA_ROUTE) {
            ContaScreen(
                onBackClick = { navController.popBackStack() },
                onMeusDados = { navController.navigate(TaskGoDestinations.MEUS_DADOS_ROUTE) },
                onMeusServicos = { navController.navigate(TaskGoDestinations.MEUS_SERVICOS_ROUTE) },
                onMeusProdutos = { navController.navigate(TaskGoDestinations.MEUS_PRODUTOS_ROUTE) },
                onMeusPedidos = { navController.navigate(TaskGoDestinations.MEUS_PEDIDOS_ROUTE) },
                onMinhasAvaliacoes = { navController.navigate(TaskGoDestinations.MINHAS_AVALIACOES_ROUTE) },
                onGerenciarPropostas = { navController.navigate(TaskGoDestinations.GERENCIAR_PROPOSTAS_ROUTE) }
            )
        }

        composable(TaskGoDestinations.MEUS_DADOS_ROUTE) {
            MeusDadosScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(TaskGoDestinations.MINHAS_AVALIACOES_ROUTE) {
            MinhasAvaliacoesScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(TaskGoDestinations.GERENCIAR_PROPOSTAS_ROUTE) {
            GerenciarPropostasScreen(
                onBackClick = { navController.popBackStack() },
                onVerProposta = { _ -> navController.navigate(TaskGoDestinations.DETALHES_PROPOSTA_ROUTE) }
            )
        }

        composable(TaskGoDestinations.CONFIGURACOES_ROUTE) {
            ConfiguracoesScreen(
                onBackClick = { navController.popBackStack() },
                onConta = { navController.navigate(TaskGoDestinations.ACCOUNT_SETTINGS_ROUTE) },
                onPreferencias = { navController.navigate(TaskGoDestinations.PREFERENCES_SETTINGS_ROUTE) },
                onNotificacoes = { navController.navigate(TaskGoDestinations.NOTIFICATIONS_SETTINGS_ROUTE) },
                onIdioma = { navController.navigate(TaskGoDestinations.LANGUAGE_SETTINGS_ROUTE) },
                onPrivacidade = { navController.navigate(TaskGoDestinations.PRIVACIDADE_ROUTE) },
                onSuporte = { navController.navigate(TaskGoDestinations.SUPORTE_ROUTE) },
                onSobre = { navController.navigate(TaskGoDestinations.SOBRE_ROUTE) }
            )
        }

        // Telas detalhadas de Configurações
        composable(TaskGoDestinations.ACCOUNT_SETTINGS_ROUTE) {
            AccountScreen(onBackClick = { navController.popBackStack() })
        }
        composable(TaskGoDestinations.PREFERENCES_SETTINGS_ROUTE) {
            PreferencesScreen(onBackClick = { navController.popBackStack() }, onSaveChanges = { navController.popBackStack() })
        }
        composable(TaskGoDestinations.NOTIFICATIONS_SETTINGS_ROUTE) {
            NotificationsSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(TaskGoDestinations.LANGUAGE_SETTINGS_ROUTE) {
            LanguageScreen(onBackClick = { navController.popBackStack() }, onLogout = { navController.navigate(TaskGoDestinations.LOGIN_ROUTE) })
        }

        // Suporte com IA
        composable(TaskGoDestinations.AI_SUPPORT_ROUTE) {
            AiSupportScreen(onBackClick = { navController.popBackStack() })
        }

        composable(TaskGoDestinations.ALTERAR_SENHA_ROUTE) {
            AlterarSenhaScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(TaskGoDestinations.PRIVACIDADE_ROUTE) {
            PrivacidadeScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(TaskGoDestinations.SUPORTE_ROUTE) {
            SuporteScreen(
                onBackClick = { navController.popBackStack() },
                onChatAi = { navController.navigate(TaskGoDestinations.AI_SUPPORT_ROUTE) },
                onSobre = { navController.navigate(TaskGoDestinations.SOBRE_ROUTE) }
            )
        }

        composable(TaskGoDestinations.SOBRE_ROUTE) {
            SobreScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(TaskGoDestinations.NOTIFICATIONS_ROUTE) {
            NotificationsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToNotificationsSettings = { navController.navigate(TaskGoDestinations.NOTIFICATIONS_SETTINGS_ROUTE) },
                onNotificationClick = { notification -> 
                    navController.navigate("${TaskGoDestinations.NOTIFICATION_DETAIL_ROUTE}/${notification.id}")
                }
            )
        }

        composable(
            route = "${TaskGoDestinations.NOTIFICATION_DETAIL_ROUTE}/{notificationId}",
            arguments = listOf(
                navArgument("notificationId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val notificationId = backStackEntry.arguments?.getLong("notificationId") ?: 0L
            // TODO: Get notification from repository
            val notification = NotificationItem(
                id = notificationId,
                title = "Notificação",
                description = "Descrição da notificação",
                timestamp = "Agora",
                icon = "📱",
                isRead = false
            )
            NotificationDetailScreen(
                notification = notification,
                onBackClick = { navController.popBackStack() },
                onActionClick = { /* TODO: Handle action */ }
            )
        }

        // Auth
        composable(TaskGoDestinations.LOGIN_ROUTE) {
            val viewModel: com.example.taskgoapp.feature.auth.presentation.AuthViewModel = hiltViewModel()
            LoginStoreScreen(
                onLoginSuccess = { navController.navigate(TaskGoDestinations.HOME_ROUTE) },
                onNavigateToSignup = { navController.navigate(TaskGoDestinations.CADASTRO_ROUTE) },
                onSwitchToPersonLogin = { /* TODO: Implementar se necessário */ },
                onForgotPassword = { navController.navigate(TaskGoDestinations.FORGOT_PASSWORD_ROUTE) },
                viewModel = viewModel
            )
        }

        composable(TaskGoDestinations.FORGOT_PASSWORD_ROUTE) {
            val viewModel: com.example.taskgoapp.feature.auth.presentation.AuthViewModel = hiltViewModel()
            val forgotState = viewModel.forgotPasswordState.collectAsState().value
            ForgotPasswordScreen(
                onBackClick = { navController.popBackStack() },
                onSendClick = { email -> viewModel.forgotPassword(email) },
                isLoading = forgotState.isLoading,
                error = forgotState.error,
                success = forgotState.success
            )
        }

        composable(TaskGoDestinations.CADASTRO_ROUTE) {
            CadastroScreen(
                onCadastrar = { navController.navigate(TaskGoDestinations.CADASTRO_FINALIZADO_ROUTE) }
            )
        }

        composable(TaskGoDestinations.CADASTRO_FINALIZADO_ROUTE) {
            CadastroFinalizadoScreen(
                onContinue = { navController.navigate(TaskGoDestinations.HOME_ROUTE) }
            )
        }

        // Checkout
        composable(TaskGoDestinations.CADASTRAR_ENDERECO_ROUTE) {
            CadastrarEnderecoScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Reviews
        composable(TaskGoDestinations.REVIEWS_ROUTE) {
            ReviewsScreen(
                onBackClick = { navController.popBackStack() },
                onProposalAccepted = { _ -> navController.navigate(TaskGoDestinations.CONFIRMAR_PROPOSTA_ROUTE) }
            )
        }

        // Ads
        composable(TaskGoDestinations.ANUNCIOS_ROUTE) {
            AnunciosScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
