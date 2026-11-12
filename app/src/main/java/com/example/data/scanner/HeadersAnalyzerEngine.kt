package com.example.data.scanner

import com.example.data.model.Finding
import com.example.data.model.HeaderCheck
import com.example.data.model.Severity
import okhttp3.Headers
import java.util.UUID

object HeadersAnalyzerEngine {

    data class HeadersScanResult(
        val headerChecks: List<HeaderCheck>,
        val findings: List<Finding>,
        val scoreDeductions: Int
    )

    fun analyze(headers: Headers, targetUrl: String, scanId: String): HeadersScanResult {
        val checks = mutableListOf<HeaderCheck>()
        val findings = mutableListOf<Finding>()
        var deductions = 0

        // 1. Strict-Transport-Security (HSTS)
        val hsts = headers["Strict-Transport-Security"]
        if (hsts == null) {
            deductions += 10
            checks.add(
                HeaderCheck(
                    headerName = "Strict-Transport-Security",
                    detectedValue = null,
                    isPresent = false,
                    importance = "Crucial",
                    status = "FAIL",
                    assessment = "Missing HSTS header. Browsers may connect via insecure plain HTTP on initial request.",
                    recommendedValue = "max-age=31536000; includeSubDomains; preload",
                    potentialRisk = "Vulnerable to SSL-stripping Man-in-the-Middle (MITM) attacks on public networks."
                )
            )
            findings.add(
                Finding(
                    id = UUID.randomUUID().toString(),
                    scanId = scanId,
                    category = "Headers",
                    title = "Missing Strict-Transport-Security (HSTS)",
                    severity = Severity.HIGH,
                    confidence = "Confirmed",
                    affectedUrl = targetUrl,
                    evidence = "HTTP response lacks Strict-Transport-Security header",
                    explanation = "HSTS enforces that all communications from browsers occur over secure HTTPS, preventing protocol downgrade attacks.",
                    impact = "Users navigating to the site may be vulnerable to interception or redirection to malicious plain HTTP clones.",
                    remediation = "Configure your web server (nginx, Apache, or Cloudflare) to add: Strict-Transport-Security: max-age=31536000; includeSubDomains; preload",
                    cwe = "CWE-319 / CWE-523"
                )
            )
        } else {
            val maxAge = extractMaxAge(hsts)
            val hasSubdomains = hsts.contains("includeSubDomains", ignoreCase = true)
            val hasPreload = hsts.contains("preload", ignoreCase = true)

            if (maxAge < 10368000) { // < 120 days
                deductions += 4
                checks.add(
                    HeaderCheck(
                        headerName = "Strict-Transport-Security",
                        detectedValue = hsts,
                        isPresent = true,
                        importance = "Crucial",
                        status = "WARN",
                        assessment = "HSTS max-age is too short ($maxAge seconds). Recommended minimum is at least 1 year (31536000s).",
                        recommendedValue = "max-age=31536000; includeSubDomains; preload",
                        potentialRisk = "Short max-age leaves users unprotected after brief browsing intervals."
                    )
                )
                findings.add(
                    Finding(
                        id = UUID.randomUUID().toString(),
                        scanId = scanId,
                        category = "Headers",
                        title = "HSTS max-age Duration Too Short",
                        severity = Severity.MEDIUM,
                        confidence = "Confirmed",
                        affectedUrl = targetUrl,
                        evidence = "Strict-Transport-Security: $hsts",
                        explanation = "The HSTS max-age value indicates how long the browser should remember to enforce HTTPS.",
                        impact = "Expiration of HTTPS enforcement occurs sooner than industry best practice.",
                        remediation = "Increase max-age to 31536000 seconds (1 year) and includeSubDomains.",
                        cwe = "CWE-319"
                    )
                )
            } else {
                checks.add(
                    HeaderCheck(
                        headerName = "Strict-Transport-Security",
                        detectedValue = hsts,
                        isPresent = true,
                        importance = "Crucial",
                        status = if (hasPreload && hasSubdomains) "PASS" else "WARN",
                        assessment = if (hasPreload) "Excellent HSTS implementation with preload." else "Valid HSTS max-age, but missing preload directive.",
                        recommendedValue = "max-age=31536000; includeSubDomains; preload",
                        potentialRisk = if (hasPreload) "None" else "Initial unvisited request can be downgraded before HSTS cache is populated."
                    )
                )
            }
        }

        // 2. Content-Security-Policy (CSP)
        val csp = headers["Content-Security-Policy"]
        if (csp == null) {
            deductions += 10
            checks.add(
                HeaderCheck(
                    headerName = "Content-Security-Policy",
                    detectedValue = null,
                    isPresent = false,
                    importance = "Crucial",
                    status = "FAIL",
                    assessment = "Missing Content-Security-Policy header. Browser will load scripts and frames from any origin.",
                    recommendedValue = "default-src 'self'; script-src 'self' 'nonce-...'; object-src 'none';",
                    potentialRisk = "Vulnerable to Cross-Site Scripting (XSS), clickjacking, and unauthorized third-party script injection."
                )
            )
            findings.add(
                Finding(
                    id = UUID.randomUUID().toString(),
                    scanId = scanId,
                    category = "Headers",
                    title = "Missing Content-Security-Policy (CSP)",
                    severity = Severity.HIGH,
                    confidence = "Confirmed",
                    affectedUrl = targetUrl,
                    evidence = "Content-Security-Policy header is absent from HTTP response",
                    explanation = "CSP is the primary defense-in-depth mechanism against Cross-Site Scripting (XSS) and data injection attacks.",
                    impact = "If an injection flaw exists, attackers can execute arbitrary JavaScript in victim browsers.",
                    remediation = "Deploy a Content-Security-Policy header restricting script-src, object-src, and frame-ancestors.",
                    cwe = "CWE-1021 / CWE-79"
                )
            )
        } else {
            val hasUnsafeInline = csp.contains("'unsafe-inline'")
            val hasUnsafeEval = csp.contains("'unsafe-eval'")
            if (hasUnsafeInline || hasUnsafeEval) {
                deductions += 4
                checks.add(
                    HeaderCheck(
                        headerName = "Content-Security-Policy",
                        detectedValue = csp.take(120) + if (csp.length > 120) "..." else "",
                        isPresent = true,
                        importance = "Crucial",
                        status = "WARN",
                        assessment = "CSP is present but contains unsafe keywords ('unsafe-inline' or 'unsafe-eval').",
                        recommendedValue = "default-src 'self'; script-src 'self' 'strict-dynamic'; object-src 'none';",
                        potentialRisk = "Enables inline script execution which reduces protection against reflected and stored XSS."
                    )
                )
            } else {
                checks.add(
                    HeaderCheck(
                        headerName = "Content-Security-Policy",
                        detectedValue = csp.take(120) + if (csp.length > 120) "..." else "",
                        isPresent = true,
                        importance = "Crucial",
                        status = "PASS",
                        assessment = "Strong Content-Security-Policy implemented without unsafe inline execution.",
                        recommendedValue = csp,
                        potentialRisk = "None identified."
                    )
                )
            }
        }

        // 3. X-Content-Type-Options
        val xcto = headers["X-Content-Type-Options"]
        if (xcto == null || !xcto.equals("nosniff", ignoreCase = true)) {
            deductions += 5
            checks.add(
                HeaderCheck(
                    headerName = "X-Content-Type-Options",
                    detectedValue = xcto,
                    isPresent = xcto != null,
                    importance = "Crucial",
                    status = "FAIL",
                    assessment = "Missing or improper nosniff directive. Browsers may inspect content and execute unexpected MIME types.",
                    recommendedValue = "nosniff",
                    potentialRisk = "MIME confusion attacks where non-executable files (e.g. user images) are interpreted as script or stylesheet."
                )
            )
            findings.add(
                Finding(
                    id = UUID.randomUUID().toString(),
                    scanId = scanId,
                    category = "Headers",
                    title = "Missing X-Content-Type-Options: nosniff",
                    severity = Severity.MEDIUM,
                    confidence = "Confirmed",
                    affectedUrl = targetUrl,
                    evidence = "X-Content-Type-Options is ${xcto ?: "not set"}",
                    explanation = "X-Content-Type-Options instructs the browser to adhere strictly to MIME types advertised in Content-Type.",
                    impact = "Can lead to cross-site script execution via MIME sniffing vulnerabilities.",
                    remediation = "Add 'X-Content-Type-Options: nosniff' header to all responses.",
                    cwe = "CWE-16"
                )
            )
        } else {
            checks.add(
                HeaderCheck(
                    headerName = "X-Content-Type-Options",
                    detectedValue = xcto,
                    isPresent = true,
                    importance = "Crucial",
                    status = "PASS",
                    assessment = "Properly configured to 'nosniff'.",
                    recommendedValue = "nosniff",
                    potentialRisk = "None."
                )
            )
        }

        // 4. X-Frame-Options
        val xfo = headers["X-Frame-Options"]
        val cspHasFrameAncestors = csp?.contains("frame-ancestors") == true
        if (xfo == null && !cspHasFrameAncestors) {
            deductions += 5
            checks.add(
                HeaderCheck(
                    headerName = "X-Frame-Options",
                    detectedValue = null,
                    isPresent = false,
                    importance = "Crucial",
                    status = "FAIL",
                    assessment = "Neither X-Frame-Options nor CSP frame-ancestors is configured. Site can be embedded into malicious iframes.",
                    recommendedValue = "DENY or SAMEORIGIN",
                    potentialRisk = "Clickjacking attacks where users are tricked into clicking invisible overlaid buttons."
                )
            )
            findings.add(
                Finding(
                    id = UUID.randomUUID().toString(),
                    scanId = scanId,
                    category = "Headers",
                    title = "Missing Clickjacking Protection (X-Frame-Options)",
                    severity = Severity.HIGH,
                    confidence = "Confirmed",
                    affectedUrl = targetUrl,
                    evidence = "Neither X-Frame-Options nor frame-ancestors found in response",
                    explanation = "Clickjacking allows malicious actors to embed your pages into transparent iframes, tricking users into sensitive actions.",
                    impact = "Unauthorized account actions, profile tampering, or rogue transactional confirmations.",
                    remediation = "Set 'X-Frame-Options: DENY' or 'X-Frame-Options: SAMEORIGIN', or use CSP 'frame-ancestors 'self''.",
                    cwe = "CWE-1021"
                )
            )
        } else {
            checks.add(
                HeaderCheck(
                    headerName = "X-Frame-Options",
                    detectedValue = xfo ?: "(Protected via CSP frame-ancestors)",
                    isPresent = true,
                    importance = "Crucial",
                    status = "PASS",
                    assessment = "Clickjacking protection is active (${xfo ?: "CSP frame-ancestors"}).",
                    recommendedValue = "DENY",
                    potentialRisk = "None."
                )
            )
        }

        // 5. Referrer-Policy
        val referrer = headers["Referrer-Policy"]
        if (referrer == null) {
            deductions += 3
            checks.add(
                HeaderCheck(
                    headerName = "Referrer-Policy",
                    detectedValue = null,
                    isPresent = false,
                    importance = "Recommended",
                    status = "WARN",
                    assessment = "Referrer-Policy header is missing. Default browser referrer behavior may leak full query paths cross-origin.",
                    recommendedValue = "strict-origin-when-cross-origin",
                    potentialRisk = "Sensitive URL parameters, reset tokens, or private IDs may be leaked to external analytics/third parties."
                )
            )
            findings.add(
                Finding(
                    id = UUID.randomUUID().toString(),
                    scanId = scanId,
                    category = "Headers",
                    title = "Missing Referrer-Policy Header",
                    severity = Severity.LOW,
                    confidence = "Confirmed",
                    affectedUrl = targetUrl,
                    evidence = "Referrer-Policy header omitted",
                    explanation = "Controls how much referrer information (URL parameters and query strings) is sent along with requests.",
                    impact = "May leak sensitive URL query parameters to third-party domains.",
                    remediation = "Add: Referrer-Policy: strict-origin-when-cross-origin",
                    cwe = "CWE-200"
                )
            )
        } else {
            checks.add(
                HeaderCheck(
                    headerName = "Referrer-Policy",
                    detectedValue = referrer,
                    isPresent = true,
                    importance = "Recommended",
                    status = "PASS",
                    assessment = "Referrer policy is explicitly declared: $referrer",
                    recommendedValue = "strict-origin-when-cross-origin",
                    potentialRisk = "None."
                )
            )
        }

        // 6. Permissions-Policy
        val permPolicy = headers["Permissions-Policy"]
        if (permPolicy == null) {
            deductions += 3
            checks.add(
                HeaderCheck(
                    headerName = "Permissions-Policy",
                    detectedValue = null,
                    isPresent = false,
                    importance = "Recommended",
                    status = "WARN",
                    assessment = "Permissions-Policy is missing. Hardware APIs (camera, microphone, geolocation) are not explicitly restricted.",
                    recommendedValue = "camera=(), microphone=(), geolocation=(), payment=()",
                    potentialRisk = "Malicious or rogue third-party widgets can query sensitive device capabilities."
                )
            )
        } else {
            checks.add(
                HeaderCheck(
                    headerName = "Permissions-Policy",
                    detectedValue = permPolicy.take(80),
                    isPresent = true,
                    importance = "Recommended",
                    status = "PASS",
                    assessment = "Hardware and browser device APIs restricted.",
                    recommendedValue = permPolicy,
                    potentialRisk = "None."
                )
            )
        }

        // 7. Cross-Origin-Opener-Policy (COOP)
        val coop = headers["Cross-Origin-Opener-Policy"]
        if (coop == null) {
            deductions += 2
            checks.add(
                HeaderCheck(
                    headerName = "Cross-Origin-Opener-Policy",
                    detectedValue = null,
                    isPresent = false,
                    importance = "Recommended",
                    status = "WARN",
                    assessment = "COOP is missing. Browsing context is not isolated from cross-origin tabs.",
                    recommendedValue = "same-origin",
                    potentialRisk = "Cross-origin windows can retain window references and execute XS-Leaks."
                )
            )
        } else {
            checks.add(
                HeaderCheck(
                    headerName = "Cross-Origin-Opener-Policy",
                    detectedValue = coop,
                    isPresent = true,
                    importance = "Recommended",
                    status = "PASS",
                    assessment = "Process isolation and top-level browsing context safeguarded.",
                    recommendedValue = "same-origin",
                    potentialRisk = "None."
                )
            )
        }

        // 8. Cross-Origin-Resource-Policy (CORP)
        val corp = headers["Cross-Origin-Resource-Policy"]
        if (corp == null) {
            deductions += 2
            checks.add(
                HeaderCheck(
                    headerName = "Cross-Origin-Resource-Policy",
                    detectedValue = null,
                    isPresent = false,
                    importance = "Optional",
                    status = "WARN",
                    assessment = "CORP is not set. Cross-origin sites can load resources (images/scripts) unless CORS blocks.",
                    recommendedValue = "same-origin",
                    potentialRisk = "Cross-origin read attacks and speculative side-channel attacks."
                )
            )
        } else {
            checks.add(
                HeaderCheck(
                    headerName = "Cross-Origin-Resource-Policy",
                    detectedValue = corp,
                    isPresent = true,
                    importance = "Optional",
                    status = "PASS",
                    assessment = "Explicit resource isolation active.",
                    recommendedValue = "same-origin",
                    potentialRisk = "None."
                )
            )
        }

        return HeadersScanResult(checks, findings, deductions)
    }

    private fun extractMaxAge(hsts: String): Long {
        val parts = hsts.split(";")
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.startsWith("max-age=", ignoreCase = true)) {
                return trimmed.substring(8).trim().toLongOrNull() ?: 0L
            }
        }
        return 0L
    }
}
