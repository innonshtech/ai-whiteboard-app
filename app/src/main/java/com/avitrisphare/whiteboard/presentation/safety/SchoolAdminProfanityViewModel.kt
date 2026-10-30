package com.avitrisphare.whiteboard.presentation.safety

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avitrisphare.whiteboard.domain.safety.AuditLogEntry
import com.avitrisphare.whiteboard.domain.safety.AuditLogRepository
import com.avitrisphare.whiteboard.domain.safety.FilterContentType
import com.avitrisphare.whiteboard.domain.safety.ProfanityFilterService
import com.avitrisphare.whiteboard.domain.safety.SchoolProfanityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SchoolAdminSafetyUiState(
    val schoolId: String = "",
    val customBlockedWords: List<String> = emptyList(),
    val auditLogs: List<AuditLogEntry> = emptyList(),
    val selectedFilterType: FilterContentType? = null,
    val selectedUserId: String? = null,
    val isLoading: Boolean = false,
    val statusMessage: String? = null
)

/**
 * ViewModel for School Admins to manage custom blocked words and inspect user audit logs.
 * Acceptance Criteria:
 * "School Admin can add custom words to the blocked list for their school"
 * "Profanity attempts logged in audit log per user"
 */
@HiltViewModel
class SchoolAdminProfanityViewModel @Inject constructor(
    private val profanityFilterService: ProfanityFilterService,
    private val schoolProfanityRepository: SchoolProfanityRepository,
    private val auditLogRepository: AuditLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SchoolAdminSafetyUiState())
    val uiState: StateFlow<SchoolAdminSafetyUiState> = _uiState.asStateFlow()

    fun loadSchoolSafetyData(schoolId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, schoolId = schoolId)
            val blockedWords = schoolProfanityRepository.getCustomBlockedWords(schoolId).sorted()
            val logs = auditLogRepository.getLogsForSchool(schoolId)
                .sortedByDescending { it.timestamp }

            _uiState.value = _uiState.value.copy(
                customBlockedWords = blockedWords,
                auditLogs = logs,
                isLoading = false
            )
        }
    }

    fun addCustomBlockedWord(schoolId: String, word: String, adminUserId: String) {
        if (word.isBlank()) return
        viewModelScope.launch {
            val success = profanityFilterService.addCustomBlockedWordsForSchool(
                schoolId = schoolId,
                words = listOf(word.trim()),
                adminUserId = adminUserId
            )
            if (success) {
                loadSchoolSafetyData(schoolId)
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Word '$word' added to school blocked list."
                )
            }
        }
    }

    fun removeCustomBlockedWord(schoolId: String, word: String, adminUserId: String) {
        viewModelScope.launch {
            val success = profanityFilterService.removeCustomBlockedWordForSchool(
                schoolId = schoolId,
                word = word.trim(),
                adminUserId = adminUserId
            )
            if (success) {
                loadSchoolSafetyData(schoolId)
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Word '$word' removed from school blocked list."
                )
            }
        }
    }

    fun filterAuditLogsByUser(userId: String?) {
        viewModelScope.launch {
            val schoolId = _uiState.value.schoolId
            val filtered = auditLogRepository.filterLogs(
                userId = userId?.takeIf { it.isNotBlank() },
                schoolId = schoolId.takeIf { it.isNotBlank() },
                contentType = _uiState.value.selectedFilterType
            ).sortedByDescending { it.timestamp }

            _uiState.value = _uiState.value.copy(
                selectedUserId = userId,
                auditLogs = filtered
            )
        }
    }

    fun filterAuditLogsByContentType(contentType: FilterContentType?) {
        viewModelScope.launch {
            val schoolId = _uiState.value.schoolId
            val filtered = auditLogRepository.filterLogs(
                userId = _uiState.value.selectedUserId,
                schoolId = schoolId.takeIf { it.isNotBlank() },
                contentType = contentType
            ).sortedByDescending { it.timestamp }

            _uiState.value = _uiState.value.copy(
                selectedFilterType = contentType,
                auditLogs = filtered
            )
        }
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }
}
