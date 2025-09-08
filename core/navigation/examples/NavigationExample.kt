package com.example.taskgoapp.core.navigation.examples

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.taskgoapp.core.designsystem.theme.TaskGoTheme
import com.example.taskgoapp.core.navigation.TaskGoNavGraph

@Composable
fun NavigationExample() {
    TaskGoTheme {
        val navController = rememberNavController()
        
        TaskGoNavGraph(
            navController = navController
        )
    }
}
