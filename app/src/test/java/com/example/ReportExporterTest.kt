package com.example

import com.example.data.model.Finding
import com.example.data.model.ScanProfile
import com.example.data.model.SecurityScan
import com.example.data.model.Severity
import com.example.data.reporting.ReportExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportExporterTest {

    private val sampleScan = SecurityScan(
        id = "scan-test-123",
        targetUrl = "https://example.com",
        domain = "example.com",
        timestamp = 1700000000000L,
        overallScore = 85,
        grade = "A",
        status = "COMPLETED",
        durationMs = 1200,
        authorizationConfirmed = true,
        authorizationProof = "Signed Agreement",
        scanProfile = ScanProfile.FULL_AUDIT,
        criticalCount = 0,
        highCount = 1,
        mediumCount = 2,
        lowCount = 0,
        sslScore = 25,
        headersScore = 25,
        cookieScore = 15,
        transportScore = 15,
        exposureScore = 5,
        executiveSummary = "Security posture is strong with minor header gaps."
    )

    private val sampleFindings = listOf(
        Finding(
            id = "f-1",
            scanId = "scan-test-123",
            category = "Headers",
            title = "Missing Permissions-Policy",
            severity = Severity.HIGH,
            confidence = "Confirmed",
            affectedUrl = "https://example.com",
            evidence = "Header omitted",
            explanation = "Hardware APIs not restricted",
            impact = "Low risk",
            remediation = "Add Permissions-Policy",
            cwe = "CWE-1021"
        )
    )

    @Test
    fun `test json report generation contains required fields`() {
        val json = ReportExporter.generateJsonReport(sampleScan, sampleFindings)
        assertTrue(json.contains("\"scanId\": \"scan-test-123\""))
        assertTrue(json.contains("\"domain\": \"example.com\""))
        assertTrue(json.contains("\"overallScore\": 85"))
        assertTrue(json.contains("\"grade\": \"A\""))
        assertTrue(json.contains("\"Missing Permissions-Policy\""))
    }

    @Test
    fun `test csv report generation contains valid header and rows`() {
        val csv = ReportExporter.generateCsvReport(sampleScan, sampleFindings)
        val lines = csv.trim().lines()
        assertTrue(lines.size >= 2)
        assertEquals("Scan ID,Domain,Timestamp,Overall Score,Grade,Severity,Category,Finding Title,Evidence,Remediation,CWE", lines[0])
        assertTrue(lines[1].contains("scan-test-123"))
        assertTrue(lines[1].contains("example.com"))
        assertTrue(lines[1].contains("Missing Permissions-Policy"))
    }

    @Test
    fun `test executive text report generation format`() {
        val text = ReportExporter.generateExecutiveTextReport(sampleScan, sampleFindings)
        assertTrue(text.contains("CYBERSHIELD AI - SECURITY AUDIT REPORT"))
        assertTrue(text.contains("TARGET DOMAIN:       example.com"))
        assertTrue(text.contains("OVERALL SCORE:       85 / 100"))
        assertTrue(text.contains("[1] CATEGORY BREAKDOWN"))
        assertTrue(text.contains("[4] DETAILED FINDINGS & REMEDIATION"))
    }
}
