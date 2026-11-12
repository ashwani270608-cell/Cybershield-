package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.AuditLog
import com.example.data.model.Finding
import com.example.data.model.MonitoredAsset
import com.example.data.model.ScanProfile
import com.example.data.model.SecurityScan
import com.example.data.model.Severity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CyberShieldDatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `test insert and query scan and findings`() = runBlocking {
        val scan = SecurityScan(
            id = "test-scan-1",
            targetUrl = "https://nexova.com",
            domain = "nexova.com",
            timestamp = System.currentTimeMillis(),
            overallScore = 90,
            grade = "A",
            status = "COMPLETED",
            durationMs = 1500,
            authorizationConfirmed = true,
            authorizationProof = "Explicit Sign-off",
            scanProfile = ScanProfile.FULL_AUDIT,
            criticalCount = 0,
            highCount = 0,
            mediumCount = 1,
            lowCount = 2,
            sslScore = 25,
            headersScore = 30,
            cookieScore = 15,
            transportScore = 15,
            exposureScore = 5,
            executiveSummary = "Test Summary"
        )
        db.scanDao().insertScan(scan)

        val retrieved = db.scanDao().getScanById("test-scan-1")
        assertNotNull(retrieved)
        assertEquals("nexova.com", retrieved?.domain)
        assertEquals(90, retrieved?.overallScore)

        val finding = Finding(
            id = "f-1",
            scanId = "test-scan-1",
            category = "Headers",
            title = "Missing COOP",
            severity = Severity.LOW,
            confidence = "Confirmed",
            affectedUrl = "https://nexova.com",
            evidence = "Header not found",
            explanation = "COOP isolation",
            impact = "Low",
            remediation = "Add COOP"
        )
        db.scanDao().insertFindings(listOf(finding))

        val findingsList = db.scanDao().getFindingsForScan("test-scan-1").first()
        assertEquals(1, findingsList.size)
        assertEquals("Missing COOP", findingsList[0].title)
    }

    @Test
    fun `test insert and update monitored asset`() = runBlocking {
        val asset = MonitoredAsset(
            domain = "testdomain.org",
            displayName = "Test Domain",
            verified = true,
            verificationMethod = "DNS_TXT",
            verificationToken = "CS-VERIFY-9999-TEST",
            lastScanDate = 1000L,
            lastScore = 75,
            monitoringEnabled = true,
            certExpiryDays = 40
        )
        db.assetDao().insertAsset(asset)

        val retrieved = db.assetDao().getAsset("testdomain.org")
        assertNotNull(retrieved)
        assertEquals(75, retrieved?.lastScore)

        // Update score
        db.assetDao().updateAsset(retrieved!!.copy(lastScore = 88))
        val updated = db.assetDao().getAsset("testdomain.org")
        assertEquals(88, updated?.lastScore)
    }

    @Test
    fun `test audit log insertion`() = runBlocking {
        val log = AuditLog(
            id = "log-1",
            timestamp = System.currentTimeMillis(),
            action = "USER_LOGIN",
            targetDomain = "nexova.com",
            actor = "sec@nexova.com",
            status = "SUCCESS",
            details = "Logged in successfully"
        )
        db.auditDao().insertLog(log)

        val logs = db.auditDao().getRecentLogs().first()
        assertEquals(1, logs.size)
        assertEquals("USER_LOGIN", logs[0].action)
    }
}
