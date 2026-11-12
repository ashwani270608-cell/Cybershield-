package com.example

import com.example.data.model.Severity
import com.example.data.scanner.HeadersAnalyzerEngine
import okhttp3.Headers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeadersAnalyzerEngineTest {

    @Test
    fun `test analyze flags completely missing security headers`() {
        val emptyHeaders = Headers.headersOf()
        val result = HeadersAnalyzerEngine.analyze(emptyHeaders, "https://test.com", "scan-1")

        // Should check all 8 headers
        assertEquals(8, result.headerChecks.size)

        // CSP should be FAIL
        val csp = result.headerChecks.first { it.headerName == "Content-Security-Policy" }
        assertFalse(csp.isPresent)
        assertEquals("FAIL", csp.status)

        // HSTS should be FAIL
        val hsts = result.headerChecks.first { it.headerName == "Strict-Transport-Security" }
        assertFalse(hsts.isPresent)
        assertEquals("FAIL", hsts.status)

        // Deductions should be significant
        assertTrue(result.scoreDeductions >= 30)

        // Findings should include HIGH severity for missing CSP and HSTS
        assertTrue(result.findings.any { it.title.contains("CSP") && it.severity == Severity.HIGH })
        assertTrue(result.findings.any { it.title.contains("HSTS") && it.severity == Severity.HIGH })
    }

    @Test
    fun `test analyze accepts hardened security headers`() {
        val hardenedHeaders = Headers.headersOf(
            "Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload",
            "Content-Security-Policy", "default-src 'self'; script-src 'self'; object-src 'none';",
            "X-Content-Type-Options", "nosniff",
            "X-Frame-Options", "DENY",
            "Referrer-Policy", "strict-origin-when-cross-origin",
            "Permissions-Policy", "camera=(), microphone=()",
            "Cross-Origin-Opener-Policy", "same-origin",
            "Cross-Origin-Resource-Policy", "same-origin"
        )
        val result = HeadersAnalyzerEngine.analyze(hardenedHeaders, "https://test.com", "scan-2")

        // All should be present
        assertTrue(result.headerChecks.all { it.isPresent })
        assertTrue(result.headerChecks.all { it.status == "PASS" })

        // Zero deductions
        assertEquals(0, result.scoreDeductions)
        // No high or critical findings
        assertFalse(result.findings.any { it.severity == Severity.HIGH || it.severity == Severity.CRITICAL })
    }

    @Test
    fun `test analyze warns on unsafe-inline in CSP`() {
        val weakCspHeaders = Headers.headersOf(
            "Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline';"
        )
        val result = HeadersAnalyzerEngine.analyze(weakCspHeaders, "https://test.com", "scan-3")

        val csp = result.headerChecks.first { it.headerName == "Content-Security-Policy" }
        assertTrue(csp.isPresent)
        assertEquals("WARN", csp.status)
        assertTrue(csp.assessment.contains("unsafe keywords"))
    }
}
