package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MonitoredAsset
import com.example.ui.CyberShieldViewModel
import com.example.ui.theme.ChartScore
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberSurfaceCard
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.SeverityCritical
import com.example.ui.theme.SeverityHigh
import com.example.ui.theme.SeveritySafe
import com.example.ui.theme.SeveritySafeBg

@Composable
fun AssetsScreen(
    viewModel: CyberShieldViewModel
) {
    val assets by viewModel.monitoredAssets.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("assets_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monitored Assets",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyberTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Verified domains configured for automated defense tracking",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyberTextSecondary
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_domain_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF090D16))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Domain", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
                }
            }
        }

        items(assets) { asset ->
            AssetCard(asset)
        }
    }

    if (showAddDialog) {
        AddDomainDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { domain, method ->
                viewModel.addAndVerifyAsset(domain, method)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AssetCard(asset: MonitoredAsset) {
    var monitoring by remember { mutableStateOf(asset.monitoringEnabled) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(SeveritySafeBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = SeveritySafe,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = asset.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyberTextPrimary
                        )
                        Text(
                            text = asset.domain,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Score Badge
                Box(
                    modifier = Modifier
                        .background(if (asset.lastScore >= 70) Color(0x2210B981) else SeverityHigh.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${asset.lastScore}/100",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (asset.lastScore >= 70) ChartScore else SeverityHigh
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Verification Token", style = MaterialTheme.typography.labelSmall, color = CyberTextMuted)
                    Text(asset.verificationToken, style = MaterialTheme.typography.labelSmall, color = CyberTextSecondary, fontFamily = FontFamily.Monospace)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("SSL Expiration", style = MaterialTheme.typography.labelSmall, color = CyberTextMuted)
                    Text("${asset.certExpiryDays} days left", style = MaterialTheme.typography.labelSmall, color = if (asset.certExpiryDays < 30) SeverityHigh else ChartScore, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Monitoring Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberSurfaceElevated, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Automated Daily Audits", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = CyberTextPrimary)
                        Text("Alert on certificate expiration & header degradation", style = MaterialTheme.typography.labelSmall, color = CyberTextMuted, fontSize = 10.sp)
                    }
                }
                Switch(
                    checked = monitoring,
                    onCheckedChange = { monitoring = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF090D16),
                        checkedTrackColor = CyberCyan,
                        uncheckedThumbColor = CyberTextMuted,
                        uncheckedTrackColor = CyberSurfaceCard
                    )
                )
            }
        }
    }
}

@Composable
private fun AddDomainDialog(
    onDismiss: () -> Unit,
    onConfirm: (domain: String, method: String) -> Unit
) {
    var domainInput by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("DNS_TXT") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurfaceCard,
        title = {
            Text("Add & Verify Domain Ownership", color = CyberTextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Security Policy requires documented authorization or DNS/Meta verification before automated scanning.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberTextSecondary
                )

                OutlinedTextField(
                    value = domainInput,
                    onValueChange = { domainInput = it },
                    label = { Text("Domain (e.g. staging.nexova.com)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = CyberTextPrimary,
                        unfocusedTextColor = CyberTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_domain_input"),
                    singleLine = true
                )

                Text("Verification Method:", style = MaterialTheme.typography.labelSmall, color = CyberTextPrimary, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (selectedMethod == "DNS_TXT") CyberCyan.copy(alpha = 0.15f) else CyberSurfaceElevated, RoundedCornerShape(6.dp))
                        .clickable { selectedMethod = "DNS_TXT" }
                        .padding(8.dp)
                ) {
                    Text("DNS TXT Record: cybershield-verify=...", style = MaterialTheme.typography.bodySmall, color = CyberTextPrimary)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (selectedMethod == "META_TAG") CyberCyan.copy(alpha = 0.15f) else CyberSurfaceElevated, RoundedCornerShape(6.dp))
                        .clickable { selectedMethod = "META_TAG" }
                        .padding(8.dp)
                ) {
                    Text("HTML Meta Tag: <meta name='cybershield-verification'>", style = MaterialTheme.typography.bodySmall, color = CyberTextPrimary)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (selectedMethod == "AUTHORIZED_ADMIN") CyberCyan.copy(alpha = 0.15f) else CyberSurfaceElevated, RoundedCornerShape(6.dp))
                        .clickable { selectedMethod = "AUTHORIZED_ADMIN" }
                        .padding(8.dp)
                ) {
                    Text("Explicit Owner Sign-off (sec@nexova.com)", style = MaterialTheme.typography.bodySmall, color = CyberTextPrimary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (domainInput.isNotBlank()) {
                        onConfirm(domainInput, selectedMethod)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                enabled = domainInput.isNotBlank()
            ) {
                Text("Verify & Save", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CyberTextSecondary)
            }
        }
    )
}
