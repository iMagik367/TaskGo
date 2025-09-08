package com.example.taskgoapp.core.designsystem.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.taskgoapp.core.designsystem.theme.TaskGoTheme
import com.example.taskgoapp.core.designsystem.theme.taskGoSpacing
import com.example.taskgoapp.core.designsystem.theme.taskGoRadii
import com.example.taskgoapp.core.designsystem.components.*

@Composable
fun DesignSystemExamples() {
    TaskGoTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MaterialTheme.taskGoSpacing.medium)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.taskGoSpacing.large)
        ) {
            // Título
            Text(
                text = "TaskGo Design System",
                style = com.example.taskgoapp.core.designsystem.theme.TaskGoTypography.headlineLarge
            )
            
            // Seção de Botões
            Text(
                text = "Botões",
                style = com.example.taskgoapp.core.designsystem.theme.TaskGoTypography.titleLarge
            )
            
            // Botões Filled
            TgPrimaryButton(
                onClick = { /* Ação */ },
                text = "Botão Primário",
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(MaterialTheme.taskGoSpacing.small))
            
            TgPrimaryButton(
                onClick = { /* Ação */ },
                text = "Botão Desabilitado",
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
            
            Spacer(modifier = Modifier.height(MaterialTheme.taskGoSpacing.small))
            
            // Botões Tonal
            TgSecondaryButton(
                onClick = { /* Ação */ },
                text = "Botão Secundário",
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(MaterialTheme.taskGoSpacing.small))
            
            // Botões de Texto
            TgTextButton(
                onClick = { /* Ação */ },
                text = "Botão de Texto",
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(MaterialTheme.taskGoSpacing.medium))
            
            // Seção de Campos de Texto
            Text(
                text = "Campos de Texto",
                style = com.example.taskgoapp.core.designsystem.theme.TaskGoTypography.titleLarge
            )
            
            var text1 by remember { mutableStateOf("") }
            var text2 by remember { mutableStateOf("") }
            var text3 by remember { mutableStateOf("") }
            var text4 by remember { mutableStateOf("") }
            
            // Campo de texto padrão
            TgPrimaryTextField(
                value = text1,
                onValueChange = { text1 = it },
                label = "Campo de Texto",
                placeholder = "Digite algo aqui..."
            )
            
            Spacer(modifier = Modifier.height(MaterialTheme.taskGoSpacing.small))
            
            // Campo de texto com texto de ajuda
            TgPrimaryTextField(
                value = text2,
                onValueChange = { text2 = it },
                label = "Campo com Ajuda",
                placeholder = "Digite seu email",
                config = TgTextFieldConfig(
                    helperText = "Este campo é obrigatório"
                )
            )
            
            Spacer(modifier = Modifier.height(MaterialTheme.taskGoSpacing.small))
            
            // Campo de texto com erro
            TgErrorTextField(
                value = text3,
                onValueChange = { text3 = it },
                label = "Campo com Erro",
                placeholder = "Digite sua senha",
                errorText = "A senha deve ter pelo menos 6 caracteres"
            )
            
            Spacer(modifier = Modifier.height(MaterialTheme.taskGoSpacing.small))
            
            // Campo de texto desabilitado
            TgDisabledTextField(
                value = "Campo desabilitado",
                label = "Campo Desabilitado"
            )
            
            Spacer(modifier = Modifier.height(MaterialTheme.taskGoSpacing.medium))
            
            // Informações sobre espaçamento e raios
            Text(
                text = "Espaçamento e Raios",
                style = com.example.taskgoapp.core.designsystem.theme.TaskGoTypography.titleLarge
            )
            
            Text(
                text = "Espaçamento Extra Pequeno: ${MaterialTheme.taskGoSpacing.extraSmall.value}dp",
                style = com.example.taskgoapp.core.designsystem.theme.TaskGoTypography.bodyMedium
            )
            
            Text(
                text = "Espaçamento Pequeno: ${MaterialTheme.taskGoSpacing.small.value}dp",
                style = com.example.taskgoapp.core.designsystem.theme.TaskGoTypography.bodyMedium
            )
            
            Text(
                text = "Espaçamento Médio: ${MaterialTheme.taskGoSpacing.medium.value}dp",
                style = com.example.taskgoapp.core.designsystem.theme.TaskGoTypography.bodyMedium
            )
            
            Text(
                text = "Espaçamento Grande: ${MaterialTheme.taskGoSpacing.large.value}dp",
                style = com.example.taskgoapp.core.designsystem.theme.TaskGoTypography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(MaterialTheme.taskGoSpacing.small))
            
            Text(
                text = "Raio Pequeno: ${MaterialTheme.taskGoRadii.small.value}dp",
                style = com.example.taskgoapp.core.designsystem.theme.TaskGoTypography.bodyMedium
            )
            
            Text(
                text = "Raio Médio: ${MaterialTheme.taskGoRadii.medium.value}dp",
                style = com.example.taskgoapp.core.designsystem.theme.TaskGoTypography.bodyMedium
            )
            
            Text(
                text = "Raio Grande: ${MaterialTheme.taskGoRadii.large.value}dp",
                style = com.example.taskgoapp.core.designsystem.theme.TaskGoTypography.bodyMedium
            )
        }
    }
}
