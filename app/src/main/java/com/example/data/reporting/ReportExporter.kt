package com.example.data.reporting

import android.content.Context
import android.content.Intent
import com.example.data.model.Finding
import com.example.data.model.SecurityScan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US)

    fun generateExecutiveTextReport(scan: SecurityScan, findings: List<Finding>): String {
        val dateStr = dateFormat.format(Date(scan.timestamp))
        return buildString {
            appendLine("=======================================================")
            appendLine("        CYBERSHIELD AI - SECURITY AUDIT REPORT         ")
            appendLine("=======================================================")
            appendLine("TARGET DOMAIN:       ${scan.domain}")
            appendLine("TARGET URL:          ${scan.targetUrl}")
            appendLine("SCAN DATE:           $dateStr")
            appendLine("SCAN PROFILE:        ${scan.scanProfile.title}")
            appendLine("OVERALL SCORE:       ${scan.overallScore} / 100  (Grade: ${scan.grade})")
            appendLine("AUTHORIZATION:       CONFIRMED & DOCUMENTED")
            appendLine("AUTHORIZATION PROOF: ${scan.authorizationProof}")
            appendLine("SCANNER ENGINE:      CyberShield Defensive Core v2.4")
            appendLine("SCAN DURATION:       ${scan.durationMs} ms")
            appendLine("=======================================================")
            appendLine("\n[1] CATEGORY BREAKDOWN")
            appendLine("-------------------------------------------------------")
            appendLine("• SSL/TLS Security:       ${scan.sslScore} / 25")
            appendLine("• HTTP Security Headers:  ${scan.headersScore} / 35")
            appendLine("• Cookie Hardening:       ${scan.cookieScore} / 15")
            appendLine("• HTTPS Transport:        ${scan.transportScore} / 15")
            appendLine("• Information Exposure:   ${scan.exposureScore} / 10")
            appendLine("\n[2] EXECUTIVE SUMMARY")
            appendLine("-------------------------------------------------------")
            appendLine(scan.executiveSummary)
            appendLine("\n[3] FINDINGS SUMMARY")
            appendLine("-------------------------------------------------------")
            appendLine("Critical: ${scan.criticalCount}  |  High: ${scan.highCount}  |  Medium: ${scan.mediumCount}  |  Low: ${scan.lowCount}")
            appendLine("\n[4] DETAILED FINDINGS & REMEDIATION")
            appendLine("-------------------------------------------------------")
            if (findings.isEmpty()) {
                appendLine("No vulnerabilities identified during this scan.")
            } else {
                findings.forEachIndexed { idx, f ->
                    appendLine("\n#${idx + 1} [${f.severity.label.uppercase()}] ${f.title}")
                    appendLine("Category:    ${f.category}  |  CWE: ${f.cwe.ifEmpty { "N/A" }}")
                    appendLine("Affected:    ${f.affectedUrl}")
                    appendLine("Evidence:    ${f.evidence}")
                    appendLine("Explanation: ${f.explanation}")
                    appendLine("Impact:      ${f.impact}")
                    appendLine("Remediation: ${f.remediation}")
                    appendLine("-------------------------------------------------------")
                }
            }
            appendLine("\nDISCLAIMER: This automated report is an assessment of public defensive")
            appendLine("configurations at the time of scanning and does not guarantee that the")
            appendLine("target is immune to sophisticated attacks. Non-destructive scanning only.")
            appendLine("=======================================================")
        }
    }

    fun generateJsonReport(scan: SecurityScan, findings: List<Finding>): String {
        val findingsJson = findings.joinToString(",\n") { f ->
            """
            {
              "id": "${f.id}",
              "category": "${f.category}",
              "title": "${escapeJson(f.title)}",
              "severity": "${f.severity.name}",
              "affectedUrl": "${f.affectedUrl}",
              "evidence": "${escapeJson(f.evidence)}",
              "explanation": "${escapeJson(f.explanation)}",
              "impact": "${escapeJson(f.impact)}",
              "remediation": "${escapeJson(f.remediation)}",
              "cwe": "${f.cwe}"
            }
            """.trimIndent()
        }

        return """
        {
          "reportType": "CyberShield Security Audit",
          "scannerVersion": "2.4.0",
          "scanId": "${scan.id}",
          "domain": "${scan.domain}",
          "targetUrl": "${scan.targetUrl}",
          "timestamp": ${scan.timestamp},
          "overallScore": ${scan.overallScore},
          "grade": "${scan.grade}",
          "authorization": {
            "confirmed": ${scan.authorizationConfirmed},
            "proof": "${escapeJson(scan.authorizationProof)}"
          },
          "categoryScores": {
            "sslTls": ${scan.sslScore},
            "securityHeaders": ${scan.headersScore},
            "cookieSecurity": ${scan.cookieScore},
            "transportSecurity": ${scan.transportScore},
            "exposure": ${scan.exposureScore}
          },
          "findingsCounts": {
            "critical": ${scan.criticalCount},
            "high": ${scan.highCount},
            "medium": ${scan.mediumCount},
            "low": ${scan.lowCount}
          },
          "executiveSummary": "${escapeJson(scan.executiveSummary)}",
          "findings": [
            $findingsJson
          ]
        }
        """.trimIndent()
    }

    fun generateCsvReport(scan: SecurityScan, findings: List<Finding>): String {
        val sb = StringBuilder()
        sb.appendLine("Scan ID,Domain,Timestamp,Overall Score,Grade,Severity,Category,Finding Title,Evidence,Remediation,CWE")
        val dateStr = dateFormat.format(Date(scan.timestamp))
        for (f in findings) {
            sb.appendLine(
                listOf(
                    scan.id,
                    scan.domain,
                    "\"$dateStr\"",
                    scan.overallScore,
                    scan.grade,
                    f.severity.label,
                    f.category,
                    "\"${escapeCsv(f.title)}\"",
                    "\"${escapeCsv(f.evidence)}\"",
                    "\"${escapeCsv(f.remediation)}\"",
                    "\"${f.cwe}\""
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    fun shareReport(context: Context, reportContent: String, title: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, reportContent)
            putExtra(Intent.EXTRA_SUBJECT, title)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share CyberShield Report")
        context.startActivity(shareIntent)
    }

    private fun escapeJson(input: String): String {
        return input.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", " ")
            .replace("\r", "")
            .replace("\t", " ")
    }

    private fun escapeCsv(input: String): String {
        return input.replace("\"", "\"\"")
            .replace("\n", " ")
            .replace("\r", "")
    }
}
