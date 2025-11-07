package com.taskgoapp.taskgo.feature.notifications.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskgoapp.taskgo.core.theme.TaskGoBackgroundWhite
import com.taskgoapp.taskgo.core.theme.TaskGoGreen
import com.taskgoapp.taskgo.core.theme.TaskGoTextBlack
import com.taskgoapp.taskgo.core.theme.TaskGoTextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
	notificationId: String,
	title: String,
	message: String,
	timestamp: String,
	onBackClick: () -> Unit
) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(TaskGoBackgroundWhite)
	) {
		TopAppBar(
			title = {
				Text(
					text = "Notificação",
					color = TaskGoTextBlack,
					fontSize = 18.sp,
					fontWeight = FontWeight.Bold
				)
			},
			navigationIcon = {
				IconButton(onClick = onBackClick) {
					Icon(
						imageVector = Icons.Filled.ArrowBack,
						contentDescription = "Voltar",
						tint = TaskGoTextBlack
					)
				}
			},
			colors = TopAppBarDefaults.topAppBarColors(
				containerColor = TaskGoBackgroundWhite
			)
		)

		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {
			Text(
				text = title,
				color = TaskGoTextBlack,
				fontSize = 20.sp,
				fontWeight = FontWeight.Bold
			)
			Text(
				text = timestamp,
				color = TaskGoTextGray,
				fontSize = 12.sp
			)
			Divider(color = Color(0xFFE0E0E0))
			Text(
				text = message,
				color = TaskGoTextBlack,
				fontSize = 14.sp,
				lineHeight = 20.sp
			)

			Spacer(modifier = Modifier.height(24.dp))

			Button(
				onClick = onBackClick,
				modifier = Modifier
					.fillMaxWidth()
					.height(52.dp),
				colors = ButtonDefaults.buttonColors(
					containerColor = TaskGoGreen
				),
				shape = MaterialTheme.shapes.medium
			) {
				Text(
					text = "Voltar",
					color = Color.White,
					fontWeight = FontWeight.Bold,
					fontSize = 16.sp,
					textAlign = TextAlign.Center
				)
			}
		}
	}
}
