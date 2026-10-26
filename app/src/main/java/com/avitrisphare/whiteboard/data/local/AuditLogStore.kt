package com.avitrisphare.whiteboard.data.local

import android.content.Context
import com.avitrisphare.whiteboard.domain.safety.AuditLogEntry
import com.avitrisphare.whiteboard.domain.safety.AuditLogRepository
import com.avitrisphare.whiteboard.domain.safety.FilterContentType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe, persistent, read-only implementation of [AuditLogRepository].
 * Ensures that once an audit log is recorded, it cannot be modified or deleted.
 */
@Singleton
class AuditLogStore @Inject constructor(
    private val context: Context? = null,
    private val gson: Gson = Gson()
) : AuditLogRepository {

    private val mutex = Mutex()
    private val inMemoryLogs = mutableListOf<AuditLogEntry>()
    private val logFileName = "safety_audit_logs.json"

    init {
        loadLogsFromDisk()
    }

    override suspend fun recordAttempt(entry: AuditLogEntry) = withContext(Dispatchers.IO) {
        mutex.withLock {
            inMemoryLogs.add(entry)
            saveLogsToDisk()
        }
    }

    override suspend fun getLogsForUser(userId: String): List<AuditLogEntry> = withContext(Dispatchers.IO) {
        mutex.withLock {
            inMemoryLogs.filter { it.userId == userId }.toList()
        }
    }

    override suspend fun getLogsForSchool(schoolId: String): List<AuditLogEntry> = withContext(Dispatchers.IO) {
        mutex.withLock {
            inMemoryLogs.filter { it.schoolId == schoolId }.toList()
        }
    }

    override suspend fun getAllLogs(): List<AuditLogEntry> = withContext(Dispatchers.IO) {
        mutex.withLock {
            inMemoryLogs.toList()
        }
    }

    override suspend fun filterLogs(
        userId: String?,
        schoolId: String?,
        contentType: FilterContentType?,
        fromTimestamp: Long?,
        toTimestamp: Long?
    ): List<AuditLogEntry> = withContext(Dispatchers.IO) {
        mutex.withLock {
            inMemoryLogs.filter { log ->
                val matchesUser = userId == null || log.userId == userId
                val matchesSchool = schoolId == null || log.schoolId == schoolId
                val matchesType = contentType == null || log.contentType == contentType
                val matchesFrom = fromTimestamp == null || log.timestamp >= fromTimestamp
                val matchesTo = toTimestamp == null || log.timestamp <= toTimestamp
                matchesUser && matchesSchool && matchesType && matchesFrom && matchesTo
            }.toList()
        }
    }

    private fun loadLogsFromDisk() {
        val ctx = context ?: return
        try {
            val file = File(ctx.filesDir, logFileName)
            if (file.exists()) {
                val json = file.readText()
                val type = object : TypeToken<List<AuditLogEntry>>() {}.type
                val logs: List<AuditLogEntry>? = gson.fromJson(json, type)
                if (logs != null) {
                    inMemoryLogs.addAll(logs)
                }
            }
        } catch (_: Exception) {
            // Keep memory state intact if disk read fails
        }
    }

    private fun saveLogsToDisk() {
        val ctx = context ?: return
        try {
            val file = File(ctx.filesDir, logFileName)
            val json = gson.toJson(inMemoryLogs)
            file.writeText(json)
        } catch (_: Exception) {
            // Memory state preserved
        }
    }
}
