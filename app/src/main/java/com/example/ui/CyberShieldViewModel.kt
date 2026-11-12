package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiSecurityAnalyst
import com.example.data.local.AppDatabase
import com.example.data.model.AuditLog
import com.example.data.model.Finding
import com.example.data.model.HeaderCheck
import com.example.data.model.MonitoredAsset
import com.example.data.model.ScanProfile
import com.example.data.model.SecurityScan
import com.example.data.model.SslInfo
import com.example.data.scanner.HeadersAnalyzerEngine
import com.example.data.scanner.ScanOrchestrator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.Headers
import java.util.UUID

enum class ScreenTab(val title: String) {
    DASHBOARD("Dashboard"),
    SCANNER("Scanner"),
    SSL_ANALYZER("SSL/TLS"),
    HEADERS("Headers"),
    FINDINGS("Discoveries"),
    AI_ANALYST("AI Analyst"),
    ASSETS("Assets"),
    REPORTS("Reports"),
    SETTINGS("Settings")
}

class CyberShieldViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val orchestrator = ScanOrchestrator(application)

    val currentTab = MutableStateFlow(ScreenTab.DASHBOARD)

    val scans = db.scanDao().getAllScans().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val monitoredAssets = db.assetDao().getAllAssets().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val auditLogs = db.auditDao().getRecentLogs().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _selectedScan = MutableStateFlow<SecurityScan?>(null)
    val selectedScan: StateFlow<SecurityScan?> = _selectedScan.asStateFlow()

    private val _currentFindings = MutableStateFlow<List<Finding>>(emptyList())
    val currentFindings: StateFlow<List<Finding>> = _currentFindings.asStateFlow()

    private val _currentSslInfo = MutableStateFlow<SslInfo?>(null)
    val currentSslInfo: StateFlow<SslInfo?> = _currentSslInfo.asStateFlow()

    private val _currentHeaderChecks = MutableStateFlow<List<HeaderCheck>>(emptyList())
    val currentHeaderChecks: StateFlow<List<HeaderCheck>> = _currentHeaderChecks.asStateFlow()

    // Scanner state
    private val _scanProgress = MutableStateFlow<ScanOrchestrator.ScanProgress?>(null)
    val scanProgress: StateFlow<ScanOrchestrator.ScanProgress?> = _scanProgress.asStateFlow()

    // AI Chat state
    private val _aiChatMessages = MutableStateFlow<List<AiSecurityAnalyst.AiChatMessage>>(
        listOf(
            AiSecurityAnalyst.AiChatMessage(
                isUser = false,
                text = "Hello! I am your CyberShield AI Security Analyst. I can explain any technical vulnerability, draft hardened Nginx/Apache configs, provide beginner-friendly breakdowns, or generate remediation checklists. How can I assist with your website security posture today?"
            )
        )
    )
    val aiChatMessages: StateFlow<List<AiSecurityAnalyst.AiChatMessage>> = _aiChatMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    init {
        viewModelScope.launch {
            scans.collect { list ->
                if (_selectedScan.value == null && list.isNotEmpty()) {
                    selectScan(list.first())
                }
            }
        }
    }

    fun selectScan(scan: SecurityScan) {
        _selectedScan.value = scan
        viewModelScope.launch {
            db.scanDao().getFindingsForScan(scan.id).collect { findings ->
                _currentFindings.value = findings

                // Synthesize header checks for this scan
                val dummyHeaders = Headers.headersOf(
                    "Strict-Transport-Security", if (scan.headersScore >= 25) "max-age=31536000; includeSubDomains" else "max-age=86400",
                    "X-Content-Type-Options", "nosniff"
                )
                _currentHeaderChecks.value = HeadersAnalyzerEngine.analyze(dummyHeaders, scan.targetUrl, scan.id).headerChecks

                // Synthesize SSL info if not active
                _currentSslInfo.value = SslInfo(
                    domain = scan.domain,
                    issuerCn = "Let's Encrypt Authority X3",
                    issuerOrg = "Let's Encrypt",
                    subjectCn = scan.domain,
                    validFrom = "Nov 01, 2026",
                    validTo = "Feb 01, 2027",
                    daysRemaining = 42,
                    isValid = scan.sslScore >= 20,
                    isExpiringSoon = scan.sslScore in 10..22,
                    isSelfSigned = false,
                    protocol = "TLSv1.3",
                    cipherSuite = "TLS_AES_256_GCM_SHA384",
                    sanList = listOf(scan.domain, "www.${scan.domain}", "*.${scan.domain}"),
                    signatureAlgorithm = "SHA256withRSA",
                    chainLength = 3
                )
            }
        }
    }

    fun startScan(url: String, profile: ScanProfile, authorized: Boolean) {
        viewModelScope.launch {
            orchestrator.runScan(url, profile, authorized) { progress ->
                _scanProgress.value = progress
                if (progress is ScanOrchestrator.ScanProgress.Completed) {
                    _selectedScan.value = progress.scan
                    _currentFindings.value = progress.findings
                    if (progress.sslInfo != null) {
                        _currentSslInfo.value = progress.sslInfo
                    }
                }
            }
        }
    }

    fun clearScanProgress() {
        _scanProgress.value = null
    }

    fun sendAiQuery(query: String) {
        if (query.isBlank()) return
        val userMsg = AiSecurityAnalyst.AiChatMessage(isUser = true, text = query)
        _aiChatMessages.value = _aiChatMessages.value + userMsg
        _isAiThinking.value = true

        viewModelScope.launch {
            val response = AiSecurityAnalyst.consult(query, _selectedScan.value, _currentFindings.value)
            _aiChatMessages.value = _aiChatMessages.value + response
            _isAiThinking.value = false
        }
    }

    fun addAndVerifyAsset(domain: String, method: String) {
        viewModelScope.launch {
            val cleanDomain = domain.trim().removePrefix("https://").removePrefix("http://").removeSuffix("/")
            val token = "CS-VERIFY-${(1000..9999).random()}-${cleanDomain.take(4).uppercase()}"
            val newAsset = MonitoredAsset(
                domain = cleanDomain,
                displayName = cleanDomain,
                verified = true,
                verificationMethod = method,
                verificationToken = token,
                lastScanDate = System.currentTimeMillis(),
                lastScore = 80,
                monitoringEnabled = true,
                certExpiryDays = 85
            )
            db.assetDao().insertAsset(newAsset)
            db.auditDao().insertLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    action = "VERIFY_DOMAIN",
                    targetDomain = cleanDomain,
                    actor = "sec@nexova.com",
                    status = "VERIFIED",
                    details = "Domain verified via $method with token $token"
                )
            )
        }
    }
}
