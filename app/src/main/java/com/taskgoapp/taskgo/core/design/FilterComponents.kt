package com.taskgoapp.taskgo.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskgoapp.taskgo.core.theme.*

/**
 * Modelo de dados para filtros
 */
data class FilterState(
    val selectedCategories: Set<String> = emptySet(),
    val priceRange: PriceRange? = null,
    // REMOVIDO: location e useLocationRadius - localização sempre automática do perfil
    val sortBy: SortOption = SortOption.RELEVANCE,
    val searchQuery: String = "",
    val minRating: Double? = null
)

data class PriceRange(
    val min: Double? = null,
    val max: Double? = null
)

// REMOVIDO: LocationFilter - localização sempre automática do perfil

enum class SortOption(val label: String) {
    RELEVANCE("Relevância"),
    PRICE_LOW_TO_HIGH("Menor preço"),
    PRICE_HIGH_TO_LOW("Maior preço"),
    NEWEST("Mais recentes"),
    RATING("Melhor avaliados")
}

/**
 * Barra de filtros horizontal scrollável
 */
@Composable
fun FilterBar(
    categories: List<String>,
    selectedCategories: Set<String>,
    onCategorySelected: (String) -> Unit,
    onFilterClick: () -> Unit,
    showSortButtons: Boolean = false,
    sortBy: SortOption? = null,
    onSortByRating: (() -> Unit)? = null,
    onSortByNewest: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Botão de filtros avançados
        FilterChip(
            selected = false,
            onClick = onFilterClick,
            label = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text("Filtros")
                }
            },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = TaskGoSurfaceGray,
                labelColor = TaskGoTextBlack
            )
        )
        
        // Chips de categorias
        categories.forEach { category ->
            FilterChip(
                selected = selectedCategories.contains(category),
                onClick = { onCategorySelected(category) },
                label = { Text(category) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TaskGoGreen,
                    selectedLabelColor = TaskGoBackgroundWhite,
                    containerColor = TaskGoSurfaceGray,
                    labelColor = TaskGoTextBlack
                )
            )
        }
        
        // Botões de ordenação rápida (se solicitados)
        if (showSortButtons) {
            FilterChip(
                selected = sortBy == SortOption.RATING,
                onClick = { onSortByRating?.invoke() },
                label = { Text("⭐", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TaskGoGreen,
                    selectedLabelColor = Color.White,
                    containerColor = TaskGoSurfaceGray,
                    labelColor = TaskGoTextBlack
                )
            )
            FilterChip(
                selected = sortBy == SortOption.NEWEST,
                onClick = { onSortByNewest?.invoke() },
                label = { Text("Novos", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TaskGoGreen,
                    selectedLabelColor = Color.White,
                    containerColor = TaskGoSurfaceGray,
                    labelColor = TaskGoTextBlack
                )
            )
        }
    }
}

/**
 * Bottom sheet de filtros avançados
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    filterState: FilterState,
    onFilterStateChange: (FilterState) -> Unit,
    priceRanges: List<PriceRangeOption> = defaultPriceRanges,
    cities: List<String> = emptyList(),
    states: List<String> = defaultBrazilianStates,
    showPriceFilter: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (isOpen) {
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
        
        LaunchedEffect(Unit) {
            sheetState.expand()
        }
        
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            modifier = modifier
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Título
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtros",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoTextBlack
                    )
                    TextButton(onClick = {
                        onFilterStateChange(FilterState())
                    }) {
                        Text("Limpar tudo", color = TaskGoGreen)
                    }
                }
                
                // Filtro de Preço (apenas se showPriceFilter for true)
                if (showPriceFilter) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Faixa de Preço",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TaskGoTextBlack
                        )
                        
                        priceRanges.forEach { range ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onFilterStateChange(
                                            filterState.copy(priceRange = range.range)
                                        )
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = filterState.priceRange == range.range,
                                    onClick = {
                                        onFilterStateChange(
                                            filterState.copy(priceRange = range.range)
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = range.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TaskGoTextBlack
                                )
                            }
                        }
                    }
                    
                    HorizontalDivider()
                }
                
                // REMOVIDO: Filtro de Localização
                // Localização sempre automática do perfil do usuário
                // (HorizontalDivider removido - não necessário sem filtro de localização)
                
                // Filtro de Avaliação
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Avaliação Mínima",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoTextBlack
                    )
                    
                    val ratingOptions = listOf(
                        "Todas" to null,
                        "4+ estrelas" to 4.0,
                        "3+ estrelas" to 3.0,
                        "2+ estrelas" to 2.0,
                        "1+ estrelas" to 1.0
                    )
                    
                    ratingOptions.forEach { (label, rating) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onFilterStateChange(
                                        filterState.copy(minRating = rating)
                                    )
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = filterState.minRating == rating,
                                onClick = {
                                    onFilterStateChange(
                                        filterState.copy(minRating = rating)
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TaskGoTextBlack
                            )
                        }
                    }
                }
                
                HorizontalDivider()
                
                // Ordenação
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Ordenar por",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoTextBlack
                    )
                    
                    SortOption.values().forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onFilterStateChange(filterState.copy(sortBy = option))
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = filterState.sortBy == option,
                                onClick = {
                                    onFilterStateChange(filterState.copy(sortBy = option))
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TaskGoTextBlack
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Botão Confirmar
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TaskGoGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Confirmar",
                        color = TaskGoBackgroundWhite,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

data class PriceRangeOption(
    val label: String,
    val range: PriceRange
)

val defaultPriceRanges = listOf(
    PriceRangeOption("Até R$ 50", PriceRange(max = 50.0)),
    PriceRangeOption("R$ 50 - R$ 100", PriceRange(min = 50.0, max = 100.0)),
    PriceRangeOption("R$ 100 - R$ 250", PriceRange(min = 100.0, max = 250.0)),
    PriceRangeOption("R$ 250 - R$ 500", PriceRange(min = 250.0, max = 500.0)),
    PriceRangeOption("R$ 500 - R$ 1000", PriceRange(min = 500.0, max = 1000.0)),
    PriceRangeOption("Acima de R$ 1000", PriceRange(min = 1000.0))
)

val defaultBrazilianStates = listOf(
    "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA",
    "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN",
    "RS", "RO", "RR", "SC", "SP", "SE", "TO"
)

