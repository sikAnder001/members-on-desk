package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessType
import com.example.ui.viewmodel.GymViewModel

@Composable
fun RegistrationScreen(
    userType: BusinessType,
    viewModel: GymViewModel,
    onNavigateBack: () -> Unit,
    onRegistrationSuccess: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Registration is currently disabled. Please contact your administrator or log in directly.")
    }
}
