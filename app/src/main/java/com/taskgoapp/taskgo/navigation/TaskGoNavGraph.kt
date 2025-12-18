package com.taskgoapp.taskgo.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskgoapp.taskgo.feature.home.presentation.HomeScreen
import com.taskgoapp.taskgo.feature.splash.presentation.SplashScreen
import com.taskgoapp.taskgo.feature.auth.presentation.LoginPersonScreen
import com.taskgoapp.taskgo.feature.auth.presentation.LoginStoreScreen
import com.taskgoapp.taskgo.feature.auth.presentation.SignUpScreen
import com.taskgoapp.taskgo.feature.auth.presentation.SignUpSuccessScreen
import com.taskgoapp.taskgo.feature.services.presentation.ServicesScreen
import com.taskgoapp.taskgo.feature.services.presentation.LocalProvidersScreen
import com.taskgoapp.taskgo.feature.services.presentation.LocalServiceOrdersScreen
import com.taskgoapp.taskgo.feature.services.presentation.ServiceOrderDetailScreen
import com.taskgoapp.taskgo.feature.services.presentation.ProposalsReceivedScreen
import com.taskgoapp.taskgo.feature.services.presentation.ProposalDetailScreen
import com.taskgoapp.taskgo.feature.services.presentation.ProposalsViewModel
import com.taskgoapp.taskgo.feature.services.presentation.ServiceHistoryScreen
import com.taskgoapp.taskgo.feature.services.presentation.RateProviderScreen
import com.taskgoapp.taskgo.feature.services.presentation.HistoricoServicosScreen
import com.taskgoapp.taskgo.feature.services.presentation.AvaliarPrestadorScreen
import com.taskgoapp.taskgo.feature.products.presentation.ProductsScreen
import com.taskgoapp.taskgo.feature.products.presentation.DiscountedProductsScreen
import com.taskgoapp.taskgo.feature.products.presentation.CreateProductScreen
import com.taskgoapp.taskgo.feature.products.presentation.EditProductScreen
import com.taskgoapp.taskgo.feature.products.presentation.ManageProductsScreen
import com.taskgoapp.taskgo.feature.messages.presentation.MessagesScreen
import com.taskgoapp.taskgo.feature.profile.presentation.ProfileScreen
import com.taskgoapp.taskgo.feature.profile.presentation.MyDataScreen
import com.taskgoapp.taskgo.feature.profile.presentation.MyReviewsScreen
import com.taskgoapp.taskgo.feature.profile.presentation.ProviderProfileScreen
import com.taskgoapp.taskgo.feature.ads.presentation.ComprarBannerScreen
import com.taskgoapp.taskgo.feature.checkout.presentation.CheckoutScreen
import com.taskgoapp.taskgo.feature.checkout.presentation.OrderSummaryScreen
import com.taskgoapp.taskgo.feature.checkout.presentation.PixPaymentScreen
import com.taskgoapp.taskgo.feature.checkout.presentation.CardDetailsScreen
import com.taskgoapp.taskgo.feature.checkout.presentation.PaymentSuccessScreen
import com.taskgoapp.taskgo.feature.orders.presentation.OrderSuccessScreen
import com.taskgoapp.taskgo.feature.notifications.presentation.NotificationsScreen
import com.taskgoapp.taskgo.feature.notifications.presentation.NotificationDetailScreen
import com.taskgoapp.taskgo.feature.orders.presentation.MeusPedidosScreen
import com.taskgoapp.taskgo.feature.orders.presentation.MyOrdersInProgressScreen
import com.taskgoapp.taskgo.feature.orders.presentation.MyOrdersCompletedScreen
import com.taskgoapp.taskgo.feature.orders.presentation.MyOrdersCanceledScreen
import com.taskgoapp.taskgo.feature.products.presentation.CartScreen
import com.taskgoapp.taskgo.feature.products.presentation.ProductDetailScreen
import com.taskgoapp.taskgo.feature.products.presentation.CarrinhoScreen
import com.taskgoapp.taskgo.feature.products.presentation.DetalhesProdutoScreen
import com.taskgoapp.taskgo.feature.products.presentation.EditarProdutoScreen
import com.taskgoapp.taskgo.feature.products.presentation.GerenciarProdutosScreen
import com.taskgoapp.taskgo.feature.products.presentation.CriarProdutoScreen
import com.taskgoapp.taskgo.feature.products.presentation.OrderTrackingScreen
import com.taskgoapp.taskgo.feature.products.presentation.CheckoutScreen as CheckoutScreenLegacy
import com.taskgoapp.taskgo.feature.services.presentation.CreateWorkOrderScreen
import com.taskgoapp.taskgo.feature.services.presentation.DetalhesServicoScreen
import com.taskgoapp.taskgo.feature.services.presentation.GerenciarPropostasScreen
import com.taskgoapp.taskgo.feature.services.presentation.ConfirmarPropostaScreen
import com.taskgoapp.taskgo.feature.services.presentation.DetalhesPropostaScreen
import com.taskgoapp.taskgo.feature.settings.presentation.ConfiguracoesScreen
import com.taskgoapp.taskgo.feature.auth.presentation.ForgotPasswordScreen
import com.taskgoapp.taskgo.feature.settings.presentation.AccountScreen
import com.taskgoapp.taskgo.feature.settings.presentation.PreferencesScreen
import com.taskgoapp.taskgo.feature.settings.presentation.NotificationsSettingsScreen
import com.taskgoapp.taskgo.feature.settings.presentation.LanguageScreen
import com.taskgoapp.taskgo.feature.settings.presentation.PrivacyScreen
import com.taskgoapp.taskgo.feature.settings.presentation.SupportScreen
import com.taskgoapp.taskgo.feature.settings.presentation.AboutScreen
import com.taskgoapp.taskgo.feature.settings.presentation.PrivacyPolicyScreen
import com.taskgoapp.taskgo.feature.settings.presentation.TermsOfServiceScreen
import com.taskgoapp.taskgo.feature.settings.presentation.ConsentHistoryScreen
import com.taskgoapp.taskgo.feature.settings.presentation.AlterarSenhaScreen
import com.taskgoapp.taskgo.feature.settings.presentation.PrivacidadeScreen
import com.taskgoapp.taskgo.feature.settings.presentation.SobreScreen
import com.taskgoapp.taskgo.feature.settings.presentation.SuporteScreen
import com.taskgoapp.taskgo.feature.settings.presentation.SettingsScreen
import com.taskgoapp.taskgo.feature.messages.presentation.ChatScreen
import com.taskgoapp.taskgo.feature.products.presentation.MeusProdutosScreen
import com.taskgoapp.taskgo.feature.services.presentation.MeusServicosScreen
import com.taskgoapp.taskgo.feature.services.presentation.ServiceFormScreen
import com.taskgoapp.taskgo.feature.services.presentation.MyServiceOrdersScreen
import com.taskgoapp.taskgo.feature.checkout.presentation.AddressBookScreen
import com.taskgoapp.taskgo.feature.checkout.presentation.CadastrarEnderecoScreen
import com.taskgoapp.taskgo.feature.checkout.presentation.PaymentMethodScreen
import com.taskgoapp.taskgo.feature.checkout.presentation.FormaPagamentoScreen
import com.taskgoapp.taskgo.feature.checkout.presentation.FinalizarPedidoScreen
import com.taskgoapp.taskgo.feature.checkout.presentation.ConfirmacaoPixScreen
import com.taskgoapp.taskgo.feature.checkout.presentation.CartaoCreditoScreen
import com.taskgoapp.taskgo.feature.checkout.presentation.CartaoDebitoScreen
import com.taskgoapp.taskgo.feature.orders.presentation.DetalhesPedidoScreen
import com.taskgoapp.taskgo.feature.orders.presentation.RastreamentoPedidoScreen
import com.taskgoapp.taskgo.feature.orders.presentation.ResumoPedidoScreen
import com.taskgoapp.taskgo.feature.products.presentation.ProductFormScreen
import com.taskgoapp.taskgo.feature.ads.presentation.AdsScreen
import com.taskgoapp.taskgo.feature.chatai.presentation.AiSupportScreen
import com.taskgoapp.taskgo.feature.chatai.presentation.ChatListScreen
import com.taskgoapp.taskgo.feature.search.presentation.UniversalSearchScreen
import com.taskgoapp.taskgo.feature.reviews.presentation.UserReviewsScreen
import com.taskgoapp.taskgo.feature.reviews.presentation.ReviewsScreen
import com.taskgoapp.taskgo.feature.reviews.presentation.CreateReviewScreen
import com.taskgoapp.taskgo.core.model.ReviewType
import com.taskgoapp.taskgo.feature.auth.presentation.IdentityVerificationScreen
import com.taskgoapp.taskgo.feature.settings.presentation.SecuritySettingsScreen

