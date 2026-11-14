package id.ac.ub.athra.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.ac.ub.athra.data.repository.WorkoutRepository
import id.ac.ub.athra.domain.model.AnalyticsSummary
import id.ac.ub.athra.domain.model.CompletedWorkout
import id.ac.ub.athra.domain.model.ExerciseReference
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
import kotlinx.coroutines.Job
import java.util.concurrent.atomic.AtomicLong
import java.time.ZoneId

data class WorkoutUiState(
    val profileName: String = "",
    val profileEmail: String = "",
    val draft: WorkoutDraft? = null,
    val catalog: List<Exercise> = emptyList(),
    val recentSessions: List<WorkoutSession> = emptyList(),
    val homeAnalytics: AnalyticsSummary? = null,
    val analytics: AnalyticsSummary? = null,
    val personalRecords: List<PersonalRecord> = emptyList(),
    val exerciseReferences: Map<String, ExerciseReference> = emptyMap(),
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
    private val analyticsRequest = AtomicLong()
    private var analyticsJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeCatalog().collectLatest { exercises ->
                val references = withContext(Dispatchers.IO) { referenceMap(exercises) }
                mutableState.value = mutableState.value.copy(catalog = exercises, exerciseReferences = references)
            }
        }
        viewModelScope.launch {
            repository.observeHistory(userId).collectLatest {
                mutableState.value = mutableState.value.copy(recentSessions = it)
            }
        }
        refreshDraft()
        refreshHomeAnalytics()
        refreshProfile()
    }

    fun refreshDraft() = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            val draft = repository.getDraft(userId)
            val records = if (draft?.session?.status == SessionStatus.REVIEW) repository.previewRecords(userId, draft) else emptyList()
            val references = referenceMap(draft?.exercises?.map { it.exercise }.orEmpty())
            Triple(draft, records, references)
        }
            .onSuccess { (draft, records, references) -> mutableState.value = mutableState.value.copy(draft = draft, personalRecords = records, exerciseReferences = references, loading = false) }
            .onFailure { fail(it) }
    }

    fun refreshAnalytics(days: Int = 7) {
        val requestId = analyticsRequest.incrementAndGet()
        analyticsJob?.cancel()
        analyticsJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.analytics(userId, days, ZoneId.systemDefault(), System.currentTimeMillis()) }
                .onSuccess { if (analyticsRequest.get() == requestId) mutableState.value = mutableState.value.copy(analytics = it) }
                .onFailure { if (analyticsRequest.get() == requestId) fail(it) }
        }
    }

    fun refreshHomeAnalytics() = viewModelScope.launch(Dispatchers.IO) {
        runCatching { repository.analytics(userId, 7, ZoneId.systemDefault(), System.currentTimeMillis(), calendarWeek = true) }
            .onSuccess { mutableState.value = mutableState.value.copy(homeAnalytics = it) }
            .onFailure { fail(it) }
    }

    fun refreshProfile() = viewModelScope.launch(Dispatchers.IO) {
        runCatching { repository.getProfile(userId) }
            .onSuccess { profile ->
                if (profile != null) mutableState.value = mutableState.value.copy(profileName = profile.displayName, profileEmail = profile.email)
            }
            .onFailure { fail(it) }
    }

    fun refreshReferences() = viewModelScope.launch(Dispatchers.IO) {
        runCatching { referenceMap(mutableState.value.catalog) }
            .onSuccess { mutableState.value = mutableState.value.copy(exerciseReferences = it) }
            .onFailure { fail(it) }
    }

    suspend fun updateProfileName(name: String): Boolean = withContext(Dispatchers.IO) {
        runCatching { repository.updateProfileName(userId, name); repository.getProfile(userId) }
            .fold(
                onSuccess = { profile ->
                    if (profile != null) mutableState.value = mutableState.value.copy(profileName = profile.displayName, profileEmail = profile.email, error = null)
                    true
                },
                onFailure = { fail(it); false },
            )
    }

    suspend fun loadWorkoutDetail(sessionId: String): CompletedWorkout? = withContext(Dispatchers.IO) {
        repository.getWorkoutDetail(userId, sessionId)
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

    suspend fun removeExercise(sessionId: String, exerciseId: String) = mutate {
        repository.removeExercise(userId, sessionId, exerciseId)
        repository.getDraft(userId)
    }

    suspend fun saveSet(sessionId: String, exerciseId: String, setId: String?, reps: String, weight: String, unit: WeightUnit): Boolean = mutateResult {
        repository.saveSet(userId, sessionId, exerciseId, setId, reps, weight, unit)
        null
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
        refreshHomeAnalytics()
        refreshAnalytics()
        refreshReferences()
        null
    }

    suspend fun discard(sessionId: String) = mutate {
        repository.discard(userId, sessionId)
        null
    }

    private suspend fun mutate(block: suspend () -> WorkoutDraft?): Unit = withContext(Dispatchers.IO) {
        runCatching { block() }
            .onSuccess { draft -> mutableState.value = mutableState.value.copy(draft = draft, error = null) }
            .onFailure { fail(it) }
        Unit
    }

    private suspend fun mutateResult(block: suspend () -> Any?): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val result = block()
            val draft = repository.getDraft(userId)
            result to draft
        }.fold(
            onSuccess = { (_, draft) -> mutableState.value = mutableState.value.copy(draft = draft, error = null); true },
            onFailure = { fail(it); false },
        )
    }

    private fun fail(error: Throwable) {
        mutableState.value = mutableState.value.copy(loading = false, busy = false, error = error.message ?: "Terjadi kesalahan.")
    }

    private suspend fun referenceMap(draftExercises: List<Exercise>): Map<String, id.ac.ub.athra.domain.model.ExerciseReference> {
        val knownIds = (mutableState.value.catalog + draftExercises).map { it.id }.toSet()
        return repository.latestReferences(userId).filterKeys { it in knownIds }
    }
}

class WorkoutViewModelFactory(
    private val userId: String,
    private val repository: WorkoutRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = WorkoutViewModel(userId, repository) as T
}
