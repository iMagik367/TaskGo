package com.taskgoapp.taskgo.feature.orders.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskgoapp.taskgo.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersInProgressScreen(
	onBackClick: () -> Unit,
	onOrderClick: (String) -> Unit
) {
	val orders = remember {
		listOf(
			OrderItem("#1001", "Guarda roupa", "Previsão entrega 30/07/2025"),
			OrderItem("#1002", "Fogão de embutir", "Previsão entrega 02/08/2025")
		)
	}
	OrdersListBase(
		title = "Meus pedidos em andamento",
		badgeColor = Color(0xFFFFF3CD),
		badgeTextColor = Color(0xFF856404),
		orders = orders,
		onBackClick = onBackClick,
		onOrderClick = onOrderClick
	)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersCompletedScreen(
	onBackClick: () -> Unit,
	onOrderClick: (String) -> Unit
) {
	val orders = remember {
		listOf(
			OrderItem("#0991", "Furadeira sem fio", "Entregue em 12/07/2025")
		)
	}
	OrdersListBase(
		title = "Meus pedidos concluídos",
		badgeColor = Color(0xFFD4EDDA),
		badgeTextColor = Color(0xFF155724),
		orders = orders,
		onBackClick = onBackClick,
		onOrderClick = onOrderClick
	)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersCanceledScreen(
	onBackClick: () -> Unit,
	onOrderClick: (String) -> Unit
) {
	val orders = remember {
		listOf(
			OrderItem("#0981", "Martelo", "Pedido cancelado")
		)
	}
	OrdersListBase(
		title = "Meus pedidos cancelados",
		badgeColor = Color(0xFFF8D7DA),
		badgeTextColor = Color(0xFF721C24),
		orders = orders,
		onBackClick = onBackClick,
		onOrderClick = onOrderClick
	)
}

data class OrderItem(
	val id: String,
	val title: String,
	val subtitle: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrdersListBase(
	title: String,
	badgeColor: Color,
	badgeTextColor: Color,
	orders: List<OrderItem>,
	onBackClick: () -> Unit,
	onOrderClick: (String) -> Unit
) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(TaskGoBackgroundWhite)
	) {
		TopAppBar(
			title = {
				Text(text = title, color = TaskGoTextBlack, fontSize = 18.sp, fontWeight = FontWeight.Bold)
			},
			navigationIcon = {
				IconButton(onClick = onBackClick) {
					Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = TaskGoTextBlack)
				}
			},
			colors = TopAppBarDefaults.topAppBarColors(containerColor = TaskGoBackgroundWhite)
		)

		LazyColumn(
			modifier = Modifier
				.fillMaxSize()
				.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {
			items(orders) { order ->
				OrderCard(order, badgeColor, badgeTextColor) { onOrderClick(order.id) }
			}
		}
	}
}

@Composable
private fun OrderCard(
	order: OrderItem,
	badgeColor: Color,
	badgeTextColor: Color,
	onClick: () -> Unit
) {
	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(containerColor = TaskGoBackgroundWhite),
		elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
		border = BorderStroke(1.dp, TaskGoBorder)
	) {
		Column(Modifier.padding(16.dp)) {
			Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
				Text(text = order.title, color = TaskGoTextBlack, fontSize = 16.sp, fontWeight = FontWeight.Bold)
				Surface(color = badgeColor, shape = MaterialTheme.shapes.small) {
					Text(
						text = order.subtitle,
						color = badgeTextColor,
						fontSize = 10.sp,
						modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
					)
				}
			}
		}
	}
}

