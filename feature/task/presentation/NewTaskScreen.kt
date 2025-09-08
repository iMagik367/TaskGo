package com.example.taskgoapp.feature.task.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.taskgoapp.core.designsystem.components.TgPrimaryButton
import com.example.taskgoapp.core.designsystem.components.TgPrimaryTextField
import com.example.taskgoapp.core.designsystem.theme.TaskGoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTaskScreen(
    onNavigateBack: () -> Unit,
    onTaskCreated: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nova Tarefa") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título da tela
            Text(
                text = "Criar Nova Tarefa",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Campo de título
            TgPrimaryTextField(
                value = title,
                onValueChange = { title = it },
                label = "Título da tarefa",
                placeholder = "Digite o título da tarefa"
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo de descrição
            TgPrimaryTextField(
                value = description,
                onValueChange = { description = it },
                label = "Descrição",
                placeholder = "Digite a descrição da tarefa",
                config = com.example.taskgoapp.core.designsystem.components.TgTextFieldConfig(
                    maxLines = 3,
                    singleLine = false
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo de prazo
            TgPrimaryTextField(
                value = deadline,
                onValueChange = { deadline = it },
                label = "Prazo",
                placeholder = "Ex: 25/12/2024 14:00"
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Botão de criar tarefa
            TgPrimaryButton(
                onClick = { 
                    // TODO: Implementar criação da tarefa
                    onTaskCreated("new-task-id")
                },
                text = "Criar Tarefa",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NewTaskScreenPreview() {
    TaskGoTheme {
        NewTaskScreen(
            onNavigateBack = {},
            onTaskCreated = {}
        )
    }
}
