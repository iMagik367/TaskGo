package br.com.taskgo.taskgo.feature.checkout.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskgoapp.core.design.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastrarEnderecoScreen(
    onBackClick: () -> Unit
) {
    var nome by remember { mutableStateOf("Lucas Almeida") }
    var telefone by remember { mutableStateOf("(41) 99866-5237") }
    var cep by remember { mutableStateOf("85 816570") }
    var endereco by remember { mutableStateOf("Rua das Alamedas, 1000") }
    var bairro by remember { mutableStateOf("Liberdade") }
    var cidade by remember { mutableStateOf("São Paulo") }
    var estado by remember { mutableStateOf("SP") }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Cadastrar Endereço",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Nome
            Column {
                Text(
                    text = "Nome",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Telefone
            Column {
                Text(
                    text = "Telefone",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = telefone,
                    onValueChange = { telefone = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // CEP
            Column {
                Text(
                    text = "CEP",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = cep,
                    onValueChange = { cep = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Endereço
            Column {
                Text(
                    text = "Endereço",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = endereco,
                    onValueChange = { endereco = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Bairro
            Column {
                Text(
                    text = "Bairro",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = bairro,
                    onValueChange = { bairro = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Cidade
            Column {
                Text(
                    text = "Cidade",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = cidade,
                    onValueChange = { cidade = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Estado
            Column {
                Text(
                    text = "Estado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = estado,
                    onValueChange = { estado = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save button
            Button(
                onClick = { /* TODO: Salvar endereço */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Salvar",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
