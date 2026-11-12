package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AuditLog
import com.example.data.model.Finding
import com.example.data.model.MonitoredAsset
import com.example.data.model.ScanProfile
import com.example.data.model.SecurityScan
import com.example.data.model.Severity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [SecurityScan::class, Finding::class, MonitoredAsset::class, AuditLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
    abstract fun assetDao(): AssetDao
    abstract fun auditDao(): AuditDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cybershield_db"
                ).addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            val now = System.currentTimeMillis()
            val dayMs = 86400000L

            // Initial monitored assets
            val asset1 = MonitoredAsset(
                domain = "nexova.com",
                displayName = "Nexova Production",
                verified = true,
                verificationMethod = "AUTHORIZED_ADMIN",
                verificationToken = "CS-VERIFY-9981-NXVA",
                lastScanDate = now - (1 * dayMs),
                lastScore = 73,
                monitoringEnabled = true,
                certExpiryDays = 42
            )
            val asset2 = MonitoredAsset(
                domain = "api.nexova.com",
                displayName = "Nexova API Gateway",
                verified = true,
                verificationMethod = "DNS_TXT",
                verificationToken = "CS-VERIFY-1102-APIG",
                lastScanDate = now - (3 * dayMs),
                lastScore = 88,
                monitoringEnabled = true,
                certExpiryDays = 118
            )
            val asset3 = MonitoredAsset(
                domain = "auth.nexova.com",
                displayName = "Nexova Identity SSO",
                verified = true,
                verificationMethod = "META_TAG",
                verificationToken = "CS-VERIFY-4522-SSO",
                lastScanDate = now - (7 * dayMs),
                lastScore = 91,
                monitoringEnabled = true,
                certExpiryDays = 64
            )
            database.assetDao().insertAsset(asset1)
            database.assetDao().insertAsset(asset2)
            database.assetDao().insertAsset(asset3)

            // Initial completed scan
            val scanId = "scan-nexova-primary"
            val scan = SecurityScan(
                id = scanId,
                targetUrl = "https://nexova.com",
                domain = "nexova.com",
                timestamp = now - (1 * dayMs),
                overallScore = 73,
                grade = "B",
                status = "COMPLETED",
                durationMs = 2450,
                authorizationConfirmed = true,
                authorizationProof = "Explicit Owner Authorization Sign-off (sec@nexova.com)",
                scanProfile = ScanProfile.FULL_AUDIT,
                criticalCount = 0,
                highCount = 10,
                mediumCount = 31,
                lowCount = 17,
                sslScore = 85,
                headersScore = 60,
                cookieScore = 70,
                transportScore = 90,
                exposureScore = 60,
                executiveSummary = "Security posture is GOOD (73/100). SSL/TLS is modern with valid certificates (42 days left). However, several key defensive HTTP security headers (CSP, Permissions-Policy, COOP) are absent or loosely configured, posing moderate clickjacking and content-injection risks."
            )
            database.scanDao().insertScan(scan)

            // Findings for nexova.com
            val findings = listOf(
                Finding(
                    id = UUID.randomUUID().toString(),
                    scanId = scanId,
                    category = "Headers",
                    title = "Missing Content-Security-Policy (CSP)",
                    severity = Severity.HIGH,
                    confidence = "Confirmed",
                    affectedUrl = "https://nexova.com",
                    evidence = "Response header 'Content-Security-Policy' is not returned by the server",
                    explanation = "Content Security Policy (CSP) restricts the sources from which scripts, styles, and other assets can be executed by browsers, preventing Cross-Site Scripting (XSS).",
                    impact = "Malicious inline scripts or unauthorized third-party scripts could execute within user browsers.",
                    remediation = "Define a restrictive CSP such as: default-src 'self'; script-src 'self' 'nonce-...'; style-src 'self' 'unsafe-inline'; object-src 'none';",
                    cwe = "CWE-1021 / OWASP A05:2021"
                ),
                Finding(
                    id = UUID.randomUUID().toString(),
                    scanId = scanId,
                    category = "Headers",
                    title = "Missing Permissions-Policy",
                    severity = Severity.HIGH,
                    confidence = "Confirmed",
                    affectedUrl = "https://nexova.com",
                    evidence = "Response header 'Permissions-Policy' is absent",
                    explanation = "Permissions-Policy controls browser hardware APIs like camera, microphone, geolocation, and payment delegation.",
                    impact = "Embedded iframes or malicious third-party scripts could attempt access to sensitive device APIs.",
                    remediation = "Add: Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=()",
                    cwe = "OWASP A05:2021"
                ),
                Finding(
                    id = UUID.randomUUID().toString(),
                    scanId = scanId,
                    category = "Headers",
                    title = "Missing Cross-Origin-Opener-Policy (COOP)",
                    severity = Severity.HIGH,
                    confidence = "Confirmed",
                    affectedUrl = "https://nexova.com",
                    evidence = "Header 'Cross-Origin-Opener-Policy' is not present",
                    explanation = "COOP isolates top-level browsing contexts from cross-origin documents, mitigating Spectre-style timing side-channel attacks.",
                    impact = "Cross-origin windows opened by or opening this site may retain references in window.opener.",
                    remediation = "Add: Cross-Origin-Opener-Policy: same-origin",
                    cwe = "OWASP A04:2021"
                ),
                Finding(
                    id = UUID.randomUUID().toString(),
                    scanId = scanId,
                    category = "Headers",
                    title = "Strict-Transport-Security (HSTS) Missing Preload",
                    severity = Severity.HIGH,
                    confidence = "Confirmed",
                    affectedUrl = "https://nexova.com",
                    evidence = "Strict-Transport-Security: max-age=31536000; includeSubDomains (missing 'preload' token)",
                    explanation = "Without 'preload', the very first connection attempt from a clean browser remains vulnerable to SSL-stripping MITM attacks.",
                    impact = "Attackers on unencrypted public Wi-Fi can downgrade the initial plain HTTP navigation.",
                    remediation = "Add 'preload' to HSTS header and submit domain to hstspreload.org.",
                    cwe = "CWE-319"
                ),
                Finding(
                    id = UUID.randomUUID().toString(),
                    scanId = scanId,
                    category = "Cookies",
                    title = "Cookie Missing SameSite Attribute",
                    severity = Severity.HIGH,
                    confidence = "Confirmed",
                    affectedUrl = "https://nexova.com",
                    evidence = "Set-Cookie: session_hint=user_active; Path=/; Secure; HttpOnly",
                    explanation = "Session cookies lacking an explicit SameSite attribute (Lax/Strict) may be sent with cross-site requests.",
                    impact = "Increases vulnerability to Cross-Site Request Forgery (CSRF).",
                    remediation = "Append '; SameSite=Lax' or '; SameSite=Strict' to all Set-Cookie directives.",
                    cwe = "CWE-1275"
                ),
                Finding(
                    id = UUID.randomUUID().toString(),
                    scanId = scanId,
                    category = "Disclosure",
                    title = "Server Software Version Disclosed in Header",
                    severity = Severity.MEDIUM,
                    confidence = "Confirmed",
                    affectedUrl = "https://nexova.com",
                    evidence = "Server: nginx/1.22.1 (Ubuntu)",
                    explanation = "Revealing exact server vendor and OS version helps malicious actors fingerprint specific known CVEs.",
                    impact = "Reconnaissance simplification for automated scanners and targeted exploit kits.",
                    remediation = "In nginx.conf, set 'server_tokens off;' to emit only generic 'Server: nginx' or strip entirely.",
                    cwe = "CWE-200"
                ),
                Finding(
                    id = UUID.randomUUID().toString(),
                    scanId = scanId,
                    category = "SSL/TLS",
                    title = "SSL Certificate Validity Below 60 Days",
                    severity = Severity.MEDIUM,
                    confidence = "Confirmed",
                    affectedUrl = "https://nexova.com",
                    evidence = "Certificate issued by Let's Encrypt expires in 42 days",
                    explanation = "Automated ACME certificate renewal should trigger well before expiration to prevent sudden outages.",
                    impact = "Service downtime and browser security warnings if renewal daemon fails.",
                    remediation = "Verify certbot / ACME cron service status and automate monitoring notifications.",
                    cwe = "CWE-295"
                ),
                Finding(
                    id = UUID.randomUUID().toString(),
                    scanId = scanId,
                    category = "Headers",
                    title = "X-Content-Type-Options Verified",
                    severity = Severity.SAFE,
                    confidence = "Confirmed",
                    affectedUrl = "https://nexova.com",
                    evidence = "X-Content-Type-Options: nosniff",
                    explanation = "Server explicitly instructs browsers not to MIME-sniff response bodies away from the declared Content-Type.",
                    impact = "No impact. Safe defensive configuration.",
                    remediation = "Maintain current nosniff setting.",
                    cwe = "N/A"
                )
            )
            database.scanDao().insertFindings(findings)

            // Audit logs matching screenshot
            val logs = listOf(
                AuditLog(UUID.randomUUID().toString(), now - 30000L, "SWITCH_ORGANIZATION", "nexova.com", "sec@nexova.com", "SUCCESS", "sec@nexova.com switched organization to Nexova Pro"),
                AuditLog(UUID.randomUUID().toString(), now - 75000L, "SWITCH_ORGANIZATION", "nexova.com", "sec@nexova.com", "SUCCESS", "sec@nexova.com switched organization"),
                AuditLog(UUID.randomUUID().toString(), now - 300000L, "SWITCH_ORGANIZATION", "nexova.com", "sec@nexova.com", "SUCCESS", "sec@nexova.com switched organization"),
                AuditLog(UUID.randomUUID().toString(), now - (1 * dayMs), "COMPLETED_SCAN", "nexova.com", "sec@nexova.com", "SUCCESS", "Full defensive security audit completed with score 73/100")
            )
            logs.forEach { database.auditDao().insertLog(it) }
        }
    }
}
