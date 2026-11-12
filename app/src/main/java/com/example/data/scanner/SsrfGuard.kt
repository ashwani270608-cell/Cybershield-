package com.example.data.scanner

import java.net.InetAddress
import java.net.URI

object SsrfGuard {
    sealed class ValidationResult {
        data class Allowed(val normalizedUrl: String, val host: String) : ValidationResult()
        data class Blocked(val reason: String) : ValidationResult()
    }

    private val PRIVATE_CIDRS = listOf(
        "10.",
        "127.",
        "169.254.",
        "192.168.",
        "0.0.0.0",
        "localhost"
    )

    fun validateUrl(rawInput: String): ValidationResult {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            return ValidationResult.Blocked("Target URL cannot be empty")
        }

        val urlWithScheme = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }

        val uri = try {
            URI(urlWithScheme)
        } catch (e: Exception) {
            return ValidationResult.Blocked("Malformed URL: ${e.message}")
        }

        val host = uri.host?.lowercase() ?: return ValidationResult.Blocked("Missing hostname in URL")

        // 1. Block prohibited local/internal names
        if (host == "localhost" || host.endsWith(".local") || host.endsWith(".internal") || host == "metadata.google.internal") {
            return ValidationResult.Blocked("Scanning internal/localhost domains ($host) is strictly prohibited by security policy.")
        }

        // 2. Resolve DNS and check resolved IP addresses against private networks
        try {
            val addresses = InetAddress.getAllByName(host)
            for (addr in addresses) {
                val ip = addr.hostAddress ?: ""
                if (addr.isLoopbackAddress || addr.isSiteLocalAddress || addr.isLinkLocalAddress || addr.isAnyLocalAddress) {
                    return ValidationResult.Blocked("Security Protection: Host resolves to private/internal IP address ($ip). Scanning private infrastructure is disallowed.")
                }

                // Check 172.16.0.0 - 172.31.255.255
                if (ip.startsWith("172.")) {
                    val parts = ip.split(".")
                    val secondOctet = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    if (secondOctet in 16..31) {
                        return ValidationResult.Blocked("Security Protection: Host resolves to private IP block ($ip).")
                    }
                }

                for (prefix in PRIVATE_CIDRS) {
                    if (ip.startsWith(prefix)) {
                        return ValidationResult.Blocked("Security Protection: Host resolves to reserved/loopback address ($ip).")
                    }
                }
            }
        } catch (e: Exception) {
            return ValidationResult.Blocked("Unable to resolve domain '$host' via DNS: ${e.localizedMessage ?: "Name resolution failure"}")
        }

        return ValidationResult.Allowed(urlWithScheme, host)
    }
}
