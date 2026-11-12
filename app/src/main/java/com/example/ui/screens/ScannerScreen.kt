package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScanProfile
import com.example.data.scanner.ScanOrchestrator
import com.example.ui.CyberShieldViewModel
import com.example.ui.ScreenTab
import com.example.ui.theme.ChartScore
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberSurfaceCard
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.SeverityCritical
import com.example.ui.theme.SeverityCriticalBg
import com.example.ui.theme.SeveritySafe

@Composable
fun ScannerScreen(
    viewModel: CyberShieldViewModel,
    onNavigate: (ScreenTab) -> Unit
) {
    var targetUrl by remember { mutableStateOf("nexova.com") }
    var selectedProfile by remember { mutableStateOf(ScanProfile.FULL_AUDIT) }
    var authorized by remember { mutableStateOf(true) }
    var scanDescription by remember { mutableStateOf("Production quarterly posture audit") }

    val scanProgress by viewModel.scanProgress.collectAsState()
    val isScanning = scanProgress is ScanOrchestrator.ScanProgress.Progress

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("scanner_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        item {
            Column {
                Text(
                    text = "Website Security Scanner",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyberTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Safe defensive posture audit: SSL/TLS, Security Headers & Transport",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberTextSecondary
                )
            }
        }

        // Scanner Input Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Target Website",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyberTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = targetUrl,
                        onValueChange = { targetUrl = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("target_url_input"),
                        placeholder = { Text("e.g. nexova.com or example.com", color = CyberTextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = CyberCyan)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary,
                            cursorColor = CyberCyan
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickDomainChip(label = "nexova.com", onClick = { targetUrl = "nexova.com" })
                        QuickDomainChip(label = "github.com", onClick = { targetUrl = "github.com" })
                        QuickDomainChip(label = "wikipedia.org", onClick = { targetUrl = "wikipedia.org" })
                        QuickDomainChip(label = "google.com", onClick = { targetUrl = "google.com" })
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scan Profile Selector
                    Text(
                        text = "Scan Profile",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyberTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScanProfile.values().forEach { profile ->
                            ProfileOptionCard(
                                profile = profile,
                                isSelected = selectedProfile == profile,
                                onSelect = { selectedProfile = profile }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Authorization Consent Box (Crucial Security Restriction)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { authorized = !authorized }
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Checkbox(
                                checked = authorized,
                                onCheckedChange = { authorized = it },
                                modifier = Modifier.testTag("authorization_checkbox"),
                                colors = CheckboxDefaults.colors(
                                    checkedColor = CyberCyan,
                                    uncheckedColor = CyberBorder,
                                    checkmarkColor = CyberSurfaceCard
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Documented Authorization & Domain Ownership",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (authorized) ChartScore else SeverityCritical
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "I explicitly confirm I own or have written authorization to assess this target. The scan executes non-destructive defensive checks and enforces SSRF restrictions.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Start Scan Button
                    Button(
                        onClick = {
                            viewModel.startScan(targetUrl, selectedProfile, authorized)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("start_scan_button"),
                        enabled = !isScanning && targetUrl.isNotBlank() && authorized,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = Color(0xFF090D16),
                            disabledContainerColor = CyberSurfaceElevated,
                            disabledContentColor = CyberTextMuted
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF090D16),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Auditing Target...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Launch Security Scan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Live Scanning Progress Feedback
        item {
            AnimatedVisibility(visible = scanProgress != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        when (val p = scanProgress) {
                            is ScanOrchestrator.ScanProgress.Progress -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Scan in Progress",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberCyan
                                    )
                                    Text(
                                        text = "${(p.percent * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyberTextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { p.percent },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = CyberCyan,
                                    trackColor = CyberSurfaceElevated,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = p.stage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CyberTextSecondary
                                )
                            }
                            is ScanOrchestrator.ScanProgress.Completed -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SeveritySafe)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Scan Completed Successfully!",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SeveritySafe
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Defensive Security Score: ${p.scan.overallScore}/100 (Grade: ${p.scan.grade})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CyberTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = p.scan.executiveSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CyberTextSecondary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onNavigate(ScreenTab.FINDINGS) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated)
                                    ) {
                                        Text("View Findings (${p.findings.size})", color = CyberCyan)
                                    }
                                    Button(
                                        onClick = { onNavigate(ScreenTab.AI_ANALYST) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                                    ) {
                                        Text("AI Fix Advice", color = Color(0xFF090D16))
                                    }
                                }
                            }
                            is ScanOrchestrator.ScanProgress.Error -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = SeverityCritical)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Scan Blocked / Failed",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SeverityCritical
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = p.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CyberTextSecondary
                                )
                            }
                            null -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickDomainChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(CyberSurfaceElevated, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF38BDF8), fontSize = 11.sp)
    }
}

@Composable
private fun ProfileOptionCard(
    profile: ScanProfile,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) CyberCyan.copy(alpha = 0.08f) else CyberSurfaceElevated,
                RoundedCornerShape(8.dp)
            )
            .clickable { onSelect() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(if (isSelected) CyberCyan else Color.Transparent, CircleShape)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF090D16), modifier = Modifier.size(14.dp))
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = profile.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) CyberCyan else CyberTextPrimary
            )
            Text(
                text = profile.description,
                style = MaterialTheme.typography.labelSmall,
                color = CyberTextMuted,
                fontSize = 11.sp
            )
        }
    }
}