@Composable
fun TaskGoNavGraph(
    navController: NavHostController,
    startDestination: String = "splash",
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("splash") {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate("login_person") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNavigateToBiometricAuth = {
                    // Biometria removida - navegar direto para home
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        
        // Rota de biometria removida - login apenas no primeiro acesso

        composable("forgot_password") {
            ForgotPasswordScreen(
                onBackClick = { navController.popBackStack() },
                onResetSent = { navController.popBackStack() }
            )
        }

        // Rotas de autenticação
        composable("login_person") {
            LoginPersonScreen(
                onNavigateToStoreLogin = { navController.navigate("login_store") },
                onNavigateToSignUp = { navController.navigate("signup") },
                onNavigateToHome = { navController.navigate("home") },
                onNavigateToForgotPassword = { navController.navigate("forgot_password") }
            )
        }
        
        composable("login_store") {
            LoginStoreScreen(
                onNavigateToPersonLogin = { navController.navigate("login_person") },
                onNavigateToSignUp = { navController.navigate("signup") },
                onNavigateToHome = { navController.navigate("home") },
                onNavigateToForgotPassword = { navController.navigate("forgot_password") }
            )
        }
        
        composable("signup") {
            SignUpScreen(
                onNavigateToLogin = { navController.navigate("login_person") },
                onNavigateToHome = { navController.navigate("signup_success") },
                onNavigateToDocumentVerification = { navController.navigate("identity_verification") },
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable("signup_success") {
            SignUpSuccessScreen(
                onNavigateToHome = { navController.navigate("home") },
                onNavigateToLogin = { navController.navigate("login_person") }
            )
        }

            composable("home") {
                HomeScreen(
                    onNavigateToService = { serviceId ->
                        navController.navigate("service_detail/$serviceId")
                    },
                    onNavigateToProduct = { productId ->
                        navController.navigate("product_detail/$productId")
                    },
                    onNavigateToCreateWorkOrder = {
                        navController.navigate("create_work_order")
                    },
                    onNavigateToProposals = {
                        navController.navigate("proposals_inbox")
                    },
                    onNavigateToBuyBanner = {
                        navController.navigate("comprar_banner")
                    },
                    onNavigateToNotifications = {
                        navController.navigate("notifications")
                    },
                    onNavigateToSettings = {
                        navController.navigate("configuracoes")
                    },
                    onNavigateToMessages = {
                        navController.navigate("messages")
                    },
                    onNavigateToCart = {
                        navController.navigate("cart")
                    },
                    onNavigateToSearch = {
                        navController.navigate("universal_search")
                    },
                    onNavigateToLocalProviders = {
                        navController.navigate("local_providers")
                    },
                    onNavigateToDiscountedProducts = {
                        navController.navigate("discounted_products")
                    },
                    onNavigateToLocalServiceOrders = {
                        navController.navigate("local_service_orders")
                    },
                    onNavigateToProviderProfile = { providerId, isStore ->
                        // Por enquanto, usar apenas providerId. Se precisar diferenciar loja, criar rota separada
                        navController.navigate("provider_profile/$providerId")
                    }
                )
            }
            
            composable("universal_search") {
                UniversalSearchScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToProduct = { productId ->
                        navController.navigate("product_detail/$productId")
                    },
                    onNavigateToService = { serviceId ->
                        navController.navigate("service_detail/$serviceId")
                    }
                )
            }

            composable("home_products") {
                HomeScreen(
                    onNavigateToService = { serviceId ->
                        navController.navigate("service_detail/$serviceId")
                    },
                    onNavigateToProduct = { productId ->
                        navController.navigate("product_detail/$productId")
                    },
                    onNavigateToCreateWorkOrder = {
                        navController.navigate("create_work_order")
                    },
                    onNavigateToProposals = {
                        navController.navigate("proposals_inbox")
                    },
                    onNavigateToBuyBanner = {
                        navController.navigate("comprar_banner")
                    },
                    onNavigateToNotifications = {
                        navController.navigate("notifications")
                    },
                    onNavigateToSettings = {
                        navController.navigate("configuracoes")
                    },
                    onNavigateToMessages = {
                        navController.navigate("messages")
                    },
                    onNavigateToCart = {
                        navController.navigate("cart")
                    },
                    onNavigateToLocalProviders = {
                        navController.navigate("local_providers")
                    },
                    onNavigateToDiscountedProducts = {
                        navController.navigate("discounted_products")
                    },
                    onNavigateToLocalServiceOrders = {
                        navController.navigate("local_service_orders")
                    },
                    variant = "products"
                )
            }
            composable("home_services") {
                HomeScreen(
                    onNavigateToService = { serviceId ->
                        navController.navigate("service_detail/$serviceId")
                    },
                    onNavigateToProduct = { productId ->
                        navController.navigate("product_detail/$productId")
                    },
                    onNavigateToCreateWorkOrder = {
                        navController.navigate("create_work_order")
                    },
                    onNavigateToProposals = {
                        navController.navigate("proposals_inbox")
                    },
                    onNavigateToBuyBanner = {
                        navController.navigate("comprar_banner")
                    },
                    onNavigateToNotifications = {
                        navController.navigate("notifications")
                    },
                    onNavigateToSettings = {
                        navController.navigate("configuracoes")
                    },
                    onNavigateToMessages = {
                        navController.navigate("messages")
                    },
                    onNavigateToCart = {
                        navController.navigate("cart")
                    },
                    onNavigateToLocalProviders = {
                        navController.navigate("local_providers")
                    },
                    onNavigateToDiscountedProducts = {
                        navController.navigate("discounted_products")
                    },
                    onNavigateToLocalServiceOrders = {
                        navController.navigate("local_service_orders")
                    },
                    variant = "services"
                )
            }
            composable("home_messages") {
                HomeScreen(
                    onNavigateToService = { serviceId ->
                        navController.navigate("service_detail/$serviceId")
                    },
                    onNavigateToProduct = { productId ->
                        navController.navigate("product_detail/$productId")
                    },
                    onNavigateToCreateWorkOrder = {
                        navController.navigate("create_work_order")
                    },
                    onNavigateToProposals = {
                        navController.navigate("proposals_inbox")
                    },
                    onNavigateToBuyBanner = {
                        navController.navigate("comprar_banner")
                    },
                    onNavigateToNotifications = {
                        navController.navigate("notifications")
                    },
                    onNavigateToSettings = {
                        navController.navigate("configuracoes")
                    },
                    onNavigateToMessages = {
                        navController.navigate("messages")
                    },
                    onNavigateToCart = {
                        navController.navigate("cart")
                    },
                    onNavigateToLocalProviders = {
                        navController.navigate("local_providers")
                    },
                    onNavigateToDiscountedProducts = {
                        navController.navigate("discounted_products")
                    },
                    onNavigateToLocalServiceOrders = {
                        navController.navigate("local_service_orders")
                    },
                    variant = "messages"
                )
            }
            composable("home_profile") {
                HomeScreen(
                    onNavigateToService = { serviceId ->
                        navController.navigate("service_detail/$serviceId")
                    },
                    onNavigateToProduct = { productId ->
                        navController.navigate("product_detail/$productId")
                    },
                    onNavigateToCreateWorkOrder = {
                        navController.navigate("create_work_order")
                    },
                    onNavigateToProposals = {
                        navController.navigate("proposals_inbox")
                    },
                    onNavigateToBuyBanner = {
                        navController.navigate("comprar_banner")
                    },
                    onNavigateToNotifications = {
                        navController.navigate("notifications")
                    },
                    onNavigateToSettings = {
                        navController.navigate("configuracoes")
                    },
                    onNavigateToMessages = {
                        navController.navigate("messages")
                    },
                    onNavigateToCart = {
                        navController.navigate("cart")
                    },
                    onNavigateToLocalProviders = {
                        navController.navigate("local_providers")
                    },
                    onNavigateToDiscountedProducts = {
                        navController.navigate("discounted_products")
                    },
                    onNavigateToLocalServiceOrders = {
                        navController.navigate("local_service_orders")
                    },
                    variant = "profile"
                )
            }

            // Novas rotas de banners promocionais
            composable("local_service_orders") {
                LocalServiceOrdersScreen(
                    onBackClick = { navController.popBackStack() },
                    onOrderClick = { orderId ->
                        navController.navigate("service_order_detail/$orderId")
                    }
                )
            }
            composable(
                route = "service_order_detail/{orderId}",
                arguments = listOf(
                    navArgument("orderId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                ServiceOrderDetailScreen(
                    orderId = orderId,
                    onBackClick = { navController.popBackStack() },
                    onSendProposal = { orderId ->
                        // Navegar para mensagens
                        // A abertura automática da conversa será implementada no MessagesScreen
                        // quando receber orderId via parâmetro
                        navController.navigate("messages")
                    }
                )
            }
            composable("local_providers") {
                LocalProvidersScreen(
                    onNavigateToServiceDetail = { providerId ->
                        navController.navigate("provider_profile/$providerId")
                    },
                    onNavigateToCreateWorkOrder = {
                        navController.navigate("create_work_order")
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
            
            // Rota de perfil do prestador/loja
            composable(
                route = "provider_profile/{providerId}",
                arguments = listOf(
                    navArgument("providerId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val providerId = backStackEntry.arguments?.getString("providerId") ?: ""
                // Por padrão, assumir que é prestador (não loja)
                // Se precisar diferenciar, pode passar via query parameter ou criar rota separada
                val isStore = false
                ProviderProfileScreen(
                    providerId = providerId,
                    isStore = isStore,
                    onBackClick = { navController.popBackStack() },
                    onRateClick = { providerId ->
                        navController.navigate("rate_provider/$providerId")
                    },
                    onMessageClick = { providerId ->
                        // Navegar para mensagens
                        // A abertura automática da conversa será implementada no MessagesScreen
                        // quando receber providerId via parâmetro
                        navController.navigate("messages")
                    },
                    onServiceClick = { serviceId ->
                        navController.navigate("service_detail/$serviceId")
                    }
                )
            }
            
            composable("discounted_products") {
                DiscountedProductsScreen(
                    onNavigateToProductDetail = { productId ->
                        navController.navigate("product_detail/$productId")
                    },
                    onNavigateToCart = {
                        navController.navigate("cart")
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Rotas das abas da navegação inferior
            composable("services") {
                ServicesScreen(
                    onNavigateToServiceDetail = { serviceId ->
                        navController.navigate("service_detail/$serviceId")
                    },
                    onNavigateToCreateWorkOrder = {
                        navController.navigate("create_work_order")
                    },
                    onNavigateToNotifications = {
                        navController.navigate("notifications")
                    },
                    onNavigateToCart = {
                        navController.navigate("cart")
                    },
                    onNavigateToMessages = {
                        navController.navigate("messages")
                    }
                )
            }

            composable("products") {
                ProductsScreen(
                    onNavigateToProductDetail = { productId ->
                        navController.navigate("product_detail/$productId")
                    },
                    onNavigateToCart = {
                        navController.navigate("cart")
                    },
                    onNavigateToAddProduct = {
                        navController.navigate("create_product")
                    },
                    onNavigateToNotifications = {
                        navController.navigate("notifications")
                    },
                    onNavigateToMessages = {
                        navController.navigate("messages")
                    }
                )
            }

            composable("messages") {
                MessagesScreen(
                    onBackClick = { navController.popBackStack() },
                    onNavigateToChat = { chatId ->
                        navController.navigate("chat/$chatId")
                    },
                    onNavigateToCreateWorkOrder = {
                        navController.navigate("create_work_order")
                    },
                    onNavigateToProposals = {
                        navController.navigate("proposals_inbox")
                    },
                    onNavigateToNotifications = {
                        navController.navigate("notifications")
                    },
                    onNavigateToSettings = {
                        navController.navigate("configuracoes")
                    },
                    onNavigateToCart = {
                        navController.navigate("cart")
                    }
                )
            }
        composable("messages_empty") {
            MessagesScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToChat = { chatId -> navController.navigate("chat/$chatId") },
                onNavigateToCreateWorkOrder = { navController.navigate("create_work_order") },
                onNavigateToProposals = { navController.navigate("proposals_inbox") },
                onNavigateToNotifications = { navController.navigate("notifications") },
                onNavigateToSettings = { navController.navigate("configuracoes") },
                onNavigateToCart = { navController.navigate("cart") },
                variant = "empty"
            )
        }
        composable("messages_loading") {
            MessagesScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToChat = { chatId -> navController.navigate("chat/$chatId") },
                onNavigateToCreateWorkOrder = { navController.navigate("create_work_order") },
                onNavigateToProposals = { navController.navigate("proposals_inbox") },
                onNavigateToNotifications = { navController.navigate("notifications") },
                onNavigateToSettings = { navController.navigate("configuracoes") },
                onNavigateToCart = { navController.navigate("cart") },
                variant = "loading"
            )
        }
        composable("messages_unread") {
            MessagesScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToChat = { chatId -> navController.navigate("chat/$chatId") },
                onNavigateToCreateWorkOrder = { navController.navigate("create_work_order") },
                onNavigateToProposals = { navController.navigate("proposals_inbox") },
                onNavigateToNotifications = { navController.navigate("notifications") },
                onNavigateToSettings = { navController.navigate("configuracoes") },
                onNavigateToCart = { navController.navigate("cart") },
                variant = "unread"
            )
        }

            composable("profile") {
                ProfileScreen(
                    onNavigateToSettings = {
                        navController.navigate("configuracoes")
                    },
                    onNavigateToMyOrders = {
                        navController.navigate("meus_pedidos")
                    },
                    onNavigateToMyServices = {
                        navController.navigate("meus_servicos")
                    },
                    onNavigateToMyProducts = {
                        navController.navigate("gerenciar_produtos")
                    },
                    onNavigateToMyServiceOrders = {
                        navController.navigate("minhas_ordens_servico")
                    },
                    onNavigateToAboutMe = {
                        navController.navigate("sobre_mim")
                    },
                    onNavigateToUserReviews = { userId, userName ->
                        navController.navigate("user_reviews/$userId/$userName")
                    }
                )
            }

        // Profile extras
        composable("my_data") { MyDataScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("sobre_mim") {
            com.taskgoapp.taskgo.feature.profile.presentation.AboutMeScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToReviews = { userId, userName ->
                    navController.navigate("user_reviews/$userId/$userName")
                }
            )
        }
        composable("my_reviews") { MyReviewsScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("user_reviews/{userId}/{userName}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val userName = backStackEntry.arguments?.getString("userName") ?: "Usuário"
            UserReviewsScreen(
                userId = userId,
                userName = userName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("identity_verification") {
            IdentityVerificationScreen(
                onBackClick = { navController.popBackStack() },
                onSkipVerification = { navController.navigate("home") },
                onVerificationComplete = { navController.navigate("home") },
                onNavigateToFacialVerification = { navController.navigate("facial_verification") }
            )
        }
        
        composable("facial_verification") {
            com.taskgoapp.taskgo.feature.auth.presentation.FacialVerificationScreen(
                onBackClick = { navController.popBackStack() },
                onVerificationComplete = { navController.popBackStack() }
            )
        }

        // Rotas de produtos
        composable("product_detail/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductDetailScreen(
                productId = productId,
                onBackClick = { navController.popBackStack() },
                onAddToCart = { navController.navigate("cart") },
                onNavigateToReviews = { targetId ->
                    navController.navigate("reviews/$targetId/PRODUCT")
                }
            )
        }

        composable("product_detail_reviews/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductDetailScreen(
                productId = productId,
                onBackClick = { navController.popBackStack() },
                onAddToCart = { navController.navigate("cart") },
                variant = "reviews"
            )
        }

        composable("product_detail_gallery/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductDetailScreen(
                productId = productId,
                onBackClick = { navController.popBackStack() },
                onAddToCart = { navController.navigate("cart") },
                variant = "gallery"
            )
        }
        
        // Rotas de avaliações
        composable("reviews/{targetId}/{type}") { backStackEntry ->
            val targetId = backStackEntry.arguments?.getString("targetId") ?: ""
            val typeString = backStackEntry.arguments?.getString("type") ?: "PRODUCT"
            val type = when (typeString) {
                "PRODUCT" -> ReviewType.PRODUCT
                "SERVICE" -> ReviewType.SERVICE
                "PROVIDER" -> ReviewType.PROVIDER
                else -> ReviewType.PRODUCT
            }
            ReviewsScreen(
                targetId = targetId,
                type = type,
                targetName = "Item", // TODO: Buscar nome do target
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreateReview = {
                    navController.navigate("create_review/$targetId/$typeString")
                }
            )
        }
        
        composable("create_review/{targetId}/{type}") { backStackEntry ->
            val targetId = backStackEntry.arguments?.getString("targetId") ?: ""
            val typeString = backStackEntry.arguments?.getString("type") ?: "PRODUCT"
            val type = when (typeString) {
                "PRODUCT" -> ReviewType.PRODUCT
                "SERVICE" -> ReviewType.SERVICE
                "PROVIDER" -> ReviewType.PROVIDER
                else -> ReviewType.PRODUCT
            }
            CreateReviewScreen(
                targetId = targetId,
                type = type,
                targetName = "Item", // TODO: Buscar nome do target
                orderId = null, // TODO: Passar orderId se disponível
                onNavigateBack = { navController.popBackStack() },
                onReviewCreated = { navController.popBackStack() }
            )
        }
        
        composable("create_review/{targetId}/{type}/{orderId}") { backStackEntry ->
            val targetId = backStackEntry.arguments?.getString("targetId") ?: ""
            val typeString = backStackEntry.arguments?.getString("type") ?: "PRODUCT"
            val orderId = backStackEntry.arguments?.getString("orderId")
            val type = when (typeString) {
                "PRODUCT" -> ReviewType.PRODUCT
                "SERVICE" -> ReviewType.SERVICE
                "PROVIDER" -> ReviewType.PROVIDER
                else -> ReviewType.PRODUCT
            }
            CreateReviewScreen(
                targetId = targetId,
                type = type,
                targetName = "Item", // TODO: Buscar nome do target
                orderId = orderId,
                onNavigateBack = { navController.popBackStack() },
                onReviewCreated = { navController.popBackStack() }
            )
        }

        composable("cart") {
            CartScreen(
                onNavigateToCheckout = { navController.navigate("checkout") },
                onNavigateToProductDetail = { productId -> 
                    navController.navigate("product_detail/$productId")
                },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProducts = { navController.navigate("products") }
            )
        }

        composable("cart_filled") {
            CartScreen(
                onNavigateToCheckout = { navController.navigate("checkout") },
                onNavigateToProductDetail = { productId -> 
                    navController.navigate("product_detail/$productId")
                },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProducts = { navController.navigate("products") },
                variant = "filled"
            )
        }
        
        composable("create_product") {
            ProductFormScreen(
                productId = null,
                onBack = { navController.popBackStack() },
                onSaved = { 
                    navController.navigate("gerenciar_produtos") {
                        popUpTo("create_product") { inclusive = true }
                    }
                }
            )
        }
        
        composable("edit_product/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductFormScreen(
                productId = productId,
                onBack = { navController.popBackStack() },
                onSaved = { 
                    navController.navigate("gerenciar_produtos") {
                        popUpTo("edit_product/$productId") { inclusive = true }
                    }
                }
            )
        }
        
        composable("manage_products") {
            // Redirecionar para a tela correta
            navController.navigate("gerenciar_produtos") {
                popUpTo("manage_products") { inclusive = true }
            }
        }

        composable("checkout") {
            val checkoutViewModel: com.taskgoapp.taskgo.feature.checkout.presentation.CheckoutViewModel = hiltViewModel()
            CheckoutScreen(
                onBackClick = { navController.popBackStack() },
                onAddressSelection = { 
                    navController.navigate("address_selection") {
                        // Garantir que o ViewModel seja compartilhado
                        launchSingleTop = true
                    }
                },
                onPaymentMethodSelection = { 
                    navController.navigate("payment_method_selection") {
                        launchSingleTop = true
                    }
                },
                onOrderSummary = { addressId, paymentType ->
                    val encodedAddress = Uri.encode(addressId)
                    navController.navigate("order_summary/$encodedAddress/${paymentType.name}")
                },
                viewModel = checkoutViewModel
            )
        }

        composable(
            route = "order_summary/{addressId}/{paymentType}",
            arguments = listOf(
                navArgument("addressId") { type = NavType.StringType },
                navArgument("paymentType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val addressId = Uri.decode(backStackEntry.arguments?.getString("addressId").orEmpty())
            val paymentType = backStackEntry.arguments?.getString("paymentType").orEmpty()
            OrderSummaryScreen(
                onNavigateBack = { navController.popBackStack() },
                addressId = addressId,
                paymentTypeName = paymentType,
                onOrderFinished = { orderId, total, trackingCode ->
                    val trackingParam = Uri.encode(trackingCode)
                    navController.navigate(
                        "payment_success?orderId=$orderId&total=$total&tracking=$trackingParam"
                    ) {
                        popUpTo("checkout") { inclusive = false }
                    }
                },
                onNavigateToPix = { orderId, total ->
                    val encodedOrderId = Uri.encode(orderId)
                    navController.navigate("pix_payment/$encodedOrderId/$total") {
                        popUpTo("checkout") { inclusive = false }
                    }
                }
            )
        }

        // Variações de método de pagamento e finalizar pedido
        composable("payment_method_two_options") {
            // Para esta rota, criar um ViewModel temporário ou usar hiltViewModel
            val tempViewModel: com.taskgoapp.taskgo.feature.checkout.presentation.CheckoutViewModel = hiltViewModel()
            PaymentMethodScreen(
                onNavigateBack = { navController.popBackStack() },
                onPaymentMethodSelected = { methodName ->
                    when (methodName) {
                        "Cartão de Crédito" -> navController.navigate("cartao_credito")
                        "Cartão de Débito" -> navController.navigate("cartao_debito")
                        else -> navController.popBackStack()
                    }
                },
                variant = "two_options",
                viewModel = tempViewModel
            )
        }

        // Checkout auxiliary routes
        composable("address_selection") {
            // Usar o mesmo ViewModel do checkout através do navController
            val checkoutBackStackEntry = remember(navController) {
                navController.getBackStackEntry("checkout")
            }
            val checkoutViewModel: com.taskgoapp.taskgo.feature.checkout.presentation.CheckoutViewModel = 
                viewModel(checkoutBackStackEntry)
            AddressBookScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddressSelected = { addressId ->
                    // A seleção já foi salva no ViewModel pela AddressBookScreen
                    navController.popBackStack()
                },
                onAddAddress = { navController.navigate("add_address") },
                viewModel = checkoutViewModel
            )
        }

        composable("add_address") {
            CadastrarEnderecoScreen(
                onBackClick = { navController.popBackStack() },
                onSave = { navController.popBackStack() }
            )
        }

        composable("payment_method_selection") {
            // Usar o mesmo ViewModel do checkout através do navController
            val checkoutBackStackEntry = remember(navController) {
                navController.getBackStackEntry("checkout")
            }
            val checkoutViewModel: com.taskgoapp.taskgo.feature.checkout.presentation.CheckoutViewModel = 
                viewModel(checkoutBackStackEntry)
            PaymentMethodScreen(
                onNavigateBack = { navController.popBackStack() },
                onPaymentMethodSelected = { methodName ->
                    // A seleção já foi salva no ViewModel pela PaymentMethodScreen
                    when (methodName) {
                        "Pix" -> navController.popBackStack() // PIX não precisa de tela adicional
                        "Cartão de Crédito" -> navController.navigate("cartao_credito")
                        "Cartão de Débito" -> navController.navigate("cartao_debito")
                        "Google Pay" -> navController.popBackStack() // Google Pay já processado
                        else -> navController.popBackStack()
                    }
                },
                viewModel = checkoutViewModel
            )
        }

        // Optional PT-BR named routes for fidelity with Figma
        composable("forma_pagamento") {
            FormaPagamentoScreen(
                onBackClick = { navController.popBackStack() },
                onPix = { navController.navigate("pix_payment") },
                onCartaoCredito = { navController.navigate("cartao_credito") },
                onCartaoDebito = { navController.navigate("cartao_debito") }
            )
        }
        composable("forma_pagamento_pix_only") {
            FormaPagamentoScreen(
                onBackClick = { navController.popBackStack() },
                onPix = { navController.navigate("pix_payment") },
                onCartaoCredito = { navController.navigate("cartao_credito") },
                onCartaoDebito = { navController.navigate("cartao_debito") },
                variant = "pix_only"
            )
        }
        composable("finalizar_pedido") {
            FinalizarPedidoScreen(
                onBackClick = { navController.popBackStack() },
                onFinalizar = { navController.navigate("payment_success") }
            )
        }
        composable("finalizar_pedido_processing") {
            FinalizarPedidoScreen(
                onBackClick = { navController.popBackStack() },
                onFinalizar = { },
                variant = "processing"
            )
        }
        composable("finalizar_pedido_pending") {
            FinalizarPedidoScreen(
                onBackClick = { navController.popBackStack() },
                onFinalizar = { },
                variant = "pending"
            )
        }
        composable("finalizar_pedido_payment_error") {
            FinalizarPedidoScreen(
                onBackClick = { navController.popBackStack() },
                onFinalizar = { },
                variant = "payment_error"
            )
        }
        composable("finalizar_pedido_success") {
            FinalizarPedidoScreen(
                onBackClick = { navController.popBackStack() },
                onFinalizar = { },
                variant = "success"
            )
        }
        composable("confirmacao_pix") {
            ConfirmacaoPixScreen(onContinue = { navController.popBackStack() })
        }
        composable("cartao_credito") {
            CartaoCreditoScreen(onBackClick = { navController.popBackStack() })
        }
        composable("cartao_debito") {
            CartaoDebitoScreen(onBackClick = { navController.popBackStack() })
        }
        composable("cartao_credito_alt") {
            CartaoCreditoScreen(onBackClick = { navController.popBackStack() }, isAlt = true)
        }
        composable("cartao_debito_alt") {
            CartaoDebitoScreen(onBackClick = { navController.popBackStack() }, isAlt = true)
        }
        
        composable(
            route = "pix_payment/{orderId}/{total}",
            arguments = listOf(
                navArgument("orderId") { type = NavType.StringType },
                navArgument("total") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val orderId = Uri.decode(backStackEntry.arguments?.getString("orderId").orEmpty())
            val total = backStackEntry.arguments?.getString("total")?.toDoubleOrNull() ?: 0.0
            PixPaymentScreen(
                orderId = orderId,
                totalAmount = total,
                onPaymentSuccess = { 
                    navController.navigate("payment_success?orderId=$orderId&total=$total") {
                        popUpTo("checkout") { inclusive = false }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = "pix_payment_waiting/{orderId}/{total}",
            arguments = listOf(
                navArgument("orderId") { type = NavType.StringType },
                navArgument("total") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val orderId = Uri.decode(backStackEntry.arguments?.getString("orderId").orEmpty())
            val total = backStackEntry.arguments?.getString("total")?.toDoubleOrNull() ?: 0.0
            PixPaymentScreen(
                orderId = orderId,
                totalAmount = total,
                onPaymentSuccess = { navController.navigate("payment_success?orderId=$orderId&total=$total") },
                onBackClick = { navController.popBackStack() },
                variant = "waiting"
            )
        }
        composable(
            route = "pix_payment_error/{orderId}/{total}",
            arguments = listOf(
                navArgument("orderId") { type = NavType.StringType },
                navArgument("total") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val orderId = Uri.decode(backStackEntry.arguments?.getString("orderId").orEmpty())
            val total = backStackEntry.arguments?.getString("total")?.toDoubleOrNull() ?: 0.0
            PixPaymentScreen(
                orderId = orderId,
                totalAmount = total,
                onPaymentSuccess = { navController.navigate("payment_success?orderId=$orderId&total=$total") },
                onBackClick = { navController.popBackStack() },
                variant = "error"
            )
        }
        composable(
            route = "pix_payment_success/{orderId}/{total}",
            arguments = listOf(
                navArgument("orderId") { type = NavType.StringType },
                navArgument("total") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val orderId = Uri.decode(backStackEntry.arguments?.getString("orderId").orEmpty())
            val total = backStackEntry.arguments?.getString("total")?.toDoubleOrNull() ?: 0.0
            PixPaymentScreen(
                orderId = orderId,
                totalAmount = total,
                onPaymentSuccess = { navController.navigate("payment_success?orderId=$orderId&total=$total") },
                onBackClick = { navController.popBackStack() },
                variant = "success"
            )
        }
        
        composable("card_details") {
            CardDetailsScreen(
                onPaymentSuccess = { navController.navigate("payment_success") },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = "payment_success?orderId={orderId}&total={total}&tracking={tracking}",
            arguments = listOf(
                navArgument("orderId") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("total") {
                    type = NavType.FloatType
                    defaultValue = 0f
                },
                navArgument("tracking") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId").orEmpty().ifBlank { "#TG-0000" }
            val total = backStackEntry.arguments?.getFloat("total")?.toDouble() ?: 0.0
            val tracking = backStackEntry.arguments?.getString("tracking").orEmpty()
            PaymentSuccessScreen(
                totalAmount = total,
                orderId = orderId,
                trackingCode = tracking,
                onContinue = {
                    navController.navigate("home") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        // Alternative order success variants
        composable("order_success/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderSuccessScreen(
                orderId = orderId,
                totalAmount = 250.0,
                address = "", // Dados vêm do Firestore
                onHome = {
                    navController.navigate("home") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onViewOrder = { navController.navigate("order_detail/$orderId") }
            )
        }
        composable("order_success_pending/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderSuccessScreen(
                orderId = orderId,
                totalAmount = 250.0,
                address = "", // Dados vêm do Firestore
                onHome = { navController.navigate("home") },
                onViewOrder = { navController.navigate("order_detail/$orderId") },
                variant = "pending"
            )
        }
        composable("order_success_error/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderSuccessScreen(
                orderId = orderId,
                totalAmount = 250.0,
                address = "", // Dados vêm do Firestore
                onHome = { navController.navigate("home") },
                onViewOrder = { navController.navigate("order_detail/$orderId") },
                variant = "error"
            )
        }

        // Rotas de serviços
        composable("service_detail/{serviceId}") { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId") ?: ""
            DetalhesServicoScreen(
                serviceId = serviceId,
                onBackClick = { navController.popBackStack() },
                onEditService = { serviceId ->
                    navController.navigate("service_form/$serviceId")
                },
                onNavigateToReviews = { targetId ->
                    navController.navigate("reviews/$targetId/SERVICE")
                }
            )
        }

        composable("create_work_order") {
            CreateWorkOrderScreen(
                onBackClick = { navController.popBackStack() },
                onWorkOrderCreated = { navController.navigate("minhas_ordens_servico") {
                    popUpTo("services") { inclusive = false }
                }},
                onNavigateToIdentityVerification = { navController.navigate("identity_verification") }
            )
        }
        
        composable("proposals_received") {
            val proposalsViewModel: ProposalsViewModel = hiltViewModel()
            ProposalsReceivedScreen(
                onBackClick = { navController.popBackStack() },
                onProposalClick = { proposalId -> navController.navigate("proposal_detail/$proposalId") },
                onAcceptProposal = { proposalId -> 
                    proposalsViewModel.acceptProposal(proposalId)
                    navController.popBackStack()
                },
                onRejectProposal = { proposalId -> 
                    proposalsViewModel.rejectProposal(proposalId)
                    navController.popBackStack()
                }
            )
        }
        
        composable("proposal_detail/{proposalId}") { backStackEntry ->
            val proposalId = backStackEntry.arguments?.getString("proposalId") ?: ""
            val proposalsViewModel: ProposalsViewModel = hiltViewModel()
            ProposalDetailScreen(
                proposalId = proposalId,
                onBackClick = { navController.popBackStack() },
                onAcceptProposal = { proposalId -> 
                    proposalsViewModel.acceptProposal(proposalId)
                    navController.popBackStack()
                },
                onRejectProposal = { proposalId -> 
                    proposalsViewModel.rejectProposal(proposalId)
                    navController.popBackStack()
                }
            )
        }
        
        composable("service_history") {
            ServiceHistoryScreen(
                onBackClick = { navController.popBackStack() },
                onServiceClick = { serviceId -> navController.navigate("service_detail/$serviceId") },
                onRateService = { serviceId -> navController.navigate("rate_provider/$serviceId") }
            )
        }
        // Legacy/alternate services routes
        composable("historico_servicos") {
            HistoricoServicosScreen(
                onBackClick = { navController.popBackStack() },
                onVerDetalhes = { id -> navController.navigate("service_detail/$id") }
            )
        }
        composable("avaliar_prestador/{serviceId}") { _ ->
            AvaliarPrestadorScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable("rate_provider/{serviceId}") { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId") ?: ""
            val createReviewViewModel: com.taskgoapp.taskgo.feature.reviews.presentation.CreateReviewViewModel = hiltViewModel()
            val orderDetailViewModel: com.taskgoapp.taskgo.feature.services.presentation.ServiceOrderDetailViewModel = hiltViewModel()
            
            // Tentar buscar como ordem primeiro
            LaunchedEffect(serviceId) {
                orderDetailViewModel.loadOrder(serviceId)
            }
            
            val orderState by orderDetailViewModel.uiState.collectAsStateWithLifecycle()
            val providerName = orderState.order?.providerId ?: "Prestador"
            val serviceTitle = orderState.order?.details ?: "Serviço"
            val providerId = orderState.order?.providerId ?: serviceId
            val orderId = if (orderState.order != null) serviceId else null
            
            LaunchedEffect(providerId, orderId) {
                createReviewViewModel.initialize(
                    targetId = providerId,
                    type = com.taskgoapp.taskgo.core.model.ReviewType.PROVIDER,
                    orderId = orderId
                )
            }
            
            RateProviderScreen(
                providerName = providerName,
                serviceTitle = serviceTitle,
                onBackClick = { navController.popBackStack() },
                onRatingSubmitted = { rating, comment -> 
                    createReviewViewModel.createReview(rating, comment)
                    navController.popBackStack()
                }
            )
        }

        composable("proposals_inbox") {
            GerenciarPropostasScreen(
                onBackClick = { navController.popBackStack() },
                onVerProposta = { proposalId -> 
                    navController.navigate("proposal_detail/$proposalId")
                }
            )
        }

        // Fluxo de proposta: confirmar e detalhes com variações
        composable("confirmar_proposta") {
            ConfirmarPropostaScreen(
                onBackClick = { navController.popBackStack() },
                onConfirmar = { navController.popBackStack() }
            )
        }
        composable("confirmar_proposta_pendente") {
            ConfirmarPropostaScreen(
                onBackClick = { navController.popBackStack() },
                onConfirmar = { navController.popBackStack() },
                variant = "pendente"
            )
        }
        composable("confirmar_proposta_aceita") {
            ConfirmarPropostaScreen(
                onBackClick = { navController.popBackStack() },
                onConfirmar = { navController.popBackStack() },
                variant = "aceita"
            )
        }
        composable("confirmar_proposta_recusada") {
            ConfirmarPropostaScreen(
                onBackClick = { navController.popBackStack() },
                onConfirmar = { navController.popBackStack() },
                variant = "recusada"
            )
        }
        composable("confirmar_proposta_erro") {
            ConfirmarPropostaScreen(
                onBackClick = { navController.popBackStack() },
                onConfirmar = { navController.popBackStack() },
                variant = "erro"
            )
        }
        composable("confirmar_proposta_sucesso") {
            ConfirmarPropostaScreen(
                onBackClick = { navController.popBackStack() },
                onConfirmar = { navController.popBackStack() },
                variant = "sucesso"
            )
        }

        composable("detalhes_proposta/{propostaId}") { backStackEntry ->
            val propostaId = backStackEntry.arguments?.getString("propostaId") ?: ""
            DetalhesPropostaScreen(
                propostaId = propostaId,
                onBackClick = { navController.popBackStack() },
                onConfirmar = { navController.popBackStack() }
            )
        }
        composable("detalhes_proposta_aceita/{propostaId}") { backStackEntry ->
            val propostaId = backStackEntry.arguments?.getString("propostaId") ?: ""
            DetalhesPropostaScreen(
                propostaId = propostaId,
                onBackClick = { navController.popBackStack() },
                onConfirmar = { navController.popBackStack() },
                variant = "aceita"
            )
        }
        composable("detalhes_proposta_recusada/{propostaId}") { backStackEntry ->
            val propostaId = backStackEntry.arguments?.getString("propostaId") ?: ""
            DetalhesPropostaScreen(
                propostaId = propostaId,
                onBackClick = { navController.popBackStack() },
                onConfirmar = { navController.popBackStack() },
                variant = "recusada"
            )
        }
        composable("detalhes_proposta_pendente/{propostaId}") { backStackEntry ->
            val propostaId = backStackEntry.arguments?.getString("propostaId") ?: ""
            DetalhesPropostaScreen(
                propostaId = propostaId,
                onBackClick = { navController.popBackStack() },
                onConfirmar = { navController.popBackStack() },
                variant = "pendente"
            )
        }
        composable("detalhes_proposta_erro/{propostaId}") { backStackEntry ->
            val propostaId = backStackEntry.arguments?.getString("propostaId") ?: ""
            DetalhesPropostaScreen(
                propostaId = propostaId,
                onBackClick = { navController.popBackStack() },
                onConfirmar = { navController.popBackStack() },
                variant = "erro"
            )
        }

        // Rotas de anúncios - Removidas: anuncios e anuncio_detalhe não são mais usadas
        // A navegação vai direto para comprar_banner
        
        composable("comprar_banner") {
            ComprarBannerScreen(
                onBackClick = { navController.popBackStack() },
                onConfirmarCompra = { _ -> navController.popBackStack() }
            )
        }

        // Ads variants - Removidas: variantes de anuncios não são mais usadas
        composable("comprar_banner_disabled") {
            ComprarBannerScreen(
                onBackClick = { navController.popBackStack() },
                onConfirmarCompra = { },
                variant = "disabled"
            )
        }

        // Rotas de notificações
        composable("notifications") {
            NotificationsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToNotificationsSettings = { navController.navigate("notification_settings") },
                onNotificationClick = { notificationId ->
                    navController.navigate("notification_detail/$notificationId")
                }
            )
        }
        composable("notifications_empty") {
            NotificationsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToNotificationsSettings = { navController.navigate("notification_settings") },
                onNotificationClick = { notificationId -> navController.navigate("notification_detail/$notificationId") },
                variant = "empty"
            )
        }
        composable("notifications_unread") {
            NotificationsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToNotificationsSettings = { navController.navigate("notification_settings") },
                onNotificationClick = { notificationId -> navController.navigate("notification_detail/$notificationId") },
                variant = "unread"
            )
        }
        composable("notifications_settings_mode") {
            NotificationsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToNotificationsSettings = { navController.navigate("notification_settings") },
                onNotificationClick = { notificationId -> navController.navigate("notification_detail/$notificationId") },
                variant = "settings"
            )
        }

        composable("notification_detail/{notificationId}") { backStackEntry ->
            val notificationId = backStackEntry.arguments?.getString("notificationId") ?: ""
            NotificationDetailScreen(
                notificationId = notificationId,
                title = "Atualização",
                message = "Seu pedido foi despachado e está a caminho.",
                timestamp = "Hoje, 08:12",
                onBackClick = { navController.popBackStack() }
            )
        }


        // Rotas de configurações
        composable("configuracoes") {
            ConfiguracoesScreen(
                onBackClick = { navController.popBackStack() },
                onConta = { navController.navigate("account") },
                onPreferencias = { navController.navigate("preferences") },
                onNotificacoes = { navController.navigate("notification_settings") },
                onIdioma = { navController.navigate("language") },
                onPrivacidade = { navController.navigate("privacy") },
                onSuporte = { navController.navigate("support") },
                onSobre = { navController.navigate("about") },
                onAiSupport = { navController.navigate("ai_support") },
                onSeguranca = { navController.navigate("security_settings") }
            )
        }

        // Settings routes
        composable("account") { 
            AccountScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate("login_person") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToBankAccounts = { navController.navigate("bank_accounts") }
            ) 
        }
        composable("bank_accounts") {
            com.taskgoapp.taskgo.feature.settings.presentation.BankAccountScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("security_settings") {
            SecuritySettingsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToIdentityVerification = { navController.navigate("identity_verification") },
                onNavigateToConsentHistory = { navController.navigate("consent_history") }
            )
        }
        composable("preferences") {
            PreferencesScreen(
                onBackClick = { navController.popBackStack() },
                onSaveChanges = { navController.popBackStack() }
            )
        }
        composable("notification_settings") {
            NotificationsSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("language") {
            LanguageScreen(
                onBackClick = { navController.popBackStack() },
                onLogout = { /* no-op */ }
            )
        }
        composable("privacy") { 
            PrivacyScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToPrivacyPolicy = { navController.navigate("privacy_policy") },
                onNavigateToTermsOfService = { navController.navigate("terms_of_service") },
                onNavigateToConsentHistory = { navController.navigate("consent_history") }
            ) 
        }
        composable("support") { SupportScreen(onBackClick = { navController.popBackStack() }) }
        composable("about") { 
            AboutScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToPrivacyPolicy = { navController.navigate("privacy_policy") },
                onNavigateToTermsOfService = { navController.navigate("terms_of_service") }
            ) 
        }
        composable("privacy_policy") {
            PrivacyPolicyScreen(onBackClick = { navController.popBackStack() })
        }
        composable("terms_of_service") {
            TermsOfServiceScreen(onBackClick = { navController.popBackStack() })
        }
        composable("consent_history") {
            ConsentHistoryScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Legacy/alternate settings routes
        composable("alterar_senha") { AlterarSenhaScreen(onBackClick = { navController.popBackStack() }) }
        composable("privacidade_alt") { PrivacidadeScreen(onBackClick = { navController.popBackStack() }) }
        composable("sobre_alt") { SobreScreen(onBackClick = { navController.popBackStack() }) }
        composable("suporte_alt") {
            SuporteScreen(
                onBackClick = { navController.popBackStack() },
                onChatAi = { navController.navigate("ai_support") },
                onSobre = { navController.navigate("about") }
            )
        }
        composable("settings_legacy") {
            SettingsScreen(
                onNavigateToAccount = { navController.navigate("account") },
                onNavigateToPreferences = { navController.navigate("preferences") },
                onNavigateToNotifications = { navController.navigate("notification_settings") },
                onNavigateToLanguage = { navController.navigate("language") },
                onNavigateToPrivacy = { navController.navigate("privacy") },
                onNavigateToSupport = { navController.navigate("support") },
                onNavigateToAbout = { navController.navigate("about") },
                onNavigateToAiSupport = { navController.navigate("ai_support") },
                onNavigateToSecurity = { navController.navigate("security_settings") }
            )
        }

        // Messages
        composable("chat/{chatId}") { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            // Converter String para Long para compatibilidade com ChatScreen
            val threadIdLong = chatId.toLongOrNull() ?: 0L
            ChatScreen(
                threadId = threadIdLong,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Profile subroutes
        composable("meus_servicos") {
            MeusServicosScreen(
                onBackClick = { navController.popBackStack() },
                onCriarServico = { navController.navigate("service_form") },
                onEditarServico = { serviceId -> navController.navigate("service_form/$serviceId") },
                onViewService = { serviceId -> navController.navigate("service_detail/$serviceId") }
            )
        }
        
        composable("minhas_ordens_servico") {
            MyServiceOrdersScreen(
                onBackClick = { navController.popBackStack() },
                onEditOrder = { orderId -> navController.navigate("create_work_order?orderId=$orderId") },
                onCreateOrder = { navController.navigate("create_work_order") }
            )
        }
        
        composable("service_form") {
            ServiceFormScreen(
                serviceId = null,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        
        composable("service_form/{serviceId}") { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId") ?: ""
            ServiceFormScreen(
                serviceId = serviceId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable("meus_produtos") {
            MeusProdutosScreen(
                onBackClick = { navController.popBackStack() },
                onCriarProduto = { navController.navigate("create_product") },
                onEditarProduto = { productId -> navController.navigate("edit_product/$productId") }
            )
        }

        // Misc routes
        composable("product_form") {
            ProductFormScreen(
                productId = null,
                onBack = { navController.popBackStack() },
                onSaved = { _ -> navController.navigate("gerenciar_produtos") }
            )
        }

        composable("ads_alt") { AdsScreen(onBackClick = { navController.popBackStack() }) }
        composable("ai_support") {
            val chatListViewModel: com.taskgoapp.taskgo.feature.chatai.presentation.ChatListViewModel = hiltViewModel()
            ChatListScreen(
                onBackClick = { navController.popBackStack() },
                onChatClick = { chatId ->
                    navController.navigate("ai_chat/$chatId")
                },
                viewModel = chatListViewModel
            )
        }
        composable("ai_chat/{chatId}") { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            val chatListViewModel: com.taskgoapp.taskgo.feature.chatai.presentation.ChatListViewModel = hiltViewModel()
            AiSupportScreen(
                chatId = chatId,
                onBackClick = { navController.popBackStack() },
                onChatUpdated = { id, message, isFirstMessage ->
                    chatListViewModel.updateChatLastMessage(id, message, isFirstMessage)
                }
            )
        }

        // Rotas de pedidos
        composable("meus_pedidos") {
            MeusPedidosScreen(
                onBackClick = { navController.popBackStack() },
                onOrderClick = { orderId -> 
                    navController.navigate("order_detail/$orderId")
                },
                onNavigateToCreateReview = { targetId, type, orderId ->
                    if (orderId != null) {
                        navController.navigate("create_review/$targetId/$type/$orderId")
                    } else {
                        navController.navigate("create_review/$targetId/$type")
                    }
                }
            )
        }

        composable("order_detail/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            DetalhesPedidoScreen(
                orderId = orderId,
                onBackClick = { navController.popBackStack() },
                onRastrearPedido = { navController.navigate("order_tracking_legacy/$it") },
                onVerResumo = { id -> navController.navigate("order_summary_alt/$id") },
                onEnviarPedido = { navController.navigate("shipment/$it") } // Para vendedores
            )
        }
        composable("order_detail_pending/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            DetalhesPedidoScreen(
                orderId = orderId,
                onBackClick = { navController.popBackStack() },
                onRastrearPedido = { navController.navigate("order_tracking/$orderId") },
                onVerResumo = { id -> navController.navigate("order_summary_alt/$id") },
                variant = "pending"
            )
        }
        composable("order_detail_canceled/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            DetalhesPedidoScreen(
                orderId = orderId,
                onBackClick = { navController.popBackStack() },
                onRastrearPedido = { },
                onVerResumo = { },
                variant = "canceled"
            )
        }

        composable("order_tracking/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            RastreamentoPedidoScreen(
                orderId = orderId,
                onBackClick = { navController.popBackStack() },
                onVerDetalhes = { id -> navController.navigate("order_detail/$id") }
            )
        }
        composable("order_tracking_delivered/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            RastreamentoPedidoScreen(
                orderId = orderId,
                onBackClick = { navController.popBackStack() },
                onVerDetalhes = { id -> navController.navigate("order_detail/$id") },
                variant = "delivered"
            )
        }
        composable("order_tracking_delayed/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            RastreamentoPedidoScreen(
                orderId = orderId,
                onBackClick = { navController.popBackStack() },
                onVerDetalhes = { id -> navController.navigate("order_detail/$id") },
                variant = "delayed"
            )
        }
        composable("order_tracking_canceled/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            RastreamentoPedidoScreen(
                orderId = orderId,
                onBackClick = { navController.popBackStack() },
                onVerDetalhes = { id -> navController.navigate("order_detail/$id") },
                variant = "canceled"
            )
        }

        composable("order_summary_alt/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            ResumoPedidoScreen(
                orderId = orderId,
                onBackClick = { navController.popBackStack() },
                onIrParaPedidos = { navController.navigate("meus_pedidos") }
            )
        }
        composable("order_summary_discount/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            ResumoPedidoScreen(
                orderId = orderId,
                onBackClick = { navController.popBackStack() },
                onIrParaPedidos = { navController.navigate("meus_pedidos") },
                variant = "discount"
            )
        }
        composable("order_summary_voucher/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            ResumoPedidoScreen(
                orderId = orderId,
                onBackClick = { navController.popBackStack() },
                onIrParaPedidos = { navController.navigate("meus_pedidos") },
                variant = "voucher"
            )
        }

        composable("my_orders_in_progress") {
            MyOrdersInProgressScreen(
                onBackClick = { navController.popBackStack() },
                onOrderClick = { orderId -> navController.navigate("order_detail/$orderId") }
            )
        }

        composable("my_orders_completed") {
            MyOrdersCompletedScreen(
                onBackClick = { navController.popBackStack() },
                onOrderClick = { orderId -> navController.navigate("order_detail/$orderId") }
            )
        }

        composable("my_orders_canceled") {
            MyOrdersCanceledScreen(
                onBackClick = { navController.popBackStack() },
                onOrderClick = { orderId -> navController.navigate("order_detail/$orderId") }
            )
        }

        // Legacy product/checkout routes for compatibility with existing files
        composable("carrinho_legacy") {
            CarrinhoScreen(
                onBackClick = { navController.popBackStack() },
                onContinuar = { navController.navigate("checkout_legacy") }
            )
        }
        composable("detalhes_produto_legacy/{productId}") { _ ->
            DetalhesProdutoScreen(
                onBackClick = { navController.popBackStack() },
                onAdicionarCarrinho = { navController.navigate("cart") }
            )
        }
        composable("editar_produto_legacy/{productId}") { _ ->
            EditarProdutoScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("gerenciar_produtos") {
            GerenciarProdutosScreen(
                onBackClick = { navController.popBackStack() },
                onCriarProduto = { navController.navigate("create_product") },
                onEditarProduto = { id -> navController.navigate("edit_product/$id") }
            )
        }
        composable("order_tracking_legacy/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderTrackingScreen(
                orderId = orderId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("shipment/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            com.taskgoapp.taskgo.feature.orders.presentation.ShipmentScreen(
                orderId = orderId,
                onBackClick = { navController.popBackStack() },
                onShipmentConfirmed = { navController.popBackStack() }
            )
        }
        composable("checkout_legacy") {
            CheckoutScreenLegacy(
                onBackClick = { navController.popBackStack() },
                onAddressSelection = { navController.navigate("address_selection") },
                onPaymentMethodSelection = { navController.navigate("payment_method_selection") },
                onOrderSummary = { navController.navigate("order_summary") }
            )
        }
    }
}