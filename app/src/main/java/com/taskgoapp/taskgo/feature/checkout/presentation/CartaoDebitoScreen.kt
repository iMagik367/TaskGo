package com.taskgoapp.taskgo.feature.checkout.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.theme.*

@Composable
fun CartaoDebitoScreen(
    onBackClick: () -> Unit,
    isAlt: Boolean = false
) {
    var nome by remember { mutableStateOf("") }
    var numero by remember { mutableStateOf("") }
    var validade by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf(false) }
    val enabled = nome.isNotBlank() && numero.length == 16 && validade.length == 5 && cvv.length == 3
    val bg = if(isAlt) TaskGoSurfaceGrayBg else TaskGoSurface
    Column(
        Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(if(isAlt)"Adicionar cartão de débito" else "Cartão de débito", style = FigmaSectionTitle)
        Spacer(Modifier.height(14.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors=CardDefaults.cardColors(containerColor = bg)) {
            Column(Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome no cartão") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = numero,
                    onValueChange = {
                        if(it.length <= 16) numero = it.filter { c -> c.isDigit() }
                    },
                    label = { Text("Número do cartão") },
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = validade,
                        onValueChange = {
                            if(it.length <= 5) validade = it
                        },
                        label = { Text("Validade (MM/AA)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cvv,
                        onValueChange = {
                            if(it.length <= 3) cvv = it.filter { ch -> ch.isDigit() }
                        },
                        label = { Text("CVV") },
                        singleLine = true,
                        visualTransformation = VisualTransformation.None,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Button(onClick={erro = !enabled}, enabled=enabled, modifier=Modifier.fillMaxWidth()) { Text("Confirmar") }
        if(erro) Text("Preencha corretamente todos os campos.", color = TaskGoError, style=FigmaProductDescription)
        OutlinedButton(onClick=onBackClick, modifier=Modifier.fillMaxWidth()) { Text("Voltar") }
    }
}
