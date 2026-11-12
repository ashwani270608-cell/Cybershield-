package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Severity(val label: String, val weight: Int) {
    CRITICAL("Critical", 4),
    HIGH("High", 3),
    MEDIUM("Medium", 2),
    LOW("Low", 1),
    INFO("Info", 0),
    SAFE("Safe", -1)
}

enum class ScanProfile(val title: String, val description: String) {
    FULL_AUDIT("Full Defensive Audit", "SSL/TLS, Security Headers, Safe passive exposure, and Cookies"),
    SSL_ONLY("SSL/TLS Deep Analyzer", "Certificates, cryptographic ciphers, and expiration check"),
    HEADERS_ONLY("HTTP Security Headers", "CSP, HSTS, X-Frame-Options, MIME sniffing & cookies"),
    PASSIVE_SAFE("Passive Posture Check", "Non-intrusive redirection, robots.txt, and disclosure checks")
}

@Entity(tableName = "security_scans")
data class SecurityScan(
    @PrimaryKey val id: String,
    val targetUrl: String,
    val domain: String,
    val timestamp: Long,
    val overallScore: Int,
    val grade: String,
    val status: String,
    val durationMs: Long,
    val authorizationConfirmed: Boolean,
    val authorizationProof: String,
    val scanProfile: ScanProfile,
    val criticalCount: Int,
    val highCount: Int,
    val mediumCount: Int,
    val lowCount: Int,
    val sslScore: Int,
    val headersScore: Int,
    val cookieScore: Int,
    val transportScore: Int,
    val exposureScore: Int,
    val executiveSummary: String = ""
)

@Entity(tableName = "findings")
data class Finding(
    @PrimaryKey val id: String,
    val scanId: String,
    val category: String, // "SSL/TLS", "Headers", "Cookies", "Transport", "Disclosure"
    val title: String,
    val severity: Severity,
    val confidence: String, // "High", "Confirmed"
    val affectedUrl: String,
    val evidence: String,
    val explanation: String,
    val impact: String,
    val remediation: String,
    val cwe: String = ""
)

data class SslInfo(
    val domain: String,
    val issuerCn: String,
    val issuerOrg: String,
    val subjectCn: String,
    val validFrom: String,
    val validTo: String,
    val daysRemaining: Long,
    val isValid: Boolean,
    val isExpiringSoon: Boolean,
    val isSelfSigned: Boolean,
    val protocol: String,
    val cipherSuite: String,
    val sanList: List<String>,
    val signatureAlgorithm: String,
    val chainLength: Int
)

data class HeaderCheck(
    val headerName: String,
    val detectedValue: String?,
    val isPresent: Boolean,
    val importance: String, // "Crucial", "Recommended", "Optional"
    val status: String, // "PASS", "WARN", "FAIL"
    val assessment: String,
    val recommendedValue: String,
    val potentialRisk: String
)

@Entity(tableName = "monitored_assets")
data class MonitoredAsset(
    @PrimaryKey val domain: String,
    val displayName: String,
    val verified: Boolean,
    val verificationMethod: String, // "DNS_TXT", "META_TAG", "AUTHORIZED_ADMIN"
    val verificationToken: String,
    val lastScanDate: Long,
    val lastScore: Int,
    val monitoringEnabled: Boolean,
    val checkIntervalDays: Int = 1,
    val certExpiryDays: Long = 90
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val action: String,
    val targetDomain: String,
    val actor: String,
    val status: String,
    val details: String
)
