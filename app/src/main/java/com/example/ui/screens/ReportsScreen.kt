package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.reporting.ReportExporter
import com.example.ui.CyberShieldViewModel
import com.example.ui.theme.ChartScore
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberSurfaceCard
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

@Composable
fun ReportsScreen(
    viewModel: CyberShieldViewModel
) {
    val context = LocalContext.current
    val selectedScan by viewModel.selectedScan.collectAsState()
    val findings by viewModel.currentFindings.collectAsState()

    var showReportViewer by remember { mutableStateOf<Pair<String, String>?>(null) } // Title to Content

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Vulnerability Reports",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyberTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Export and share production-grade audit documentation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberTextSecondary
                )
            }
        }

        if (selectedScan == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Please select or run a scan to generate compliance reports.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyberTextSecondary
                        )
                    }
                }
            }
        } else {
            val scan = selectedScan!!

            // Executive Summary Card
            item {
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
                            Column {
                                Text(
                                    text = scan.domain,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberTextPrimary
                                )
                                Text(
                                    text = "Scan ID: ${scan.id} • ${scan.scanProfile.title}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color(0x2210B981), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Score: ${scan.overallScore}/100",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ChartScore
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = scan.executiveSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyberTextSecondary,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val report = ReportExporter.generateExecutiveTextReport(scan, findings)
                                    ReportExporter.shareReport(context, report, "CyberShield Security Audit - ${scan.domain}")
                                },
                                modifier = Modifier.weight(1f).testTag("share_report_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF090D16), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Report", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val report = ReportExporter.generateExecutiveTextReport(scan, findings)
                                    showReportViewer = Pair("Executive Audit Document", report)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Preview Text", color = CyberCyan)
                            }
                        }
                    }
                }
            }

            // Export Formats Cards
            item {
                Text(
                    text = "Export Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyberTextPrimary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // JSON Export Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = CyberCyan)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("JSON Format", fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                            Text("Machine-readable structured vulnerability export", style = MaterialTheme.typography.labelSmall, color = CyberTextMuted, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val json = ReportExporter.generateJsonReport(scan, findings)
                                    showReportViewer = Pair("JSON Vulnerability Schema", json)
                                },
                                modifier = Modifier.fillMaxWidth().testTag("export_json_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated)
                            ) {
                                Text("Export JSON", color = CyberCyan, fontSize = 12.sp)
                            }
                        }
                    }

                    // CSV Export Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Default.TableChart, contentDescription = null, tint = ChartScore)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("CSV Spreadsheet", fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                            Text("Comma-separated values for spreadsheet tools", style = MaterialTheme.typography.labelSmall, color = CyberTextMuted, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val csv = ReportExporter.generateCsvReport(scan, findings)
                                    showReportViewer = Pair("CSV Spreadsheet Data", csv)
                                },
                                modifier = Modifier.fillMaxWidth().testTag("export_csv_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated)
                            ) {
                                Text("Export CSV", color = ChartScore, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Report Viewer Dialog
    showReportViewer?.let { (title, content) ->
        AlertDialog(
            onDismissRequest = { showReportViewer = null },
            containerColor = CyberSurfaceCard,
            title = {
                Text(title, color = CyberTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .background(CyberSurfaceElevated, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextSecondary,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("CyberShield Report", content)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Report copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("Copy to Clipboard", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportViewer = null }) {
                    Text("Close", color = CyberTextSecondary)
                }
            }
        )
    }
}
