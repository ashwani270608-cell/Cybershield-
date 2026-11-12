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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditLog
import com.example.data.model.SecurityScan
import com.example.ui.CyberShieldViewModel
import com.example.ui.ScreenTab
import com.example.ui.components.SecurityTimelineChart
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
import java.util.concurrent.TimeUnit

@Composable
fun DashboardScreen(
    viewModel: CyberShieldViewModel,
    onNavigate: (ScreenTab) -> Unit
) {
    val scans by viewModel.scans.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val selectedScan by viewModel.selectedScan.collectAsState()

    val currentScan = selectedScan ?: scans.firstOrNull()
    val score = currentScan?.overallScore ?: 73
    val scoreGrade = currentScan?.grade ?: "B"
    val criticalCount = currentScan?.criticalCount ?: 0
    val highCount = currentScan?.highCount ?: 10
    val mediumCount = currentScan?.mediumCount ?: 31
    val lowCount = currentScan?.lowCount ?: 17

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Security Overview",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyberTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Monitor your assets and vulnerabilities in real-time",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberTextSecondary
                )
            }
        }

        // Top 4 Metrics Row (2x2 grid for mobile)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Security,
                        iconTint = CyberCyan,
                        title = "Security Score",
                        value = "$score/100",
                        badge = if (score >= 85) "EXCELLENT" else if (score >= 70) "GOOD" else "ATTENTION",
                        badgeBg = if (score >= 70) Color(0x2210B981) else SeverityHighBg,
                        badgeColor = if (score >= 70) ChartScore else SeverityHigh,
                        trend = "↑ +1",
                        trendColor = ChartScore
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Visibility,
                        iconTint = Color(0xFF38BDF8),
                        title = "Discovered Assets",
                        value = "96",
                        trend = "↑ +11",
                        trendColor = ChartScore,
                        actionText = "View discoveries →",
                        onAction = { onNavigate(ScreenTab.FINDINGS) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Warning,
                        iconTint = SeverityCritical,
                        title = "Critical Issues",
                        value = "$criticalCount",
                        actionText = "Recommendations →",
                        onAction = { onNavigate(ScreenTab.AI_ANALYST) }
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Info,
                        iconTint = SeverityHigh,
                        title = "High Issues",
                        value = "$highCount",
                        actionText = "Recommendations →",
                        onAction = { onNavigate(ScreenTab.AI_ANALYST) }
                    )
                }
            }
        }

        // Interactive Security Timeline Chart
        item {
            SecurityTimelineChart()
        }

        // Recent Scans Card
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
                        Text(
                            text = "Recent Scans",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyberTextPrimary
                        )
                        Text(
                            text = "View all →",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.clickable { onNavigate(ScreenTab.SCANNER) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (scans.isEmpty()) {
                        Text(
                            text = "No scans completed yet. Run your first scan!",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyberTextMuted
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            scans.take(3).forEach { scan ->
                                ScanRowItem(
                                    scan = scan,
                                    isSelected = scan.id == currentScan?.id,
                                    onClick = { viewModel.selectScan(scan) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Activity Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyberTextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (auditLogs.isEmpty()) {
                        Text(
                            text = "No recorded activity events.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyberTextMuted
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            auditLogs.take(5).forEach { log ->
                                ActivityRowItem(log)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    badge: String? = null,
    badgeBg: Color = Color.Transparent,
    badgeColor: Color = CyberTextPrimary,
    trend: String? = null,
    trendColor: Color = ChartScore,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconTint.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (trend != null) {
                    Box(
                        modifier = Modifier
                            .background(trendColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = trend,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = trendColor,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyberTextPrimary
                )
                if (badge != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .background(badgeBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = CyberTextSecondary
            )

            if (actionText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onAction?.invoke() }
                )
            }
        }
    }
}

@Composable
private fun ScanRowItem(
    scan: SecurityScan,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) CyberSurfaceElevated else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(SeverityHighBg, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = SeverityHigh,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = scan.domain,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyberTextPrimary
                )
                Text(
                    text = formatTimeAgo(scan.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberTextMuted
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CountBadge(count = scan.highCount, bg = SeverityHighBg, textColor = SeverityHigh)
            CountBadge(count = scan.mediumCount, bg = SeverityMediumBg, textColor = SeverityMedium)
            CountBadge(count = scan.lowCount, bg = SeverityLowBg, textColor = SeverityLow)
        }
    }
}

@Composable
private fun CountBadge(count: Int, bg: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ActivityRowItem(log: AuditLog) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(Color(0xFF0284C7).copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.details,
                style = MaterialTheme.typography.bodySmall,
                color = CyberTextPrimary,
                maxLines = 1
            )
            Text(
                text = formatTimeAgo(log.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = CyberTextMuted,
                fontSize = 11.sp
            )
        }
    }
}

fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "$mins minutes ago"
        hours < 24 -> "$hours hours ago"
        days == 1L -> "1 days ago"
        else -> "$days days ago"
    }
}
