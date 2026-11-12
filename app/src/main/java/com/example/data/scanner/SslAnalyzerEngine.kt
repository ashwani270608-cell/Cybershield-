package com.example.data.scanner

import com.example.data.model.Finding
import com.example.data.model.Severity
import com.example.data.model.SslInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

object SslAnalyzerEngine {

    data class SslScanResult(
        val sslInfo: SslInfo?,
        val findings: List<Finding>,
        val scoreDeductions: Int
    )

    suspend fun analyze(domain: String, scanId: String): SslScanResult = withContext(Dispatchers.IO) {
        val findings = mutableListOf<Finding>()
        var deductions = 0
        var sslInfo: SslInfo? = null

        try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, null, null)
            val factory: SSLSocketFactory = sslContext.socketFactory

            val socket = factory.createSocket(domain, 443) as SSLSocket
            socket.soTimeout = 8000
            socket.startHandshake()

            val session = socket.session
            val peerCertificates = session.peerCertificates
            val primaryCert = peerCertificates.firstOrNull() as? X509Certificate

            val protocol = session.protocol ?: "Unknown"
            val cipherSuite = session.cipherSuite ?: "Unknown"

            socket.close()

            if (primaryCert != null) {
                val now = System.currentTimeMillis()
                val notAfter = primaryCert.notAfter.time
                val notBefore = primaryCert.notBefore.time
                val daysRemaining = ((notAfter - now) / (1000 * 60 * 60 * 24)).coerceAtLeast(0)

                val subject = primaryCert.subjectX500Principal.name
                val issuer = primaryCert.issuerX500Principal.name

                val subjectCn = extractCn(subject)
                val issuerCn = extractCn(issuer)
                val issuerOrg = extractOrg(issuer)

                val sigAlg = primaryCert.sigAlgName ?: "Unknown"
                val chainCount = peerCertificates.size

                val isSelfSigned = (subject == issuer)
                val isExpired = now > notAfter
                val isNotYetValid = now < notBefore
                val isExpiringSoon = daysRemaining in 1..29

                // Extract SAN (Subject Alternative Names)
                val sanList = mutableListOf<String>()
                try {
                    val altNames = primaryCert.subjectAlternativeNames
                    altNames?.forEach { item ->
                        val value = item.getOrNull(1)?.toString()
                        if (value != null) sanList.add(value)
                    }
                } catch (_: Exception) {}

                // Hostname match check
                val hostnameMatches = checkHostnameMatch(domain, subjectCn, sanList)

                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                val validFromStr = dateFormat.format(Date(notBefore))
                val validToStr = dateFormat.format(Date(notAfter))

                sslInfo = SslInfo(
                    domain = domain,
                    issuerCn = issuerCn,
                    issuerOrg = issuerOrg,
                    subjectCn = subjectCn,
                    validFrom = validFromStr,
                    validTo = validToStr,
                    daysRemaining = daysRemaining,
                    isValid = !isExpired && !isNotYetValid && hostnameMatches && !isSelfSigned,
                    isExpiringSoon = isExpiringSoon,
                    isSelfSigned = isSelfSigned,
                    protocol = protocol,
                    cipherSuite = cipherSuite,
                    sanList = sanList.take(8),
                    signatureAlgorithm = sigAlg,
                    chainLength = chainCount
                )

                // Audit findings
                if (isExpired) {
                    deductions += 25
                    findings.add(
                        Finding(
                            id = UUID.randomUUID().toString(),
                            scanId = scanId,
                            category = "SSL/TLS",
                            title = "SSL Certificate Has Expired",
                            severity = Severity.CRITICAL,
                            confidence = "Confirmed",
                            affectedUrl = "https://$domain",
                            evidence = "Certificate expired on $validToStr",
                            explanation = "Expired certificates trigger major browser warnings and break HTTPS encryption trust.",
                            impact = "Users receive security warnings and cannot safely connect to the site.",
                            remediation = "Renew and install an updated SSL certificate immediately.",
                            cwe = "CWE-298"
                        )
                    )
                } else if (isExpiringSoon) {
                    deductions += 8
                    findings.add(
                        Finding(
                            id = UUID.randomUUID().toString(),
                            scanId = scanId,
                            category = "SSL/TLS",
                            title = "SSL Certificate Expiring Soon ($daysRemaining days)",
                            severity = Severity.HIGH,
                            confidence = "Confirmed",
                            affectedUrl = "https://$domain",
                            evidence = "Valid until $validToStr ($daysRemaining days remaining)",
                            explanation = "Certificates approaching expiration must be renewed before expiration to avoid sudden downtime.",
                            impact = "Risk of service outage if automatic renewal fails.",
                            remediation = "Verify ACME / certbot renewal hooks and re-issue the certificate.",
                            cwe = "CWE-298"
                        )
                    )
                }

                if (isSelfSigned) {
                    deductions += 20
                    findings.add(
                        Finding(
                            id = UUID.randomUUID().toString(),
                            scanId = scanId,
                            category = "SSL/TLS",
                            title = "Self-Signed Certificate Detected",
                            severity = Severity.CRITICAL,
                            confidence = "Confirmed",
                            affectedUrl = "https://$domain",
                            evidence = "Issuer matches Subject: $subjectCn",
                            explanation = "Self-signed certificates are not signed by a trusted Certificate Authority (CA) recognized by public browsers.",
                            impact = "Users will be blocked by major browsers with 'Untrusted Authority' interstitial pages.",
                            remediation = "Replace with a free public certificate from Let's Encrypt or a trusted commercial CA.",
                            cwe = "CWE-295"
                        )
                    )
                }

                if (!hostnameMatches) {
                    deductions += 20
                    findings.add(
                        Finding(
                            id = UUID.randomUUID().toString(),
                            scanId = scanId,
                            category = "SSL/TLS",
                            title = "SSL Hostname Mismatch",
                            severity = Severity.CRITICAL,
                            confidence = "Confirmed",
                            affectedUrl = "https://$domain",
                            evidence = "Domain '$domain' does not match CN '$subjectCn' or SANs: ${sanList.joinToString(", ")}",
                            explanation = "The certificate was issued for different domain names, failing RFC 6125 hostname verification.",
                            impact = "Browsers will abort connection with ERR_CERT_COMMON_NAME_INVALID.",
                            remediation = "Re-issue the certificate with '$domain' added to Subject Alternative Names (SAN).",
                            cwe = "CWE-297"
                        )
                    )
                }

                if (protocol.contains("1.0") || protocol.contains("1.1")) {
                    deductions += 15
                    findings.add(
                        Finding(
                            id = UUID.randomUUID().toString(),
                            scanId = scanId,
                            category = "SSL/TLS",
                            title = "Deprecated TLS Version ($protocol)",
                            severity = Severity.HIGH,
                            confidence = "Confirmed",
                            affectedUrl = "https://$domain",
                            evidence = "Negotiated legacy protocol: $protocol",
                            explanation = "TLS 1.0 and 1.1 are deprecated by RFC 8996 due to known vulnerabilities like POODLE and BEAST.",
                            impact = "Cryptographic downgrade attacks and non-compliance with PCI-DSS 3.1+ standards.",
                            remediation = "Disable TLS 1.0 and 1.1 in web server config; enforce TLSv1.2 and TLSv1.3 only.",
                            cwe = "CWE-326"
                        )
                    )
                }

                if (sigAlg.contains("SHA1") || sigAlg.contains("MD5")) {
                    deductions += 15
                    findings.add(
                        Finding(
                            id = UUID.randomUUID().toString(),
                            scanId = scanId,
                            category = "SSL/TLS",
                            title = "Weak Certificate Signature Algorithm ($sigAlg)",
                            severity = Severity.HIGH,
                            confidence = "Confirmed",
                            affectedUrl = "https://$domain",
                            evidence = "Signature algorithm: $sigAlg",
                            explanation = "SHA-1 and MD5 have demonstrated collision vulnerabilities and are distrusted by modern root stores.",
                            impact = "Vulnerability to certificate forgery and MITM interception.",
                            remediation = "Re-issue certificate signed with SHA-256 or SHA-384.",
                            cwe = "CWE-328"
                        )
                    )
                }

                if (findings.none { it.category == "SSL/TLS" }) {
                    findings.add(
                        Finding(
                            id = UUID.randomUUID().toString(),
                            scanId = scanId,
                            category = "SSL/TLS",
                            title = "SSL/TLS Certificate Valid & Modern",
                            severity = Severity.SAFE,
                            confidence = "Confirmed",
                            affectedUrl = "https://$domain",
                            evidence = "Issued by $issuerOrg, valid until $validToStr ($daysRemaining days remaining, $protocol, $cipherSuite)",
                            explanation = "Certificate is trusted, signed with $sigAlg, and negotiates modern $protocol cipher suites.",
                            impact = "Secure end-to-end encryption active.",
                            remediation = "Maintain automated renewal schedules.",
                            cwe = "N/A"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            deductions += 25
            findings.add(
                Finding(
                    id = UUID.randomUUID().toString(),
                    scanId = scanId,
                    category = "SSL/TLS",
                    title = "HTTPS/TLS Handshake Failure",
                    severity = Severity.CRITICAL,
                    confidence = "Confirmed",
                    affectedUrl = "https://$domain",
                    evidence = "Connection error: ${e.message ?: "Failed to complete SSL handshake on port 443"}",
                    explanation = "Unable to establish an encrypted TLS connection with the target domain.",
                    impact = "Website is inaccessible over HTTPS or blocking connections.",
                    remediation = "Ensure port 443 is open and web server TLS configuration is running properly.",
                    cwe = "CWE-319"
                )
            )
        }

        SslScanResult(sslInfo, findings, deductions)
    }

    private fun extractCn(dn: String): String {
        val parts = dn.split(",")
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.startsWith("CN=", ignoreCase = true)) {
                return trimmed.substring(3).trim()
            }
        }
        return dn
    }

    private fun extractOrg(dn: String): String {
        val parts = dn.split(",")
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.startsWith("O=", ignoreCase = true)) {
                return trimmed.substring(2).trim()
            }
        }
        return extractCn(dn)
    }

    private fun checkHostnameMatch(domain: String, cn: String, sans: List<String>): Boolean {
        val lowerDomain = domain.lowercase()
        val allNames = (sans + cn).map { it.lowercase() }
        for (name in allNames) {
            if (name == lowerDomain) return true
            if (name.startsWith("*.")) {
                val wildSuffix = name.substring(2)
                if (lowerDomain.endsWith(wildSuffix) && !lowerDomain.substring(0, lowerDomain.length - wildSuffix.length).contains(".")) {
                    return true
                }
            }
        }
        return false
    }
}
