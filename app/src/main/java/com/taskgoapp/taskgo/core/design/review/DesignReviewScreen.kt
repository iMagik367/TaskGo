package com.taskgoapp.taskgo.core.design.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.R
import com.taskgoapp.taskgo.core.theme.*

data class RouteSpec(
    val name: String,
    val route: String,
    val drawableRes: Int
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DesignReviewScreen(
    onNavigateToRoute: (String) -> Unit,
    onBackPressed: () -> Unit
) {
    var selectedScreenshot by remember { mutableStateOf(screenshots.first()) }
    var overlayEnabled by remember { mutableStateOf(DesignReviewState.overlayEnabled) }
    var overlayAlpha by remember { mutableStateOf(DesignReviewState.overlayAlpha) }
    var gridEnabled by remember { mutableStateOf(DesignReviewState.gridEnabled) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Design Review") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundWhite
                ),
                border = BorderStroke(1.dp, TaskGoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Selecionar Screenshot",
                        style = MaterialTheme.typography.titleMedium
                    )
                    // Overlay toggles
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = overlayEnabled, onCheckedChange = {
                            overlayEnabled = it
                            DesignReviewState.overlayEnabled = it
                            if (it) DesignReviewState.overlayScreenshotRes = selectedScreenshot.drawableRes
                        })
                        Spacer(Modifier.width(8.dp))
                        Text("Ativar overlay global")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = gridEnabled, onCheckedChange = {
                            gridEnabled = it
                            DesignReviewState.gridEnabled = it
                        })
                        Spacer(Modifier.width(8.dp))
                        Text("Mostrar grade de 8dp")
                    }
                    
                    // Alpha Slider
                    if (overlayEnabled) {
                        Column {
                            Text(
                                text = "Opacidade: ${(overlayAlpha * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = overlayAlpha,
                                onValueChange = { overlayAlpha = it },
                                valueRange = 0f..1f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            // Persist state globally
                            LaunchedEffect(overlayAlpha) {
                                DesignReviewState.overlayAlpha = overlayAlpha
                            }
                        }
                    }
                    
                    // Navigation Button
                    Button(
                        onClick = { onNavigateToRoute(selectedScreenshot.route) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Abrir UI Alvo: ${selectedScreenshot.name}")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Screenshot List
            Text(
                text = "Screenshots Disponíveis",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(screenshots) { screenshot ->
                    ScreenshotItem(
                        screenshot = screenshot,
                        isSelected = selectedScreenshot == screenshot,
                        onClick = {
                            selectedScreenshot = screenshot
                            if (overlayEnabled) {
                                DesignReviewState.overlayScreenshotRes = screenshot.drawableRes
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ScreenshotItem(
    screenshot: RouteSpec,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = TaskGoBackgroundWhite
        ),
        border = BorderStroke(1.dp, TaskGoBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(screenshot.drawableRes),
                contentDescription = screenshot.name,
                modifier = Modifier
                    .size(60.dp)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = screenshot.name,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = screenshot.route,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun OverlayCompare(
    ui: @Composable () -> Unit,
    screenshotRes: Int,
    alpha: Float
) {
    Box(modifier = Modifier.fillMaxSize()) {
        ui()
        Image(
            painter = painterResource(screenshotRes),
            contentDescription = "Screenshot overlay",
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha),
            contentScale = ContentScale.FillBounds
        )
    }
}

val screenshots = listOf(
    RouteSpec("Início", "home", R.drawable.inicio),
    RouteSpec("Serviços", "services", R.drawable.criar_ordem_de_servico),
    RouteSpec("Produtos", "products", R.drawable.gerencie_seus_produtos),
    RouteSpec("Mensagens", "messages", R.drawable.mensagens),
    RouteSpec("Perfil", "profile", R.drawable.meus_dados),
    RouteSpec("Configurações", "settings", R.drawable.configuracoes),
    RouteSpec("Carrinho", "cart", R.drawable.carrinho_1),
    RouteSpec("Finalizar Pedido", "checkout", R.drawable.finalizar_pedido),
    RouteSpec("Detalhes do Produto", "productDetail", R.drawable.detalhes_do_produto_1),
    RouteSpec("Detalhes da Proposta", "proposalDetail", R.drawable.detalhes_da_proposta_1),
    RouteSpec("Meus Pedidos", "myOrders", R.drawable.meus_pedidos_em_andamento),
    RouteSpec("Notificações", "notifications", R.drawable.notificacoes),
    RouteSpec("Cadastro", "signup", R.drawable.cadastro),
    RouteSpec("Login Loja", "loginStore", R.drawable.login_loja),
    RouteSpec("Anúncios", "ads", R.drawable.anuncios),
    RouteSpec("Cartão de Crédito", "paymentMethod", R.drawable.cartao_de_credito),
    RouteSpec("Confirmar Proposta", "confirmProposal", R.drawable.confirmar_proposta_1),
    RouteSpec("Confirmar Pix", "confirmPix", R.drawable.confirmacao_pix),
    RouteSpec("Cadastrar Endereço", "addressBook", R.drawable.cadastrar_endereco),
    RouteSpec("Idioma", "language", R.drawable.idioma),
    RouteSpec("Conta", "account", R.drawable.conta),
    RouteSpec("Minhas Avaliações", "myReviews", R.drawable.minhas_avaliacoes),
    RouteSpec("Histórico de Serviços", "serviceHistory", R.drawable.historico_de_servicos),
    RouteSpec("Alterar Senha", "changePassword", R.drawable.alterar_senha),
    RouteSpec("Avaliar Prestador", "rateProvider", R.drawable.avaliar_prestador)
)
