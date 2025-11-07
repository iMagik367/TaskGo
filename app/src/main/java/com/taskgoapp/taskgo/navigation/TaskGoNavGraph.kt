package com.taskgoapp.taskgo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.taskgoapp.taskgo.feature.home.presentation.HomeScreen
import com.taskgoapp.taskgo.feature.splash.presentation.SplashScreen
import com.taskgoapp.taskgo.feature.auth.presentation.LoginPersonScreen
import com.taskgoapp.taskgo.feature.auth.presentation.LoginStoreScreen
import com.taskgoapp.taskgo.feature.auth.presentation.SignUpScreen
import com.taskgoapp.taskgo.feature.auth.presentation.SignUpSuccessScreen
import com.taskgoapp.taskgo.feature.services.presentation.ServicesScreen
import com.taskgoapp.taskgo.feature.services.presentation.ProposalsReceivedScreen
import com.taskgoapp.taskgo.feature.services.presentation.ProposalDetailScreen
import com.taskgoapp.taskgo.feature.services.presentation.ServiceHistoryScreen
import com.taskgoapp.taskgo.feature.services.presentation.RateProviderScreen
import com.taskgoapp.taskgo.feature.services.presentation.HistoricoServicosScreen
import com.taskgoapp.taskgo.feature.services.presentation.AvaliarPrestadorScreen
import com.taskgoapp.taskgo.feature.products.presentation.ProductsScreen
import com.taskgoapp.taskgo.feature.products.presentation.CreateProductScreen
import com.taskgoapp.taskgo.feature.products.presentation.EditProductScreen
import com.taskgoapp.taskgo.feature.products.presentation.ManageProductsScreen
import com.taskgoapp.taskgo.feature.messages.presentation.MessagesScreen
import com.taskgoapp.taskgo.feature.profile.presentation.ProfileScreen
import com.taskgoapp.taskgo.feature.profile.presentation.MyDataScreen
import com.taskgoapp.taskgo.feature.profile.presentation.MyReviewsScreen
import com.taskgoapp.taskgo.feature.ads.presentation.AnunciosScreen
import com.taskgoapp.taskgo.feature.ads.presentation.AnuncioDetalheScreen
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
import com.taskgoapp.taskgo.core.design.review.DesignReviewScreen
import com.taskgoapp.taskgo.feature.auth.presentation.ForgotPasswordScreen
import com.taskgoapp.taskgo.feature.settings.presentation.AccountScreen
import com.taskgoapp.taskgo.feature.settings.presentation.PreferencesScreen
import com.taskgoapp.taskgo.feature.settings.presentation.NotificationsSettingsScreen
import com.taskgoapp.taskgo.feature.settings.presentation.LanguageScreen
import com.taskgoapp.taskgo.feature.settings.presentation.PrivacyScreen
import com.taskgoapp.taskgo.feature.settings.presentation.SupportScreen
import com.taskgoapp.taskgo.feature.settings.presentation.AboutScreen
import com.taskgoapp.taskgo.feature.settings.presentation.AlterarSenhaScreen
import com.taskgoapp.taskgo.feature.settings.presentation.PrivacidadeScreen
import com.taskgoapp.taskgo.feature.settings.presentation.SobreScreen
import com.taskgoapp.taskgo.feature.settings.presentation.SuporteScreen
import com.taskgoapp.taskgo.feature.settings.presentation.SettingsScreen
import com.taskgoapp.taskgo.feature.messages.presentation.ChatScreen
import com.taskgoapp.taskgo.feature.products.presentation.MeusProdutosScreen
import com.taskgoapp.taskgo.feature.services.presentation.MeusServicosScreen
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
import com.taskgoapp.taskgo.feature.settings.presentation.AccountTypeScreen
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
                }
            )
        }

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
                        navController.navigate("anuncios")
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
                        navController.navigate("anuncios")
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
                        navController.navigate("anuncios")
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
                        navController.navigate("anuncios")
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
                        navController.navigate("anuncios")
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
                    variant = "profile"
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
                        navController.navigate("meus_produtos")
                    }
                )
            }

        // Profile extras
        composable("my_data") { MyDataScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("my_reviews") { MyReviewsScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("identity_verification") {
            IdentityVerificationScreen(
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
                onAddToCart = { navController.navigate("cart") }
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
            CreateProductScreen(
                onBackClick = { navController.popBackStack() },
                onProductCreated = { navController.navigate("manage_products") }
            )
        }
        
        composable("edit_product/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            EditProductScreen(
                productId = productId,
                onBackClick = { navController.popBackStack() },
                onProductUpdated = { navController.navigate("manage_products") },
                onProductDeleted = { navController.navigate("manage_products") }
            )
        }
        
        composable("manage_products") {
            ManageProductsScreen(
                onBackClick = { navController.popBackStack() },
                onCreateProduct = { navController.navigate("product_form") },
                onEditProduct = { productId -> navController.navigate("edit_product/$productId") },
                onDeleteProduct = { productId -> 
                    // TODO: Implementar lógica de exclusão
                    navController.popBackStack()
                }
            )
        }

        composable("checkout") {
            CheckoutScreen(
                onBackClick = { navController.popBackStack() },
                onAddressSelection = { navController.navigate("address_selection") },
                onPaymentMethodSelection = { navController.navigate("payment_method_selection") },
                onOrderSummary = { navController.navigate("order_summary") }
            )
        }

        composable("order_summary") {
            OrderSummaryScreen(
                onNavigateBack = { navController.popBackStack() },
                onConfirmOrder = { navController.navigate("payment_success") }
            )
        }

        // Variações de método de pagamento e finalizar pedido
        composable("payment_method_two_options") {
            PaymentMethodScreen(
                onNavigateBack = { navController.popBackStack() },
                onPaymentMethodSelected = { methodName ->
                    when (methodName) {
                        "Cartão de Crédito" -> navController.navigate("cartao_credito")
                        "Cartão de Débito" -> navController.navigate("cartao_debito")
                        else -> navController.popBackStack()
                    }
                },
                variant = "two_options"
            )
        }

        // Checkout auxiliary routes
        composable("address_selection") {
            AddressBookScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddressSelected = { _ -> navController.popBackStack() }
            )
        }

        composable("add_address") {
            CadastrarEnderecoScreen(
                onBackClick = { navController.popBackStack() },
                onSave = { navController.popBackStack() }
            )
        }

        composable("payment_method_selection") {
            PaymentMethodScreen(
                onNavigateBack = { navController.popBackStack() },
                onPaymentMethodSelected = { methodName ->
                    when (methodName) {
                        "Pix" -> navController.navigate("pix_payment")
                        "Cartão de Crédito" -> navController.navigate("cartao_credito")
                        "Cartão de Débito" -> navController.navigate("cartao_debito")
                        else -> navController.popBackStack()
                    }
                }
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
        
        composable("pix_payment") {
            PixPaymentScreen(
                totalAmount = 250.0, // TODO: Passar valor real
                onPaymentSuccess = { navController.navigate("payment_success") },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("pix_payment_waiting") {
            PixPaymentScreen(
                totalAmount = 250.0,
                onPaymentSuccess = { navController.navigate("payment_success") },
                onBackClick = { navController.popBackStack() },
                variant = "waiting"
            )
        }
        composable("pix_payment_error") {
            PixPaymentScreen(
                totalAmount = 250.0,
                onPaymentSuccess = { navController.navigate("payment_success") },
                onBackClick = { navController.popBackStack() },
                variant = "error"
            )
        }
        composable("pix_payment_success") {
            PixPaymentScreen(
                totalAmount = 250.0,
                onPaymentSuccess = { navController.navigate("payment_success") },
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

        composable("payment_success") {
            PaymentSuccessScreen(
                totalAmount = 250.0,
                orderId = "#TG-0001",
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
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("create_work_order") {
            CreateWorkOrderScreen(
                onBackClick = { navController.popBackStack() },
                onWorkOrderCreated = { navController.navigate("proposals_inbox") }
            )
        }
        
        composable("proposals_received") {
            ProposalsReceivedScreen(
                onBackClick = { navController.popBackStack() },
                onProposalClick = { proposalId -> navController.navigate("proposal_detail/$proposalId") },
                onAcceptProposal = { proposalId -> 
                    // TODO: Implementar lógica de aceitar proposta
                    navController.navigate("proposal_detail/$proposalId")
                },
                onRejectProposal = { proposalId -> 
                    // TODO: Implementar lógica de rejeitar proposta
                    navController.popBackStack()
                }
            )
        }
        
        composable("proposal_detail/{proposalId}") { backStackEntry ->
            val proposalId = backStackEntry.arguments?.getString("proposalId") ?: ""
            ProposalDetailScreen(
                proposalId = proposalId,
                onBackClick = { navController.popBackStack() },
                onAcceptProposal = { proposalId -> 
                    // TODO: Implementar lógica de aceitar proposta
                    navController.popBackStack()
                },
                onRejectProposal = { proposalId -> 
                    // TODO: Implementar lógica de rejeitar proposta
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
            RateProviderScreen(
                providerName = "Carlos Montador", // TODO: Buscar dados reais
                serviceTitle = "Montagem de Guarda-roupa", // TODO: Buscar dados reais
                onBackClick = { navController.popBackStack() },
                onRatingSubmitted = { rating, comment -> 
                    // TODO: Implementar lógica de envio de avaliação
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

        // Rotas de anúncios
        composable("anuncios") {
            AnunciosScreen(
                onBackClick = { navController.popBackStack() },
                onComprarBanner = { navController.navigate("comprar_banner") },
                onVerDetalhe = { navController.navigate("anuncio_detalhe") }
            )
        }

        composable("anuncio_detalhe") {
            AnuncioDetalheScreen(
                onBackClick = { navController.popBackStack() },
                onComprarBanner = { navController.navigate("comprar_banner") }
            )
        }

        composable("comprar_banner") {
            ComprarBannerScreen(
                onBackClick = { navController.popBackStack() },
                onConfirmarCompra = { _ -> navController.popBackStack() }
            )
        }

        // Ads variants
        composable("anuncios_empty") {
            AnunciosScreen(
                onBackClick = { navController.popBackStack() },
                onComprarBanner = { navController.navigate("comprar_banner") },
                onVerDetalhe = { navController.navigate("anuncio_detalhe") },
                variant = "empty"
            )
        }
        composable("anuncios_cta_secondary") {
            AnunciosScreen(
                onBackClick = { navController.popBackStack() },
                onComprarBanner = { navController.navigate("comprar_banner") },
                onVerDetalhe = { navController.navigate("anuncio_detalhe") },
                variant = "cta_secondary"
            )
        }
        composable("anuncio_detalhe_disabled") {
            AnuncioDetalheScreen(
                onBackClick = { navController.popBackStack() },
                onComprarBanner = { },
                variant = "disabled"
            )
        }
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
                onNotificationClick = { notification ->
                    navController.navigate("notification_detail/${notification.id}")
                }
            )
        }
        composable("notifications_empty") {
            NotificationsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToNotificationsSettings = { navController.navigate("notification_settings") },
                onNotificationClick = { notification -> navController.navigate("notification_detail/${notification.id}") },
                variant = "empty"
            )
        }
        composable("notifications_unread") {
            NotificationsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToNotificationsSettings = { navController.navigate("notification_settings") },
                onNotificationClick = { notification -> navController.navigate("notification_detail/${notification.id}") },
                variant = "unread"
            )
        }
        composable("notifications_settings_mode") {
            NotificationsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToNotificationsSettings = { navController.navigate("notification_settings") },
                onNotificationClick = { notification -> navController.navigate("notification_detail/${notification.id}") },
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
                onDesignReview = { navController.navigate("design_review") },
                onTipoConta = { navController.navigate("account_type") },
                onAiSupport = { navController.navigate("ai_support") },
                onSeguranca = { navController.navigate("security_settings") }
            )
        }

        composable("design_review") {
            DesignReviewScreen(
                onNavigateToRoute = { route ->
                    navController.navigate(route)
                },
                onBackPressed = { navController.popBackStack() }
            )
        }

        // Settings routes
        composable("account") { AccountScreen(onBackClick = { navController.popBackStack() }) }
        composable("security_settings") {
            SecuritySettingsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToIdentityVerification = { navController.navigate("identity_verification") }
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
        composable("privacy") { PrivacyScreen(onBackClick = { navController.popBackStack() }) }
        composable("support") { SupportScreen(onBackClick = { navController.popBackStack() }) }
        composable("about") { AboutScreen(onBackClick = { navController.popBackStack() }) }

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
            val chatId = backStackEntry.arguments?.getString("chatId")?.toLongOrNull() ?: 0L
            ChatScreen(
                threadId = chatId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Profile subroutes
        composable("meus_servicos") {
            MeusServicosScreen(
                onBackClick = { navController.popBackStack() },
                onCriarServico = { navController.navigate("create_work_order") },
                onEditarServico = { serviceId -> navController.navigate("service_detail/$serviceId") }
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
                onSaved = { _ -> navController.navigate("manage_products") }
            )
        }

        composable("ads_alt") { AdsScreen(onBackClick = { navController.popBackStack() }) }
        composable("ai_support") { AiSupportScreen(onBackClick = { navController.popBackStack() }) }
        composable("account_type") { 
            AccountTypeScreen(
                onBackClick = { navController.popBackStack() },
                onSaveChanges = { navController.popBackStack() }
            )
        }

        // Rotas de pedidos
        composable("meus_pedidos") {
            MeusPedidosScreen(
                onBackClick = { navController.popBackStack() },
                onOrderClick = { orderId -> 
                    navController.navigate("order_detail/$orderId")
                }
            )
        }

        composable("order_detail/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            DetalhesPedidoScreen(
                orderId = orderId,
                onBackClick = { navController.popBackStack() },
                onRastrearPedido = { navController.navigate("order_tracking/$orderId") },
                onVerResumo = { id -> navController.navigate("order_summary_alt/$id") }
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
        composable("gerenciar_produtos_legacy") {
            GerenciarProdutosScreen(
                onBackClick = { navController.popBackStack() },
                onCriarProduto = { navController.navigate("criar_produto_legacy") },
                onEditarProduto = { id -> navController.navigate("editar_produto_legacy/$id") }
            )
        }
        composable("criar_produto_legacy") {
            CriarProdutoScreen(
                onBackClick = { navController.popBackStack() },
                onProductCreated = { navController.navigate("gerenciar_produtos_legacy") }
            )
        }
        composable("order_tracking_legacy/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId")?.toLongOrNull() ?: 0L
            OrderTrackingScreen(
                orderId = orderId,
                onNavigateBack = { navController.popBackStack() }
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