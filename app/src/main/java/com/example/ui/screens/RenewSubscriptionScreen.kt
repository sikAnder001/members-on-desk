package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PaymentMode
import com.example.data.model.SubscriptionPlan
import com.example.ui.viewmodel.GymViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenewSubscriptionScreen(
    memberId: Long,
    viewModel: GymViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val member by viewModel.selectedMemberFlow.collectAsStateWithLifecycle()
    val plans by viewModel.activePlans.collectAsStateWithLifecycle()

    var selectedPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }
    var paymentMode by remember { mutableStateOf(PaymentMode.UPI) }
    var discountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, memberId) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPlans()
                viewModel.refreshMemberDetail(memberId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(plans) {
        if (selectedPlan == null && plans.isNotEmpty()) {
            selectedPlan = plans.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Renew Subscription", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Renewing for: ${member?.name ?: "Member"}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedPlan?.let { "${it.name} - ₹${it.price.toInt()} (${it.durationDays} days)" } ?: "Select Package",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Plan") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    plans.forEach { plan ->
                        DropdownMenuItem(
                            text = { Text("${plan.name} - ₹${plan.price.toInt()} (${plan.durationDays} days)") },
                            onClick = {
                                selectedPlan = plan
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = discountText,
                onValueChange = { discountText = it },
                label = { Text("Discount Amount (₹)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Payment Mode", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaymentMode.values().forEach { mode ->
                    FilterChip(
                        selected = paymentMode == mode,
                        onClick = { paymentMode = mode },
                        label = { Text(mode.name) }
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Remarks") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val plan = selectedPlan
                    if (plan == null) {
                        Toast.makeText(context, "Please select a plan", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val discount = discountText.toDoubleOrNull() ?: 0.0
                    viewModel.renewSubscription(
                        memberId = memberId,
                        plan = plan,
                        paymentMode = paymentMode,
                        discount = discount,
                        notes = notes
                    ) {
                        Toast.makeText(context, "Subscription renewed successfully!", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Confirm Renewal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
