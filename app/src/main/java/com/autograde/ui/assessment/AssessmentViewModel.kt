package com.autograde.ui.assessment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autograde.data.FileRepository
import com.autograde.util.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

const val DEFAULT_QUESTIONS = 20
const val MIN_QUESTIONS = 1
const val MAX_QUESTIONS = 100

/** Answer choices on the bubble sheet. */
val OMR_OPTIONS = listOf("A", "B", "C", "D")

private fun dateFormat() = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

fun todayLabel(): String = dateFormat().format(Date())

/** Formats a UTC-midnight date from the Material date picker. */
fun utcDateLabel(millis: Long): String =
    dateFormat().apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(millis))

/** Lifecycle of one file the teacher picked, from tap to stored-on-server. */
enum class UploadState { IDLE, UPLOADING, DONE, ERROR }

data class UploadedFile(
    val localName: String,
    val displaySize: String,
    val state: UploadState = UploadState.UPLOADING,
    val serverId: Int? = null,
    val serverPath: String? = null,
    val error: String? = null,
    /** Stable row id within this flow (list positions shift as rows are removed). */
    val id: Long = 0L,
)

data class AssessmentUiState(
    // --- form ---
    val title: String = "",
    val subject: String = "Operating System",
    val className: String = "BSc 3rd Year",
    val examDate: String = todayLabel(),
    val totalMarks: String = "100",
    val type: String = "OMR",
    // --- files ---
    val submissions: List<UploadedFile> = emptyList(),
    // --- answer key: one slot per question, null = not set yet ---
    val answerKey: List<String?> = List(DEFAULT_QUESTIONS) { null },
    val message: String? = null,
) {
    /** Step 1 is complete once there's a title and a positive mark total. */
    val detailsValid: Boolean
        get() = title.isNotBlank() && (totalMarks.toIntOrNull() ?: 0) > 0

    /** Anything the teacher would lose by leaving the flow. */
    val hasProgress: Boolean
        get() = title.isNotBlank() || submissions.isNotEmpty() || answerKey.any { it != null }

    val questionCount: Int get() = answerKey.size
    val answeredCount: Int get() = answerKey.count { it != null }
    val answerKeyComplete: Boolean get() = answerKey.isNotEmpty() && answerKey.all { it != null }

    /** Sheets stored on the server and ready to grade. */
    val sheetCount: Int get() = uploadedSubmissions

    /** How many submissions are safely stored server-side. */
    val uploadedSubmissions: Int
        get() = submissions.count { it.state == UploadState.DONE }

    val uploadsInFlight: Boolean
        get() = submissions.any { it.state == UploadState.UPLOADING }
}

/**
 * Owns the OMR create-assessment flow: form fields and student submission uploads.
 *
 * Uploads go to the real backend via [FileRepository]; OMR key setup and scoring
 * are handled in the OMR screens.
 */
class AssessmentViewModel(
    private val fileRepo: FileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AssessmentUiState())
    private var nextId = 0L
    val state: StateFlow<AssessmentUiState> = _state

    fun consumeMessage() = _state.update { it.copy(message = null) }

    // --- form fields ---

    fun onTitle(v: String) = _state.update { it.copy(title = v) }
    fun onSubject(v: String) = _state.update { it.copy(subject = v) }
    fun onClass(v: String) = _state.update { it.copy(className = v) }
    fun onDate(v: String) = _state.update { it.copy(examDate = v) }
    fun onMarks(v: String) = _state.update { it.copy(totalMarks = v.filter(Char::isDigit)) }
    fun onType(v: String) = _state.update { it.copy(type = v) }

    // --- answer key ---

    /** Grow or shrink the key, keeping answers already set for surviving questions. */
    fun setQuestionCount(count: Int) = _state.update { s ->
        val n = count.coerceIn(MIN_QUESTIONS, MAX_QUESTIONS)
        s.copy(answerKey = List(n) { i -> s.answerKey.getOrNull(i) })
    }

    /** Set question [index]'s answer; tapping the current answer clears it. */
    fun setAnswer(index: Int, option: String) = _state.update { s ->
        if (index !in s.answerKey.indices) return@update s
        val list = s.answerKey.toMutableList()
        list[index] = if (list[index] == option) null else option
        s.copy(answerKey = list)
    }

    // --- uploads ---

    /** Student answer sheets picked on the collect-submissions screen. */
    fun uploadSubmissions(files: List<File>) {
        files.forEach { file ->
            val pending = UploadedFile(
                localName = file.name,
                displaySize = com.autograde.util.humanSize(file.length()),
                id = nextId++,
            )
            _state.update { it.copy(submissions = it.submissions + pending) }
            viewModelScope.launch {
                val done = when (val result = fileRepo.upload(file)) {
                    is ApiResult.Ok -> pending.copy(
                        state = UploadState.DONE,
                        serverId = result.value.id,
                        serverPath = result.value.filePath,
                    )
                    is ApiResult.Err -> pending.copy(state = UploadState.ERROR, error = result.message)
                    ApiResult.Loading -> pending
                }
                // Match by id, not position: rows may have been removed meanwhile.
                val stillListed = _state.value.submissions.any { it.id == pending.id }
                if (!stillListed) {
                    // Removed while uploading — don't leave an orphan on the server.
                    done.serverId?.let { fileRepo.delete(it) }
                    return@launch
                }
                _state.update { s ->
                    s.copy(submissions = s.submissions.map { if (it.id == pending.id) done else it })
                }
                if (done.state == UploadState.ERROR) {
                    _state.update { it.copy(message = "Upload failed: ${done.error}") }
                }
            }
        }
    }

    fun removeSubmission(id: Long) {
        val target = _state.value.submissions.firstOrNull { it.id == id } ?: return
        _state.update { s -> s.copy(submissions = s.submissions.filterNot { it.id == id }) }
        target.serverId?.let { serverId -> viewModelScope.launch { fileRepo.delete(serverId) } }
    }

    fun reset() {
        _state.value = AssessmentUiState()
    }
}
