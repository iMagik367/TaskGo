package com.taskgoapp.taskgo.feature.ads.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskgoapp.taskgo.core.theme.TaskGoBackgroundWhite
import com.taskgoapp.taskgo.core.theme.TaskGoGreen
import com.taskgoapp.taskgo.core.theme.TaskGoTextBlack
import com.taskgoapp.taskgo.core.theme.TaskGoTextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprarBannerScreen(
	onBackClick: () -> Unit,
	onConfirmarCompra: (String) -> Unit,
	variant: String? = null
) {
	var plano by remember { mutableStateOf("pequeno") }

	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(TaskGoBackgroundWhite)
	) {
		TopAppBar(
			title = { Text("Anúncios", color = TaskGoTextBlack, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
			navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TaskGoTextBlack)
                }
			},
			colors = TopAppBarDefaults.topAppBarColors(containerColor = TaskGoBackgroundWhite)
		)

		Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
			Text("Escolha seu plano de banner", color = TaskGoTextBlack, fontSize = 20.sp, fontWeight = FontWeight.Bold)
			Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
				Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
					Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
						Text("Pequeno", color = TaskGoTextGray)
						Text("R$ 50/dia", color = TaskGoTextBlack, fontWeight = FontWeight.Bold)
					}
                    RadioButtonRow("pequeno", plano) { plano = it }
                    HorizontalDivider()
					Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
						Text("Grande", color = TaskGoTextGray)
						Text("R$ 90/dia", color = TaskGoTextBlack, fontWeight = FontWeight.Bold)
					}
					RadioButtonRow("grande", plano) { plano = it }
				}
			}

			Spacer(Modifier.height(12.dp))
			Button(
				onClick = { onConfirmarCompra(plano) },
				modifier = Modifier.fillMaxWidth().height(56.dp),
				enabled = variant != "disabled",
				colors = ButtonDefaults.buttonColors(containerColor = TaskGoGreen)
			) {
				Text("Confirmar compra", color = Color.White, fontWeight = FontWeight.Bold)
			}
		}
	}
}

@Composable
private fun RadioButtonRow(value: String, selected: String, onSelect: (String) -> Unit) {
	Row(verticalAlignment = Alignment.CenterVertically) {
		RadioButton(selected = value == selected, onClick = { onSelect(value) }, colors = RadioButtonDefaults.colors(selectedColor = TaskGoGreen))
		Spacer(Modifier.width(8.dp))
		Text(text = if (value == "pequeno") "Pequeno" else "Grande", color = TaskGoTextBlack)
	}
}

