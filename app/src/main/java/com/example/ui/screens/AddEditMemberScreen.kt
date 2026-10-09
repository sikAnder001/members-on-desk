package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PaymentMode
import com.example.data.model.SubscriptionPlan
import com.example.ui.viewmodel.GymViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMemberScreen(
    viewModel: GymViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val plans by viewModel.activePlans.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }
    var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var isPaid by remember { mutableStateOf(true) }
    var planDropdownExpanded by remember { mutableStateOf(false) }

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

    LaunchedEffect(plans) {
        if (selectedPlan == null && plans.isNotEmpty()) {
            selectedPlan = plans.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Member", fontWeight = FontWeight.Bold) },
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("add_member_name_input")
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("add_member_phone_input")
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Residential Address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = planDropdownExpanded,
                onExpandedChange = { planDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedPlan?.let { "${it.name} - ₹${it.price.toInt()} (${it.durationDays} days)" } ?: "Select Membership Plan",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Membership Package *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = planDropdownExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = planDropdownExpanded,
                    onDismissRequest = { planDropdownExpanded = false }
                ) {
                    plans.forEach { plan ->
                        DropdownMenuItem(
                            text = { Text("${plan.name} - ₹${plan.price.toInt()} (${plan.durationDays} days)") },
                            onClick = {
                                selectedPlan = plan
                                planDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Text("Payment Mode", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaymentMode.values().forEach { mode ->
                    FilterChip(
                        selected = paymentMode == mode,
                        onClick = { paymentMode = mode },
                        label = { Text(mode.name) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Collected Registration Fee?", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = isPaid, onCheckedChange = { isPaid = it })
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isBlank() || phone.isBlank()) {
                        Toast.makeText(context, "Please fill required fields (Name & Phone)", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val plan = selectedPlan
                    if (plan == null) {
                        Toast.makeText(context, "Please select a membership plan", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.addMember(
                        name = name.trim(),
                        phone = phone.trim(),
                        email = email.trim(),
                        address = address.trim(),
                        plan = plan,
                        paymentMode = paymentMode,
                        isPaid = isPaid
                    ) {
                        Toast.makeText(context, "Member added successfully!", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("add_member_save_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Member", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
