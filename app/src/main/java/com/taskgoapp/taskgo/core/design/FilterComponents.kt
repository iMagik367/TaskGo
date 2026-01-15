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
    val location: LocationFilter? = null,
    val sortBy: SortOption = SortOption.RELEVANCE,
    val searchQuery: String = "",
    val minRating: Double? = null,
    val useLocationRadius: Boolean = false
)

data class PriceRange(
    val min: Double? = null,
    val max: Double? = null
)

data class LocationFilter(
    val city: String? = null,
    val state: String? = null,
    val radiusKm: Int? = null // Raio em km
)

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
                
                // Filtro de Localização
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Localização",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoTextBlack
                    )
                    
                    // Estado - Dropdown
                    var selectedState by remember(filterState.location?.state) {
                        mutableStateOf(filterState.location?.state ?: "")
                    }
                    var expandedState by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = expandedState,
                        onExpandedChange = { expandedState = !expandedState }
                    ) {
                        OutlinedTextField(
                            value = selectedState,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Estado (UF)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedState) },
                            placeholder = { Text("Selecione o estado") }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedState,
                            onDismissRequest = { expandedState = false }
                        ) {
                            com.taskgoapp.taskgo.core.data.BrazilianCities.allStates.forEach { state ->
                                DropdownMenuItem(
                                    text = { Text(state) },
                                    onClick = {
                                        selectedState = state
                                        expandedState = false
                                        onFilterStateChange(
                                            filterState.copy(
                                                location = filterState.location?.copy(state = state, city = null) 
                                                    ?: LocationFilter(state = state)
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Cidade - Dropdown (carrega baseado no estado selecionado)
                    var selectedCity by remember(filterState.location?.city, selectedState) {
                        mutableStateOf(filterState.location?.city ?: "")
                    }
                    var expandedCity by remember { mutableStateOf(false) }
                    val availableCities = remember(selectedState) {
                        if (selectedState.isNotEmpty()) {
                            com.taskgoapp.taskgo.core.data.BrazilianCities.getCitiesForState(selectedState)
                        } else {
                            emptyList()
                        }
                    }
                    
                    ExposedDropdownMenuBox(
                        expanded = expandedCity,
                        onExpandedChange = { expandedCity = !expandedCity }
                    ) {
                        OutlinedTextField(
                            value = selectedCity,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Cidade") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCity) },
                            placeholder = { Text(if (selectedState.isEmpty()) "Selecione primeiro o estado" else "Selecione a cidade") },
                            enabled = selectedState.isNotEmpty()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCity,
                            onDismissRequest = { expandedCity = false }
                        ) {
                            availableCities.forEach { city ->
                                DropdownMenuItem(
                                    text = { Text(city) },
                                    onClick = {
                                        selectedCity = city
                                        expandedCity = false
                                        onFilterStateChange(
                                            filterState.copy(
                                                location = filterState.location?.copy(city = city)
                                                    ?: LocationFilter(state = selectedState, city = city)
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Checkbox para usar localização atual
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onFilterStateChange(
                                    filterState.copy(
                                        useLocationRadius = !filterState.useLocationRadius
                                    )
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = filterState.useLocationRadius,
                            onCheckedChange = { checked ->
                                onFilterStateChange(
                                    filterState.copy(useLocationRadius = checked)
                                )
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Usar minha localização atual",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TaskGoTextBlack
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Raio - Slider (habilitado apenas se usar localização)
                    if (filterState.useLocationRadius) {
                        var sliderValue by remember(filterState.location?.radiusKm, filterState.useLocationRadius) {
                            mutableStateOf((filterState.location?.radiusKm?.takeIf { it in 10..100 } ?: 10).toFloat())
                        }
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Raio: ${sliderValue.toInt()} km",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TaskGoTextBlack
                            )
                            
                            Slider(
                                value = sliderValue,
                                onValueChange = { newValue ->
                                    sliderValue = newValue
                                    val radiusKm = newValue.toInt()
                                    onFilterStateChange(
                                        filterState.copy(
                                            location = filterState.location?.copy(radiusKm = radiusKm)
                                                ?: LocationFilter(radiusKm = radiusKm)
                                        )
                                    )
                                },
                                valueRange = 10f..100f,
                                steps = 8, // Incrementos de 10km (10, 20, 30, ..., 100)
                                colors = SliderDefaults.colors(
                                    thumbColor = TaskGoGreen,
                                    activeTrackColor = TaskGoGreen,
                                    inactiveTrackColor = Color(0xFFE0E0E0)
                                )
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "10 km",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TaskGoTextGray
                                )
                                Text(
                                    text = "100 km",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TaskGoTextGray
                                )
                            }
                        }
                    }
                    
                }
                
                HorizontalDivider()
                
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

