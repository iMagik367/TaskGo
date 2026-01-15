package com.taskgoapp.taskgo.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

data class PreferenceCategory(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val description: String,
    val subcategories: List<String> = emptyList()
)

@Composable
fun PreferencesScreen(
    onBackClick: () -> Unit,
    onSaveChanges: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    
    // Lista ampla de preferências
    val allPreferences = remember {
        listOf(
            PreferenceCategory(
                id = "services",
                name = "Serviços",
                icon = Icons.Default.Build,
                description = "Serviços que você precisa",
                subcategories = listOf(
                    "Montagem", "Reforma", "Jardinagem", "Elétrica", "Encanamento",
                    "Pintura", "Limpeza", "Marcenaria", "Alvenaria", "Hidráulica",
                    "Ar Condicionado", "Segurança", "Piscina", "Paisagismo", "Decoração"
                )
            ),
            PreferenceCategory(
                id = "products",
                name = "Produtos",
                icon = Icons.Default.ShoppingCart,
                description = "Produtos que você procura",
                subcategories = listOf(
                    "Eletrônicos", "Casa e Decoração", "Ferramentas", "Móveis",
                    "Roupas", "Esportes", "Livros", "Brinquedos", "Beleza e Cuidados",
                    "Automotivo", "Pet Shop", "Alimentos", "Bebidas", "Saúde"
                )
            ),
            PreferenceCategory(
                id = "price_range",
                name = "Faixa de Preço",
                icon = Icons.Default.AttachMoney,
                description = "Seu orçamento preferido",
                subcategories = listOf(
                    "Até R$ 50", "R$ 50 - R$ 100", "R$ 100 - R$ 250", "R$ 250 - R$ 500",
                    "R$ 500 - R$ 1000", "R$ 1000 - R$ 2500", "Acima de R$ 2500"
                )
            ),
            PreferenceCategory(
                id = "location",
                name = "Localização",
                icon = Icons.Default.LocationOn,
                description = "Onde você prefere buscar",
                subcategories = listOf(
                    "Próximo a mim (até 5km)", "Na minha cidade", "Na minha região",
                    "Em todo o estado", "Qualquer lugar"
                )
            ),
            PreferenceCategory(
                id = "urgency",
                name = "Urgência",
                icon = Icons.Default.Schedule,
                description = "Com que frequência você precisa",
                subcategories = listOf(
                    "Imediato (hoje)", "Esta semana", "Este mês", "Planejamento futuro"
                )
            ),
            PreferenceCategory(
                id = "quality",
                name = "Qualidade",
                icon = Icons.Default.Star,
                description = "Nível de qualidade desejado",
                subcategories = listOf(
                    "Premium (melhor qualidade)", "Alta qualidade", "Qualidade média",
                    "Boa relação custo-benefício"
                )
            )
        )
    }
    
    // Estado das preferências selecionadas
    var selectedPreferences by remember { mutableStateOf<Set<String>>(emptySet()) }
    var hasInitialized by remember { mutableStateOf(false) }
    var isSyncingFromRemote by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    var saveJob by remember { mutableStateOf<Job?>(null) }
    
    LaunchedEffect(settings.categories) {
        isSyncingFromRemote = true
        val parsed = if (settings.categories.isNotEmpty() && settings.categories != "[]") {
            runCatching {
                settings.categories
                    .removePrefix("[")
                    .removeSuffix("]")
                    .split(",")
                    .map { it.trim().removeSurrounding("\"") }
                    .filter { it.isNotBlank() }
                    .toSet()
            }.getOrDefault(emptySet())
        } else {
            emptySet()
        }
        selectedPreferences = parsed
        hasInitialized = true
        isSyncingFromRemote = false
    }
    
    fun queueSave(preferences: Set<String>) {
        if (!hasInitialized) return
        saveJob?.cancel()
        saveJob = coroutineScope.launch {
            delay(800)
            val json = preferences.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
            viewModel.saveCategories(json)
        }
    }
    
    LaunchedEffect(selectedPreferences) {
        if (hasInitialized && !isSyncingFromRemote) {
            queueSave(selectedPreferences)
        }
    }
    
    // Salvar quando sair da tela (garantir salvamento final)
    DisposableEffect(Unit) {
        onDispose {
            saveJob?.cancel()
            val json = selectedPreferences.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
            viewModel.saveCategories(json)
            onSaveChanges()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Preferências",
                subtitle = "Personalize suas recomendações",
                onBackClick = onBackClick,
                backgroundColor = TaskGoGreen,
                titleColor = Color.White,
                subtitleColor = Color.White,
                backIconColor = Color.White
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header informativo
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = TaskGoBackgroundGray
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Personalize Suas Recomendações",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TaskGoTextBlack
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Selecione suas preferências para receber recomendações personalizadas de produtos e serviços que realmente interessam a você.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TaskGoTextGray
                        )
                    }
                }
            }
            
            // Lista de categorias de preferências
            items(allPreferences) { category ->
                PreferenceCategoryCard(
                    category = category,
                    selectedSubcategories = selectedPreferences.filter { pref ->
                        category.subcategories.contains(pref)
                    }.toSet(),
                    onSubcategoryToggle = { subcategory ->
                        selectedPreferences = if (selectedPreferences.contains(subcategory)) {
                            selectedPreferences - subcategory
                        } else {
                            selectedPreferences + subcategory
                        }
                    }
                )
            }
            
            // Resumo das preferências selecionadas
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = TaskGoGreen.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = TaskGoGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Preferências Selecionadas",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TaskGoTextBlack
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${selectedPreferences.size} preferências selecionadas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TaskGoTextGray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PreferenceCategoryCard(
    category: PreferenceCategory,
    selectedSubcategories: Set<String>,
    onSubcategoryToggle: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header da categoria
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = TaskGoGreen,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoTextBlack
                    )
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TaskGoTextGray
                )
            }
            
            // Subcategorias (expandível)
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    category.subcategories.forEach { subcategory ->
                        PreferenceChip(
                            text = subcategory,
                            isSelected = selectedSubcategories.contains(subcategory),
                            onClick = { onSubcategoryToggle(subcategory) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = TaskGoGreen,
            selectedLabelColor = Color.White,
            containerColor = TaskGoSurfaceGray,
            labelColor = TaskGoTextBlack
        )
    )
}
