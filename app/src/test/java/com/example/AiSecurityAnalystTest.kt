package com.example

import com.example.data.ai.AiSecurityAnalyst
import com.example.data.model.Finding
import com.example.data.model.ScanProfile
import com.example.data.model.SecurityScan
import com.example.data.model.Severity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiSecurityAnalystTest {

    private val sampleFinding = Finding(
        id = "f-1",
        scanId = "scan-1",
        category = "Headers",
        title = "Missing Content-Security-Policy (CSP)",
        severity = Severity.HIGH,
        confidence = "Confirmed",
        affectedUrl = "https://nexova.com",
        evidence = "Header missing",
        explanation = "CSP protects against XSS",
        impact = "Script injection",
        remediation = "Add CSP header"
    )

    @Test
    fun `test beginner explanation generation`() {
        val explanation = AiSecurityAnalyst.getBeginnerExplanation(sampleFinding)
        assertTrue(explanation.contains("bouncer at the door"))
    }

    @Test
    fun `test remediation snippet contains NGINX config`() {
        val snippet = AiSecurityAnalyst.getRemediationSnippet(sampleFinding, "nexova.com")
        assertTrue(snippet.contains("Content-Security-Policy"))
        assertTrue(snippet.contains("NGINX Configuration"))
    }

    @Test
    fun `test false positive analysis`() {
        val analysis = AiSecurityAnalyst.getFalsePositiveAnalysis(sampleFinding)
        assertTrue(analysis.contains("False Positive"))
    }

    @Test
    fun `test AI consultation returns code snippet and checklist`() = runBlocking {
        val scan = SecurityScan(
            id = "scan-1",
            targetUrl = "https://nexova.com",
            domain = "nexova.com",
            timestamp = 1000L,
            overallScore = 75,
            grade = "B",
            status = "COMPLETED",
            durationMs = 1000,
            authorizationConfirmed = true,
            authorizationProof = "Auth",
            scanProfile = ScanProfile.FULL_AUDIT,
            criticalCount = 0,
            highCount = 1,
            mediumCount = 0,
            lowCount = 0,
            sslScore = 25,
            headersScore = 20,
            cookieScore = 15,
            transportScore = 10,
            exposureScore = 5,
            executiveSummary = "Summary"
        )
        val response = AiSecurityAnalyst.consult("Give me the Nginx config for CSP", scan, listOf(sampleFinding))
        assertFalse(response.isUser)
        assertNotNull(response.codeSnippet)
        assertTrue(response.codeSnippet!!.contains("Content-Security-Policy"))
        assertNotNull(response.checklist)
    }
}
