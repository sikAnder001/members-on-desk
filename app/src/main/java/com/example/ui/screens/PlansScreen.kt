package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BusinessType
import com.example.data.model.SubscriptionPlan
import com.example.ui.viewmodel.GymViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(
    viewModel: GymViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    var newPlanName by remember { mutableStateOf("") }
    var newPlanDays by remember { mutableStateOf("30") }
    var newPlanPrice by remember { mutableStateOf("1500") }
    var newPlanDesc by remember { mutableStateOf("") }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPlans()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Membership Plans", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Plan")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Plan")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(plans) { plan ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(plan.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${plan.durationDays} days duration", style = MaterialTheme.typography.bodySmall)
                            if (plan.description.isNotBlank()) {
                                Text(plan.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("₹${plan.price.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Create Membership Plan") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newPlanName,
                            onValueChange = { newPlanName = it },
                            label = { Text("Plan Name") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newPlanDays,
                            onValueChange = { newPlanDays = it },
                            label = { Text("Duration (Days)") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newPlanPrice,
                            onValueChange = { newPlanPrice = it },
                            label = { Text("Price (₹)") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newPlanDesc,
                            onValueChange = { newPlanDesc = it },
                            label = { Text("Description") }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val price = newPlanPrice.toDoubleOrNull() ?: 500.0
                            val days = newPlanDays.toIntOrNull() ?: 30
                            if (newPlanName.isNotBlank()) {
                                viewModel.savePlan(
                                    SubscriptionPlan(
                                        name = newPlanName.trim(),
                                        durationDays = days,
                                        price = price,
                                        description = newPlanDesc.trim(),
                                        businessType = BusinessType.GYM
                                    )
                                ) {
                                    showAddDialog = false
                                    Toast.makeText(context, "Plan added successfully!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
