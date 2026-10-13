package com.finora.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MessApp(vm: MessViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var memberName by remember { mutableStateOf("") }
    var bazarAmount by remember { mutableStateOf("") }
    var bazarNote by remember { mutableStateOf("") }
    var sharedTitle by remember { mutableStateOf("") }
    var sharedAmount by remember { mutableStateOf("") }

    val money = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Finora Mess Manager", style = MaterialTheme.typography.headlineSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = { vm.shiftMonth(-1) }) { Text("Previous") }
                Text(state?.month?.toString() ?: "Loading…", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { vm.shiftMonth(1) }) { Text("Next") }
            }

            state?.let { s ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Monthly Summary", style = MaterialTheme.typography.titleMedium)
                        Text("Deposits: ${money.format(s.summary.totals.deposit)}")
                        Text("Bazar: ${money.format(s.summary.totals.bazar)}")
                        Text("Meals: ${s.summary.totals.meals}")
                        Text("Meal rate: ${money.format(s.summary.totals.rate)}")
                        Text("Shared costs: ${money.format(s.summary.totals.shared)}")
                    }
                }

                Text("Add member", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = memberName,
                    onValueChange = { memberName = it },
                    label = { Text("Member name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = { if (memberName.isNotBlank()) { vm.addMember(memberName); memberName = "" } },
                    enabled = memberName.isNotBlank()
                ) { Text("Add member") }

                HorizontalDivider()
                Text("Add bazar expense", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = bazarAmount,
                    onValueChange = { bazarAmount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = bazarNote,
                    onValueChange = { bazarNote = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(onClick = {
                    val amount = bazarAmount.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        vm.addBazar(amount, s.month.lengthOfMonth().coerceAtMost(java.time.LocalDate.now().dayOfMonth), bazarNote)
                        bazarAmount = ""
                        bazarNote = ""
                    }
                }, enabled = (bazarAmount.toDoubleOrNull() ?: 0.0) > 0.0) { Text("Save bazar") }

                HorizontalDivider()
                Text("Members and balances", style = MaterialTheme.typography.titleMedium)
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = true),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(s.summary.rows, key = { it.member.id }) { row ->
                        Card {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(row.member.name, style = MaterialTheme.typography.titleMedium)
                                Text("Deposit: ${money.format(row.month.deposit)}")
                                Text("Meal cost: ${money.format(row.month.mealCost)}")
                                Text("Shared cost: ${money.format(row.month.shared)}")
                                Text("Balance: ${money.format(row.balance)}")
                                if (row.member.endMonth == null) {
                                    Button(onClick = { vm.archiveMember(row.member) }) { Text("Archive this month") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
