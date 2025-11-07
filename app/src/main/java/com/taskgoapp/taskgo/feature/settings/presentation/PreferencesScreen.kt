package com.taskgoapp.taskgo.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.taskgoapp.taskgo.core.theme.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.design.AppTopBar

enum class ServiceCategory(val displayName: String) {
    ASSEMBLY("Montagem"),
    RENOVATION("Reforma"),
    GARDENING("Jardinagem"),
    ELECTRICAL("Elétrica"),
    CLEANING("Limpeza")
}

@Composable
fun PreferencesScreen(
    onBackClick: () -> Unit,
    onSaveChanges: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.state.collectAsState()
    var selectedCategories by remember {
        mutableStateOf(emptySet<ServiceCategory>())
    }

        Column(
            modifier = Modifier
                .fillMaxSize()
            .background(Color.White)
    ) {
        // Header
        AppTopBar(
            title = "Preferências",
            onBackClick = onBackClick,
            backgroundColor = TaskGoGreen,
            titleColor = Color.White,
            backIconColor = Color.White
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            ServiceCategory.values().forEach { category ->
                PreferenceItem(
                    category = category,
                    isSelected = selectedCategories.contains(category),
                    onCategorySelected = { isSelected ->
                        if (isSelected) {
                            selectedCategories = selectedCategories + category
                        } else {
                            selectedCategories = selectedCategories - category
                        }
                    }
                )
                if (category != ServiceCategory.values().last()) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(TaskGoDividerLight)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Save Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        color = TaskGoGreen,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        val json = selectedCategories.joinToString(prefix = "[", postfix = "]") { "\"${it.displayName}\"" }
                        viewModel.saveCategories(json)
                        onSaveChanges()
                    },
                contentAlignment = Alignment.Center
                ) {
                    Text(
                    text = "Salvar Alterações",
                    color = Color.White,
                    fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PreferenceItem(
    category: ServiceCategory,
    isSelected: Boolean,
    onCategorySelected: (Boolean) -> Unit
                    ) {
                        Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCategorySelected(!isSelected) }
            .padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
        // Selection indicator
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    color = if (isSelected) TaskGoGreen else TaskGoDivider,
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(2.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Category name
                                Text(
            text = category.displayName,
            color = TaskGoTextDarkGray,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )
    }
}


