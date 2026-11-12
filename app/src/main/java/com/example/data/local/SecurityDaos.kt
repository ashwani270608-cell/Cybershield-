package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AuditLog
import com.example.data.model.Finding
import com.example.data.model.MonitoredAsset
import com.example.data.model.SecurityScan
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: SecurityScan)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFindings(findings: List<Finding>)

    @Query("SELECT * FROM security_scans ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<SecurityScan>>

    @Query("SELECT * FROM security_scans WHERE id = :id LIMIT 1")
    suspend fun getScanById(id: String): SecurityScan?

    @Query("SELECT * FROM security_scans ORDER BY timestamp DESC LIMIT 1")
    fun getLatestScan(): Flow<SecurityScan?>

    @Query("SELECT * FROM findings WHERE scanId = :scanId ORDER BY severity ASC")
    fun getFindingsForScan(scanId: String): Flow<List<Finding>>

    @Query("SELECT * FROM findings ORDER BY severity ASC")
    fun getAllFindings(): Flow<List<Finding>>
}

@Dao
interface AssetDao {
    @Query("SELECT * FROM monitored_assets ORDER BY lastScanDate DESC")
    fun getAllAssets(): Flow<List<MonitoredAsset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: MonitoredAsset)

    @Update
    suspend fun updateAsset(asset: MonitoredAsset)

    @Query("SELECT * FROM monitored_assets WHERE domain = :domain LIMIT 1")
    suspend fun getAsset(domain: String): MonitoredAsset?
}

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLog)
}
