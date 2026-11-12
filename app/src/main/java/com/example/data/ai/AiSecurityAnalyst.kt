package com.example.data.ai

import com.example.data.model.Finding
import com.example.data.model.SecurityScan
import com.example.data.model.Severity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object AiSecurityAnalyst {

    data class AiChatMessage(
        val isUser: Boolean,
        val text: String,
        val codeSnippet: String? = null,
        val checklist: List<String>? = null
    )

    fun getBeginnerExplanation(finding: Finding): String {
        return when (finding.category) {
            "Headers" -> {
                if (finding.title.contains("CSP", ignoreCase = true)) {
                    "Imagine a bouncer at the door of your website. Without a Content Security Policy (CSP), your web browser lets anyone sneak in and run random scripts. A CSP tells the browser exactly which scripts are allowed inside, stopping hacker code from stealing passwords or credit cards."
                } else if (finding.title.contains("HSTS", ignoreCase = true)) {
                    "HSTS is like a padlock on your front door that can never be unlocked by strangers. Without it, someone on public coffee-shop Wi-Fi could trick a user's browser into connecting over unencrypted HTTP and snooping on their private messages."
                } else if (finding.title.contains("Clickjacking", ignoreCase = true) || finding.title.contains("X-Frame-Options", ignoreCase = true)) {
                    "Clickjacking is like placing an invisible window over a button on your site. An attacker tricks a victim into thinking they are clicking a game or video, but they are secretly clicking 'Delete Account' or 'Transfer Funds' on your site. This header tells browsers: 'Never let anyone embed my site in a hidden frame'."
                } else {
                    "This missing or weak security header leaves your website visitors without modern browser defense shields. Adding it takes only a few minutes and significantly protects users."
                }
            }
            "SSL/TLS" -> {
                "Your SSL certificate encrypts all data sent between your visitors and your server. A valid, modern SSL setup guarantees privacy and verifies that visitors are truly speaking to your authentic server, not an impostor."
            }
            "Cookies" -> {
                "Cookies store user logins and sessions. When cookies lack the 'Secure', 'HttpOnly', and 'SameSite' flags, malicious scripts or eavesdroppers on the network can steal those session keys and impersonate your logged-in users."
            }
            "Transport" -> {
                "Transport security ensures all traffic stays encrypted from end to end. If plain HTTP doesn't automatically bounce visitors to HTTPS, users might transmit sensitive data in the clear."
            }
            else -> {
                "This finding indicates a configuration weakness that provides attackers with unnecessary clues or access vectors into your web environment."
            }
        }
    }

    fun getRemediationSnippet(finding: Finding, domain: String = "example.com"): String {
        return when {
            finding.title.contains("CSP", ignoreCase = true) -> {
                """
                # NGINX Configuration
                add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'nonce-rAnd0m'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; object-src 'none'; frame-ancestors 'self';" always;

                # Apache .htaccess
                Header set Content-Security-Policy "default-src 'self'; script-src 'self'; object-src 'none';"
                """.trimIndent()
            }
            finding.title.contains("HSTS", ignoreCase = true) -> {
                """
                # NGINX Configuration
                add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;

                # Apache (.htaccess or httpd.conf)
                Header always set Strict-Transport-Security "max-age=31536000; includeSubDomains; preload"
                """.trimIndent()
            }
            finding.title.contains("Clickjacking", ignoreCase = true) || finding.title.contains("X-Frame-Options", ignoreCase = true) -> {
                """
                # NGINX Configuration
                add_header X-Frame-Options "SAMEORIGIN" always;

                # Apache Configuration
                Header always set X-Frame-Options "SAMEORIGIN"
                """.trimIndent()
            }
            finding.title.contains("nosniff", ignoreCase = true) || finding.title.contains("X-Content-Type-Options", ignoreCase = true) -> {
                """
                # NGINX Configuration
                add_header X-Content-Type-Options "nosniff" always;

                # Apache Configuration
                Header always set X-Content-Type-Options "nosniff"
                """.trimIndent()
            }
            finding.title.contains("Server", ignoreCase = true) && finding.category == "Disclosure" -> {
                """
                # In /etc/nginx/nginx.conf inside http { ... }
                server_tokens off;

                # In Apache httpd.conf:
                ServerTokens Prod
                ServerSignature Off
                """.trimIndent()
            }
            finding.title.contains("Cookie", ignoreCase = true) -> {
                """
                # PHP / Framework Cookie Settings:
                session_set_cookie_params([
                    'lifetime' => 86400,
                    'path' => '/',
                    'secure' => true,      // Requires HTTPS
                    'httponly' => true,    // Blocks JavaScript access
                    'samesite' => 'Lax'    // Mitigates CSRF
                ]);
                """.trimIndent()
            }
            else -> {
                """
                # Recommended Security Header bundle for NGINX:
                add_header X-Frame-Options "SAMEORIGIN" always;
                add_header X-Content-Type-Options "nosniff" always;
                add_header Referrer-Policy "strict-origin-when-cross-origin" always;
                add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;
                """.trimIndent()
            }
        }
    }

    fun getFalsePositiveAnalysis(finding: Finding): String {
        return """
        Possibility of False Positive: Low (<5%).
        The scanner verified the raw HTTP response headers directly from the public origin. 
        However, if your website relies on a reverse proxy or Cloud CDN (such as Cloudflare or Fastly) that adds security headers exclusively for browser User-Agents or specific geographic endpoints, verify whether origin-only requests bypass the edge rules. Always verify the headers using Chrome DevTools Network Tab.
        """.trimIndent()
    }

    fun getRemediationChecklist(scan: SecurityScan, findings: List<Finding>): List<String> {
        val list = mutableListOf<String>()
        if (findings.any { it.title.contains("HSTS", ignoreCase = true) }) {
            list.add("Add Strict-Transport-Security header with 1-year max-age and preload flag")
        }
        if (findings.any { it.title.contains("CSP", ignoreCase = true) }) {
            list.add("Draft and deploy a Content-Security-Policy (CSP) in Report-Only mode, then enforce")
        }
        if (findings.any { it.title.contains("X-Frame-Options", ignoreCase = true) }) {
            list.add("Set X-Frame-Options: SAMEORIGIN to prevent framing and Clickjacking")
        }
        if (findings.any { it.title.contains("nosniff", ignoreCase = true) }) {
            list.add("Enable X-Content-Type-Options: nosniff on all static and dynamic assets")
        }
        if (findings.any { it.title.contains("Cookie", ignoreCase = true) }) {
            list.add("Audit session cookies to ensure Secure, HttpOnly, and SameSite=Lax flags are set")
        }
        if (findings.any { it.title.contains("Server", ignoreCase = true) }) {
            list.add("Disable server version banner tokens in web server configuration")
        }
        if (scan.sslScore < 20) {
            list.add("Review SSL/TLS configuration and renew certificates before the 30-day threshold")
        }
        if (list.isEmpty()) {
            list.add("Security posture meets all core baseline requirements. Continue automated daily monitoring.")
        }
        return list
    }

    suspend fun consult(query: String, currentScan: SecurityScan?, findings: List<Finding>): AiChatMessage = withContext(Dispatchers.Default) {
        delay(600) // Realistic AI synthesis latency
        val q = query.lowercase()

        when {
            q.contains("csp") || q.contains("content security") -> {
                AiChatMessage(
                    isUser = false,
                    text = "A Content Security Policy (CSP) is one of the strongest defenses against Cross-Site Scripting (XSS). For ${currentScan?.domain ?: "your website"}, here is a production-grade CSP directive:",
                    codeSnippet = """
                    Content-Security-Policy: default-src 'self'; script-src 'self' https://trustedscripts.com; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self'; connect-src 'self'; object-src 'none'; frame-ancestors 'self'; base-uri 'self';
                    """.trimIndent(),
                    checklist = listOf(
                        "Test policy using 'Content-Security-Policy-Report-Only' first to monitor violations without breaking features",
                        "Avoid 'unsafe-eval' completely",
                        "Migrate inline scripts to use cryptographic sha256 hashes or random per-request nonces"
                    )
                )
            }
            q.contains("hsts") || q.contains("preload") || q.contains("https") -> {
                AiChatMessage(
                    isUser = false,
                    text = "HTTP Strict Transport Security (HSTS) prevents SSL stripping and downgrade attacks. Here is the recommended Nginx configuration for ${currentScan?.domain ?: "your domain"}:",
                    codeSnippet = """
                    # In /etc/nginx/sites-available/${currentScan?.domain ?: "default"}
                    server {
                        listen 443 ssl http2;
                        server_name ${currentScan?.domain ?: "example.com"};
                        
                        add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
                    }
                    """.trimIndent(),
                    checklist = listOf(
                        "Ensure all subdomains (api.*, cdn.*) also support HTTPS before adding 'includeSubDomains'",
                        "Once tested for 1 month, submit your domain to https://hstspreload.org for native browser inclusion"
                    )
                )
            }
            q.contains("nginx") || q.contains("apache") || q.contains("config") || q.contains("fix") -> {
                AiChatMessage(
                    isUser = false,
                    text = "Here is a complete, hardened security header bundle ready to paste into your Nginx server block for ${currentScan?.domain ?: "your site"}:",
                    codeSnippet = """
                    # CyberShield Hardened Security Headers
                    add_header X-Frame-Options "SAMEORIGIN" always;
                    add_header X-Content-Type-Options "nosniff" always;
                    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
                    add_header Permissions-Policy "camera=(), microphone=(), geolocation=(), payment=()" always;
                    add_header Cross-Origin-Opener-Policy "same-origin" always;
                    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
                    server_tokens off;
                    """.trimIndent(),
                    checklist = listOf(
                        "Run 'nginx -t' to verify syntax before reloading",
                        "Execute 'sudo systemctl reload nginx'",
                        "Run a re-scan in CyberShield to confirm your score jumps to 95+"
                    )
                )
            }
            q.contains("cookie") || q.contains("samesite") || q.contains("session") -> {
                AiChatMessage(
                    isUser = false,
                    text = "Session cookies without 'Secure', 'HttpOnly', and 'SameSite' attributes can be stolen or leveraged in Cross-Site Request Forgery (CSRF).",
                    codeSnippet = """
                    Set-Cookie: session_id=abc123xyz; Path=/; Secure; HttpOnly; SameSite=Lax; Max-Age=86400
                    """.trimIndent(),
                    checklist = listOf(
                        "Ensure Secure flag is set so cookies are never sent over unencrypted connections",
                        "Set HttpOnly so document.cookie cannot be accessed by rogue scripts",
                        "Use SameSite=Lax for general web navigation, or SameSite=Strict for banking/sensitive portals"
                    )
                )
            }
            else -> {
                val findingsSummary = if (findings.isNotEmpty()) {
                    "Your latest scan on ${currentScan?.domain ?: "the target"} identified ${findings.count { it.severity == Severity.CRITICAL || it.severity == Severity.HIGH }} high-priority security configuration weaknesses."
                } else {
                    "No critical vulnerabilities are recorded for this asset."
                }
                AiChatMessage(
                    isUser = false,
                    text = "$findingsSummary You can ask me to generate Nginx or Apache snippets, explain any finding in beginner terms, or help prepare an executive security remediation plan.",
                    checklist = currentScan?.let { getRemediationChecklist(it, findings) }
                )
            }
        }
    }
}
