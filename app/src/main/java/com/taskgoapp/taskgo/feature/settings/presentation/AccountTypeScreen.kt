package com.taskgoapp.taskgo.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.taskgoapp.taskgo.core.theme.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskgoapp.taskgo.core.design.AppTopBar

enum class AccountType(val displayName: String) {
    PROVIDER("Prestador"),
    CLIENT("Cliente"),
    SELLER("Vendedor")
}

@Composable
fun AccountTypeScreen(
    onBackClick: () -> Unit,
    onSaveChanges: () -> Unit
) {
    var selectedAccountType by remember { mutableStateOf(AccountType.CLIENT) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header
        AppTopBar(
            title = "Tipo de conta",
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
            AccountType.values().forEach { accountType ->
                AccountTypeItem(
                    accountType = accountType,
                    isSelected = selectedAccountType == accountType,
                    onAccountTypeSelected = { selectedAccountType = accountType }
                )
                if (accountType != AccountType.values().last()) {
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
                        color = Color(0xFFE53E3E),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSaveChanges() },
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
private fun AccountTypeItem(
    accountType: AccountType,
    isSelected: Boolean,
    onAccountTypeSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAccountTypeSelected() }
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

        // Account type name
        Text(
            text = accountType.displayName,
            color = TaskGoTextDarkGray,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
