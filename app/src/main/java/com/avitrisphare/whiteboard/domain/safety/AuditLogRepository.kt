package com.avitrisphare.whiteboard.domain.safety

/**
 * Repository interface for managing safety audit logs.
 * Acceptance Criteria:
 * "Profanity attempts logged in audit log per user"
 *
 * Story #6 Compliance:
 * - Records action type, user ID, role, IP address, timestamp
 * - Read-only (immutable once created, cannot be modified or deleted)
 * - Searchable and filterable by user, school, content type, date range
 */
interface AuditLogRepository {

    /**
     * Records a new audit log entry.
     * Guaranteed to be append-only.
     */
    suspend fun recordAttempt(entry: AuditLogEntry)

    /**
     * Retrieves all audit logs recorded for a specific user.
     */
    suspend fun getLogsForUser(userId: String): List<AuditLogEntry>

    /**
     * Retrieves all audit logs recorded for a specific school.
     */
    suspend fun getLogsForSchool(schoolId: String): List<AuditLogEntry>

    /**
     * Retrieves all audit logs in the system (read-only view for Admins).
     */
    suspend fun getAllLogs(): List<AuditLogEntry>

    /**
     * Filters audit logs by user, school, content type, or timestamp range.
     */
    suspend fun filterLogs(
        userId: String? = null,
        schoolId: String? = null,
        contentType: FilterContentType? = null,
        fromTimestamp: Long? = null,
        toTimestamp: Long? = null
    ): List<AuditLogEntry>
}
