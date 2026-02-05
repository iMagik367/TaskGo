package com.taskgoapp.taskgo.feature.ads.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.taskgoapp.taskgo.core.theme.TaskGoBorder
import com.taskgoapp.taskgo.core.theme.TaskGoGreen
import com.taskgoapp.taskgo.core.theme.TaskGoTextBlack
import com.taskgoapp.taskgo.core.theme.TaskGoTextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnuncioDetalheScreen(
	onBackClick: () -> Unit,
	onComprarBanner: () -> Unit,
	variant: String? = null
) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(TaskGoBackgroundWhite)
	) {
		TopAppBar(
			title = {
				Text("Anúncio", color = TaskGoTextBlack, fontSize = 18.sp, fontWeight = FontWeight.Bold)
			},
			navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TaskGoTextBlack)
                }
			},
			colors = TopAppBarDefaults.topAppBarColors(containerColor = TaskGoBackgroundWhite)
		)

		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp)
		) {
			Text("Divulgue seu trabalho!", color = TaskGoTextBlack, fontSize = 20.sp, fontWeight = FontWeight.Bold)
			Text(
				"Destaque seus serviços nos banners da página inicial de TaskGo",
				color = TaskGoTextGray,
				fontSize = 14.sp
			)

			Card(
				colors = CardDefaults.cardColors(containerColor = TaskGoBackgroundWhite),
				border = BorderStroke(1.dp, TaskGoBorder)
			) {
				Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
					Text("Planos", color = TaskGoTextBlack, fontWeight = FontWeight.SemiBold)
					Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
						Text("Pequeno", color = TaskGoTextGray)
						Text("R$ 50/dia", color = TaskGoTextBlack, fontWeight = FontWeight.Bold)
					}
					Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
						Text("Grande", color = TaskGoTextGray)
						Text("R$ 90/dia", color = TaskGoTextBlack, fontWeight = FontWeight.Bold)
					}
					Spacer(Modifier.height(8.dp))
					val comprarEnabled = variant != "disabled"
					Button(
						onClick = onComprarBanner,
						modifier = Modifier.fillMaxWidth().height(52.dp),
						enabled = comprarEnabled,
						colors = ButtonDefaults.buttonColors(containerColor = TaskGoGreen)
					) {
						Text("Comprar banner", color = Color.White, fontWeight = FontWeight.Bold)
					}
				}
			}
		}
	}
}

