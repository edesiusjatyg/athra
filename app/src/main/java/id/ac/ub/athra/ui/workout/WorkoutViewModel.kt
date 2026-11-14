package id.ac.ub.athra.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.ac.ub.athra.data.repository.WorkoutRepository
import id.ac.ub.athra.domain.model.AnalyticsSummary
import id.ac.ub.athra.domain.model.Exercise
import id.ac.ub.athra.domain.model.WorkoutDraft
import id.ac.ub.athra.domain.model.WorkoutSession
import id.ac.ub.athra.domain.model.WeightUnit
import id.ac.ub.athra.domain.model.PersonalRecord
import id.ac.ub.athra.domain.model.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId

data class WorkoutUiState(
    val draft: WorkoutDraft? = null,
    val catalog: List<Exercise> = emptyList(),
    val recentSessions: List<WorkoutSession> = emptyList(),
    val analytics: AnalyticsSummary? = null,
    val personalRecords: List<PersonalRecord> = emptyList(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
)

class WorkoutViewModel(
    private val userId: String,
    private val repository: WorkoutRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(WorkoutUiState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeCatalog().collectLatest { mutableState.value = mutableState.value.copy(catalog = it) }
        }
        viewModelScope.launch {
            repository.observeHistory(userId).collectLatest {
                mutableState.value = mutableState.value.copy(recentSessions = it.take(3))
            }
        }
        refreshDraft()
        refreshAnalytics()
    }

    fun refreshDraft() = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            val draft = repository.getDraft(userId)
            val records = if (draft?.session?.status == SessionStatus.REVIEW) repository.previewRecords(userId, draft) else emptyList()
            draft to records
        }
            .onSuccess { (draft, records) -> mutableState.value = mutableState.value.copy(draft = draft, personalRecords = records, loading = false) }
            .onFailure { fail(it) }
    }

    fun refreshAnalytics(days: Int = 7) = viewModelScope.launch(Dispatchers.IO) {
        runCatching { repository.analytics(userId, days, ZoneId.systemDefault(), System.currentTimeMillis()) }
            .onSuccess { mutableState.value = mutableState.value.copy(analytics = it) }
            .onFailure { fail(it) }
    }

    suspend fun start(name: String? = null): String? = withContext(Dispatchers.IO) {
        runCatching { repository.startOrResume(userId, name) }
            .onSuccess { mutableState.value = mutableState.value.copy(draft = it, error = null) }
            .onFailure { fail(it) }
            .getOrNull()?.session?.id
    }

    suspend fun addExercise(sessionId: String, exerciseId: String) = mutate {
        repository.addExercise(userId, sessionId, exerciseId)
        repository.getDraft(userId)
    }

    suspend fun saveSet(sessionId: String, exerciseId: String, setId: String?, reps: String, weight: String, unit: WeightUnit) = mutate {
        repository.saveSet(userId, sessionId, exerciseId, setId, reps, weight, unit)
    }

    suspend fun deleteSet(sessionId: String, setId: String) = mutate {
        repository.deleteSet(userId, sessionId, setId)
    }

    suspend fun finish(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val draft = repository.enterReview(userId, sessionId, System.currentTimeMillis())
            val records = repository.previewRecords(userId, draft)
            mutableState.value = mutableState.value.copy(draft = draft, personalRecords = records, error = null)
        }.fold(onSuccess = { true }, onFailure = { fail(it); false })
    }

    suspend fun resume(sessionId: String) = mutate { repository.resume(userId, sessionId) }

    suspend fun save(sessionId: String): Boolean = mutateResult {
        repository.finalizeWorkout(userId, sessionId)
        refreshAnalytics()
        null
    }

    suspend fun discard(sessionId: String) = mutate {
        repository.discard(userId, sessionId)
        null
    }

    private suspend fun mutate(block: suspend () -> WorkoutDraft?): Unit = withContext(Dispatchers.IO) {
        runCatching { block() }
            .onSuccess { mutableState.value = mutableState.value.copy(draft = it, error = null) }
            .onFailure { fail(it) }
        Unit
    }

    private suspend fun mutateResult(block: suspend () -> Any?): Boolean = withContext(Dispatchers.IO) {
        runCatching { block() }.fold(
            onSuccess = { mutableState.value = mutableState.value.copy(draft = repository.getDraft(userId), error = null); true },
            onFailure = { fail(it); false },
        )
    }

    private fun fail(error: Throwable) {
        mutableState.value = mutableState.value.copy(loading = false, busy = false, error = error.message ?: "Terjadi kesalahan.")
    }
}

class WorkoutViewModelFactory(
    private val userId: String,
    private val repository: WorkoutRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = WorkoutViewModel(userId, repository) as T
}
