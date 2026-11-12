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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiSecurityAnalyst
import com.example.data.model.Finding
import com.example.data.model.Severity
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
import com.example.ui.theme.SeverityHigh
import com.example.ui.theme.SeverityHighBg
import com.example.ui.theme.SeverityLow
import com.example.ui.theme.SeverityLowBg
import com.example.ui.theme.SeverityMedium
import com.example.ui.theme.SeverityMediumBg
import com.example.ui.theme.SeveritySafe
import com.example.ui.theme.SeveritySafeBg

@Composable
fun FindingsScreen(
    viewModel: CyberShieldViewModel,
    onNavigate: (ScreenTab) -> Unit
) {
    val findings by viewModel.currentFindings.collectAsState()
    val selectedScan by viewModel.selectedScan.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedSeverityFilter by remember { mutableStateOf<Severity?>(null) }

    val filteredFindings = findings.filter { f ->
        (selectedSeverityFilter == null || f.severity == selectedSeverityFilter) &&
                (searchQuery.isBlank() || f.title.contains(searchQuery, ignoreCase = true) || f.category.contains(searchQuery, ignoreCase = true))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("findings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Title
        item {
            Column {
                Text(
                    text = "Vulnerability Discoveries",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyberTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${findings.size} total items identified on ${selectedScan?.domain ?: "nexova.com"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberTextSecondary
                )
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_findings_input"),
                placeholder = { Text("Filter findings by keyword or category...", color = CyberTextMuted) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = CyberCyan)
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
        }

        // Severity Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        label = "All (${findings.size})",
                        isSelected = selectedSeverityFilter == null,
                        color = CyberCyan,
                        onClick = { selectedSeverityFilter = null }
                    )
                }
                item {
                    val count = findings.count { it.severity == Severity.CRITICAL }
                    FilterChip(
                        label = "Critical ($count)",
                        isSelected = selectedSeverityFilter == Severity.CRITICAL,
                        color = SeverityCritical,
                        onClick = { selectedSeverityFilter = Severity.CRITICAL }
                    )
                }
                item {
                    val count = findings.count { it.severity == Severity.HIGH }
                    FilterChip(
                        label = "High ($count)",
                        isSelected = selectedSeverityFilter == Severity.HIGH,
                        color = SeverityHigh,
                        onClick = { selectedSeverityFilter = Severity.HIGH }
                    )
                }
                item {
                    val count = findings.count { it.severity == Severity.MEDIUM }
                    FilterChip(
                        label = "Medium ($count)",
                        isSelected = selectedSeverityFilter == Severity.MEDIUM,
                        color = SeverityMedium,
                        onClick = { selectedSeverityFilter = Severity.MEDIUM }
                    )
                }
                item {
                    val count = findings.count { it.severity == Severity.LOW }
                    FilterChip(
                        label = "Low ($count)",
                        isSelected = selectedSeverityFilter == Severity.LOW,
                        color = SeverityLow,
                        onClick = { selectedSeverityFilter = Severity.LOW }
                    )
                }
                item {
                    val count = findings.count { it.severity == Severity.SAFE }
                    FilterChip(
                        label = "Safe ($count)",
                        isSelected = selectedSeverityFilter == Severity.SAFE,
                        color = SeveritySafe,
                        onClick = { selectedSeverityFilter = Severity.SAFE }
                    )
                }
            }
        }

        if (filteredFindings.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No findings match your filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyberTextSecondary
                        )
                    }
                }
            }
        } else {
            items(filteredFindings) { finding ->
                FindingCard(
                    finding = finding,
                    onConsultAi = {
                        viewModel.sendAiQuery("How do I fix '${finding.title}' on ${finding.affectedUrl}?")
                        onNavigate(ScreenTab.AI_ANALYST)
                    }
                )
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) color.copy(alpha = 0.2f) else CyberSurfaceCard,
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) color else CyberTextSecondary
        )
    }
}

@Composable
private fun FindingCard(
    finding: Finding,
    onConsultAi: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val (badgeBg, badgeColor) = when (finding.severity) {
        Severity.CRITICAL -> Pair(SeverityCriticalBg, SeverityCritical)
        Severity.HIGH -> Pair(SeverityHighBg, SeverityHigh)
        Severity.MEDIUM -> Pair(SeverityMediumBg, SeverityMedium)
        Severity.LOW -> Pair(SeverityLowBg, SeverityLow)
        else -> Pair(SeveritySafeBg, SeveritySafe)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(badgeBg, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = finding.severity.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = finding.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (finding.cwe.isNotBlank()) {
                            Text(
                                text = finding.cwe,
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberTextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = finding.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyberTextPrimary
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = CyberTextMuted
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Evidence Snippet
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberSurfaceElevated, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = finding.evidence,
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberTextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }

            // Expanded details
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = "Technical Explanation:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyberTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = finding.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Potential Threat Impact:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SeverityCritical
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = finding.impact,
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Recommended Remediation:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ChartScore
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = finding.remediation,
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Ask AI button
                    Button(
                        onClick = onConsultAi,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ask AI Analyst for Remediation Snippet", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
