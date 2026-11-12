package com.example.data.scanner

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.AuditLog
import com.example.data.model.Finding
import com.example.data.model.ScanProfile
import com.example.data.model.SecurityScan
import com.example.data.model.Severity
import com.example.data.model.SslInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.TimeUnit

class ScanOrchestrator(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    sealed class ScanProgress {
        data class Progress(val percent: Float, val stage: String) : ScanProgress()
        data class Completed(val scan: SecurityScan, val findings: List<Finding>, val sslInfo: SslInfo?) : ScanProgress()
        data class Error(val message: String) : ScanProgress()
    }

    suspend fun runScan(
        rawUrl: String,
        profile: ScanProfile,
        authorized: Boolean,
        onProgress: (ScanProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        // 1. Authorization Safety Gate
        if (!authorized) {
            onProgress(ScanProgress.Error("Scan Aborted: Target domain scanning requires explicit documented authorization confirming ownership or assessment permission."))
            return@withContext
        }

        onProgress(ScanProgress.Progress(0.1f, "Validating URL & checking SSRF safety guards..."))
        delay(200)

        // 2. SSRF Guard
        val validation = SsrfGuard.validateUrl(rawUrl)
        if (validation is SsrfGuard.ValidationResult.Blocked) {
            onProgress(ScanProgress.Error(validation.reason))
            return@withContext
        }

        val allowed = validation as SsrfGuard.ValidationResult.Allowed
        val targetUrl = allowed.normalizedUrl
        val targetDomain = allowed.host
        val scanId = "scan-${UUID.randomUUID().toString().take(8)}"

        // Log audit
        db.auditDao().insertLog(
            AuditLog(
                id = UUID.randomUUID().toString(),
                timestamp = startTime,
                action = "INITIATE_SCAN",
                targetDomain = targetDomain,
                actor = "authorized-security-auditor",
                status = "STARTED",
                details = "Started ${profile.title} on $targetDomain with authorization confirmation"
            )
        )

        val allFindings = mutableListOf<Finding>()
        var sslInfo: SslInfo? = null
        var sslDeductions = 0
        var headersDeductions = 0
        var transportDeductions = 0
        var cookieDeductions = 0
        var exposureDeductions = 0

        // 3. SSL/TLS Handshake
        if (profile == ScanProfile.FULL_AUDIT || profile == ScanProfile.SSL_ONLY) {
            onProgress(ScanProgress.Progress(0.3f, "Inspecting SSL/TLS certificate chain & ciphers..."))
            val sslResult = SslAnalyzerEngine.analyze(targetDomain, scanId)
            sslInfo = sslResult.sslInfo
            sslDeductions = sslResult.scoreDeductions
            allFindings.addAll(sslResult.findings)
            delay(300)
        }

        // 4. HTTP Headers Inspection
        if (profile == ScanProfile.FULL_AUDIT || profile == ScanProfile.HEADERS_ONLY) {
            onProgress(ScanProgress.Progress(0.55f, "Evaluating HTTP Security Headers (CSP, HSTS, XFO)..."))
            try {
                val req = Request.Builder()
                    .url("https://$targetDomain")
                    .header("User-Agent", "CyberShield-Security-Auditor/2.4")
                    .build()
                httpClient.newCall(req).execute().use { response ->
                    val headersResult = HeadersAnalyzerEngine.analyze(response.headers, "https://$targetDomain", scanId)
                    headersDeductions = headersResult.scoreDeductions
                    allFindings.addAll(headersResult.findings)
                }
            } catch (e: Exception) {
                headersDeductions += 15
            }
            delay(300)
        }

        // 5. Safe Vulnerability & Transport Checks
        if (profile == ScanProfile.FULL_AUDIT || profile == ScanProfile.PASSIVE_SAFE) {
            onProgress(ScanProgress.Progress(0.8f, "Running passive transport, cookie & disclosure audits..."))
            val safeResult = SafeVulnerabilityEngine.analyze(targetDomain, scanId)
            transportDeductions = safeResult.transportScoreDeductions
            cookieDeductions = safeResult.cookieScoreDeductions
            exposureDeductions = safeResult.exposureScoreDeductions
            allFindings.addAll(safeResult.findings)
            delay(300)
        }

        onProgress(ScanProgress.Progress(0.95f, "Calculating defensive score & generating report..."))

        // Calculate Scores
        val sslScore = (25 - sslDeductions).coerceIn(0, 25)
        val headersScore = (35 - headersDeductions).coerceIn(0, 35)
        val cookieScore = (15 - cookieDeductions).coerceIn(0, 15)
        val transportScore = (15 - transportDeductions).coerceIn(0, 15)
        val exposureScore = (10 - exposureDeductions).coerceIn(0, 10)

        val overallScore = sslScore + headersScore + cookieScore + transportScore + exposureScore
        val grade = when {
            overallScore >= 95 -> "A+"
            overallScore >= 85 -> "A"
            overallScore >= 70 -> "B"
            overallScore >= 55 -> "C"
            overallScore >= 40 -> "D"
            else -> "F"
        }

        val criticalCount = allFindings.count { it.severity == Severity.CRITICAL }
        val highCount = allFindings.count { it.severity == Severity.HIGH }
        val mediumCount = allFindings.count { it.severity == Severity.MEDIUM }
        val lowCount = allFindings.count { it.severity == Severity.LOW }

        val summary = buildString {
            append("Defensive security score for $targetDomain is $overallScore/100 (Grade: $grade). ")
            if (criticalCount > 0) {
                append("$criticalCount critical issue(s) detected requiring immediate attention. ")
            }
            if (highCount > 0) {
                append("$highCount high severity finding(s) identified. ")
            }
            if (criticalCount == 0 && highCount == 0) {
                append("No critical or high severity vulnerabilities found. Safe baseline posture. ")
            }
            append("SSL Score: $sslScore/25, Headers: $headersScore/35, Cookies: $cookieScore/15, Transport: $transportScore/15, Exposure: $exposureScore/10.")
        }

        val scan = SecurityScan(
            id = scanId,
            targetUrl = targetUrl,
            domain = targetDomain,
            timestamp = System.currentTimeMillis(),
            overallScore = overallScore,
            grade = grade,
            status = "COMPLETED",
            durationMs = System.currentTimeMillis() - startTime,
            authorizationConfirmed = true,
            authorizationProof = "Explicit Owner Authorization Sign-off confirmed in UI",
            scanProfile = profile,
            criticalCount = criticalCount,
            highCount = highCount,
            mediumCount = mediumCount,
            lowCount = lowCount,
            sslScore = sslScore,
            headersScore = headersScore,
            cookieScore = cookieScore,
            transportScore = transportScore,
            exposureScore = exposureScore,
            executiveSummary = summary
        )

        // Save to Database
        db.scanDao().insertScan(scan)
        db.scanDao().insertFindings(allFindings)

        // Update monitored asset if exists or create
        val existingAsset = db.assetDao().getAsset(targetDomain)
        if (existingAsset != null) {
            db.assetDao().updateAsset(
                existingAsset.copy(
                    lastScanDate = scan.timestamp,
                    lastScore = scan.overallScore,
                    certExpiryDays = sslInfo?.daysRemaining ?: existingAsset.certExpiryDays
                )
            )
        }

        db.auditDao().insertLog(
            AuditLog(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                action = "COMPLETED_SCAN",
                targetDomain = targetDomain,
                actor = "authorized-security-auditor",
                status = "SUCCESS",
                details = "Completed scan $scanId: Score $overallScore/100, $criticalCount critical, $highCount high findings"
            )
        )

        onProgress(ScanProgress.Completed(scan, allFindings, sslInfo))
    }
}
