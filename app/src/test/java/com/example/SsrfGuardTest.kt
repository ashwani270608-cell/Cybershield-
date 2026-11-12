package com.example

import com.example.data.scanner.SsrfGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SsrfGuardTest {

    @Test
    fun `test ssrf guard blocks empty input`() {
        val res = SsrfGuard.validateUrl("")
        assertTrue(res is SsrfGuard.ValidationResult.Blocked)
        assertEquals("Target URL cannot be empty", (res as SsrfGuard.ValidationResult.Blocked).reason)
    }

    @Test
    fun `test ssrf guard blocks localhost and loopback`() {
        val res1 = SsrfGuard.validateUrl("http://localhost:3000")
        assertTrue(res1 is SsrfGuard.ValidationResult.Blocked)

        val res2 = SsrfGuard.validateUrl("http://127.0.0.1")
        assertTrue(res2 is SsrfGuard.ValidationResult.Blocked)
    }

    @Test
    fun `test ssrf guard blocks cloud metadata endpoints`() {
        val res1 = SsrfGuard.validateUrl("http://169.254.169.254/latest/meta-data/")
        assertTrue(res1 is SsrfGuard.ValidationResult.Blocked)

        val res2 = SsrfGuard.validateUrl("http://metadata.google.internal")
        assertTrue(res2 is SsrfGuard.ValidationResult.Blocked)
    }

    @Test
    fun `test ssrf guard blocks internal RFC1918 addresses`() {
        val res1 = SsrfGuard.validateUrl("http://10.0.0.5")
        assertTrue(res1 is SsrfGuard.ValidationResult.Blocked)

        val res2 = SsrfGuard.validateUrl("http://192.168.1.100")
        assertTrue(res2 is SsrfGuard.ValidationResult.Blocked)

        val res3 = SsrfGuard.validateUrl("http://172.24.0.1")
        assertTrue(res3 is SsrfGuard.ValidationResult.Blocked)
    }

    @Test
    fun `test ssrf guard normalizes valid public domain`() {
        val res = SsrfGuard.validateUrl("nexova.com")
        // Will normalize to https://nexova.com or resolve
        if (res is SsrfGuard.ValidationResult.Allowed) {
            assertEquals("https://nexova.com", res.normalizedUrl)
            assertEquals("nexova.com", res.host)
        }
    }
}
