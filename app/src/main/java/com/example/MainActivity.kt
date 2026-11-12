package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CyberShieldViewModel
import com.example.ui.ScreenTab
import com.example.ui.screens.AiAnalystScreen
import com.example.ui.screens.AssetsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FindingsScreen
import com.example.ui.screens.HeadersScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SslAnalyzerScreen
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberShieldTheme
import com.example.ui.theme.CyberSurfaceCard
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

class MainActivity : ComponentActivity() {

    private val viewModel: CyberShieldViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CyberShieldTheme {
                val currentTab by viewModel.currentTab.collectAsState()

                // BackHandler to return to Dashboard if on any sub-screen
                BackHandler(enabled = currentTab != ScreenTab.DASHBOARD) {
                    viewModel.currentTab.value = ScreenTab.DASHBOARD
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CyberBg),
                    containerColor = CyberBg,
                    topBar = {
                        CyberTopBar(
                            currentTab = currentTab,
                            onTabSelected = { viewModel.currentTab.value = it }
                        )
                    },
                    bottomBar = {
                        CyberBottomBar(
                            currentTab = currentTab,
                            onTabSelected = { viewModel.currentTab.value = it }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            ScreenTab.DASHBOARD -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.currentTab.value = it }
                            )
                            ScreenTab.SCANNER -> ScannerScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.currentTab.value = it }
                            )
                            ScreenTab.SSL_ANALYZER -> SslAnalyzerScreen(viewModel = viewModel)
                            ScreenTab.HEADERS -> HeadersScreen(viewModel = viewModel)
                            ScreenTab.FINDINGS -> FindingsScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.currentTab.value = it }
                            )
                            ScreenTab.AI_ANALYST -> AiAnalystScreen(viewModel = viewModel)
                            ScreenTab.ASSETS -> AssetsScreen(viewModel = viewModel)
                            ScreenTab.REPORTS -> ReportsScreen(viewModel = viewModel)
                            ScreenTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CyberTopBar(
    currentTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit
) {
    Column(modifier = Modifier.background(CyberBg)) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.cybershield_icon_1794475345408),
                        contentDescription = "CyberShield Logo",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "CYBERSHIELD",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyberCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(CyberCyan.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("AI", color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            actions = {
                // Organization Indicator Chip matching screenshot
                Box(
                    modifier = Modifier
                        .background(CyberSurfaceElevated, RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Nexova Pro",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberTextSecondary,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { onTabSelected(ScreenTab.SCANNER) },
                    modifier = Modifier.testTag("topbar_scan_action")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "New Scan",
                        tint = CyberCyan
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = CyberBg,
                titleContentColor = CyberTextPrimary,
                actionIconContentColor = CyberCyan
            )
        )

        // Scrollable Tab Row for quick access across all 9 modules
        ScrollableTabRow(
            selectedTabIndex = ScreenTab.values().indexOf(currentTab),
            containerColor = CyberBg,
            contentColor = CyberCyan,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                val index = ScreenTab.values().indexOf(currentTab)
                if (index in tabPositions.indices) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[index]),
                        color = CyberCyan,
                        height = 2.dp
                    )
                }
            },
            divider = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(CyberBorder)
                )
            }
        ) {
            ScreenTab.values().forEach { tab ->
                val selected = currentTab == tab
                Tab(
                    selected = selected,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Text(
                            text = tab.title,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) CyberCyan else CyberTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun CyberBottomBar(
    currentTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit
) {
    NavigationBar(
        containerColor = CyberSurfaceCard,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentTab == ScreenTab.DASHBOARD,
            onClick = { onTabSelected(ScreenTab.DASHBOARD) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
            label = { Text("Overview", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyberCyan,
                selectedTextColor = CyberCyan,
                unselectedIconColor = CyberTextMuted,
                unselectedTextColor = CyberTextMuted,
                indicatorColor = CyberSurfaceElevated
            )
        )
        NavigationBarItem(
            selected = currentTab == ScreenTab.SCANNER,
            onClick = { onTabSelected(ScreenTab.SCANNER) },
            icon = { Icon(Icons.Default.Security, contentDescription = "Scanner") },
            label = { Text("Scanner", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyberCyan,
                selectedTextColor = CyberCyan,
                unselectedIconColor = CyberTextMuted,
                unselectedTextColor = CyberTextMuted,
                indicatorColor = CyberSurfaceElevated
            )
        )
        NavigationBarItem(
            selected = currentTab == ScreenTab.FINDINGS,
            onClick = { onTabSelected(ScreenTab.FINDINGS) },
            icon = { Icon(Icons.Default.Visibility, contentDescription = "Discoveries") },
            label = { Text("Findings", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyberCyan,
                selectedTextColor = CyberCyan,
                unselectedIconColor = CyberTextMuted,
                unselectedTextColor = CyberTextMuted,
                indicatorColor = CyberSurfaceElevated
            )
        )
        NavigationBarItem(
            selected = currentTab == ScreenTab.AI_ANALYST,
            onClick = { onTabSelected(ScreenTab.AI_ANALYST) },
            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Analyst") },
            label = { Text("AI Fix", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyberCyan,
                selectedTextColor = CyberCyan,
                unselectedIconColor = CyberTextMuted,
                unselectedTextColor = CyberTextMuted,
                indicatorColor = CyberSurfaceElevated
            )
        )
        NavigationBarItem(
            selected = currentTab == ScreenTab.SETTINGS || currentTab == ScreenTab.REPORTS || currentTab == ScreenTab.ASSETS,
            onClick = { onTabSelected(ScreenTab.REPORTS) },
            icon = { Icon(Icons.Default.Description, contentDescription = "Reports") },
            label = { Text("Reports", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyberCyan,
                selectedTextColor = CyberCyan,
                unselectedIconColor = CyberTextMuted,
                unselectedTextColor = CyberTextMuted,
                indicatorColor = CyberSurfaceElevated
            )
        )
    }
}
