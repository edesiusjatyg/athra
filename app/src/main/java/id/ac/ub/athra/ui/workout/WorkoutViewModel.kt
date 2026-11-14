package id.ac.ub.athra.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
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
import id.ac.ub.athra.domain.time.SystemWorkoutClock
import id.ac.ub.athra.domain.time.WorkoutClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.atomic.AtomicLong
import java.time.ZoneId

data class WorkoutUiState(
    val profileName: String = "",
    val profileEmail: String = "",
    val profileLoading: Boolean = true,
    val profileError: String? = null,
    val draft: WorkoutDraft? = null,
    val catalog: List<Exercise> = emptyList(),
    val catalogLoading: Boolean = true,
    val catalogError: String? = null,
    val recentSessions: List<WorkoutSession> = emptyList(),
    val historyLoading: Boolean = true,
    val historyError: String? = null,
    val draftLoading: Boolean = true,
    val draftError: String? = null,
    val homeAnalytics: AnalyticsSummary? = null,
    val homeAnalyticsLoading: Boolean = true,
    val homeAnalyticsError: String? = null,
    val analytics: AnalyticsSummary? = null,
    val analyticsLoading: Boolean = false,
    val analyticsError: String? = null,
    val personalRecords: List<PersonalRecord> = emptyList(),
    val exerciseReferences: Map<String, ExerciseReference> = emptyMap(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
    val quickStartName: String = "",
    val selectedExerciseId: String = "",
    val repsInput: String = "",
    val weightInput: String = "",
    val editingSetId: String? = null,
    val editingOriginalDisplay: String? = null,
    val editingOriginalKg: String? = null,
    val librarySearch: String = "",
    val libraryFilter: String = "Semua",
    val pendingLibraryFilter: String = "Semua",
    val selectedWorkoutDetail: CompletedWorkout? = null,
    val workoutDetailLoading: Boolean = false,
    val workoutDetailError: String? = null,
)

class WorkoutViewModel(
    private val userId: String,
    private val repository: WorkoutRepository,
    private val clock: WorkoutClock = SystemWorkoutClock,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        WorkoutUiState(
            quickStartName = savedStateHandle["quickStartName"] ?: "",
            selectedExerciseId = savedStateHandle["selectedExerciseId"] ?: "",
            repsInput = savedStateHandle["repsInput"] ?: "",
            weightInput = savedStateHandle["weightInput"] ?: "",
            editingSetId = savedStateHandle["editingSetId"],
            editingOriginalDisplay = savedStateHandle["editingOriginalDisplay"],
            editingOriginalKg = savedStateHandle["editingOriginalKg"],
            librarySearch = savedStateHandle["librarySearch"] ?: "",
            libraryFilter = savedStateHandle["libraryFilter"] ?: "Semua",
            pendingLibraryFilter = savedStateHandle["pendingLibraryFilter"] ?: "Semua",
        ),
    )
    val state = mutableState.asStateFlow()
    private val analyticsRequest = AtomicLong()
    private var analyticsJob: Job? = null
    private val detailRequest = AtomicLong()
    private var catalogJob: Job? = null
    private var historyJob: Job? = null
    private val mutationMutex = Mutex()

    init {
        observeCatalog()
        refreshHistory()
        refreshDraft()
        refreshHomeAnalytics()
        refreshProfile()
    }

    private fun observeCatalog() {
        catalogJob?.cancel()
        catalogJob = viewModelScope.launch {
            repository.observeCatalog()
                .catch { error -> mutableState.value = mutableState.value.copy(catalogLoading = false, catalogError = error.message ?: "Katalog gagal dimuat.") }
                .collectLatest { exercises ->
                    capture { withContext(Dispatchers.IO) { referenceMap(exercises) } }
                        .onSuccess { references ->
                            mutableState.value = mutableState.value.copy(
                                catalog = exercises,
                                exerciseReferences = references,
                                catalogLoading = false,
                                catalogError = null,
                            )
                        }
                        .onFailure { error ->
                            mutableState.value = mutableState.value.copy(
                                catalogLoading = false,
                                catalogError = error.message ?: "Referensi gerakan gagal dimuat.",
                            )
                        }
                }
        }
    }

    fun retryCatalog() = viewModelScope.launch(Dispatchers.IO) {
        mutableState.value = mutableState.value.copy(catalogLoading = true, catalogError = null)
        capture { repository.seedCatalog() }
            .onSuccess { observeCatalog() }
            .onFailure {
                mutableState.value = mutableState.value.copy(
                    catalogLoading = false,
                    catalogError = it.message ?: "Katalog gagal dimuat.",
                )
            }
    }

    fun refreshHistory() {
        historyJob?.cancel()
        mutableState.value = mutableState.value.copy(historyLoading = true, historyError = null)
        historyJob = viewModelScope.launch {
            repository.observeHistory(userId)
                .catch { error -> mutableState.value = mutableState.value.copy(historyLoading = false, historyError = error.message ?: "Riwayat gagal dimuat.") }
                .collectLatest { sessions ->
                    mutableState.value = mutableState.value.copy(
                        recentSessions = sessions,
                        historyLoading = false,
                        historyError = null,
                    )
                }
        }
    }

    fun refreshDraft() = viewModelScope.launch(Dispatchers.IO) {
        mutableState.value = mutableState.value.copy(loading = true, error = null, draftLoading = true, draftError = null)
        capture {
            val draft = repository.getDraft(userId)
            val records = if (draft?.session?.status == SessionStatus.REVIEW) repository.previewRecords(userId, draft) else emptyList()
            val references = referenceMap(draft?.exercises?.map { it.exercise }.orEmpty())
            Triple(draft, records, references)
        }
            .onSuccess { (draft, records, references) ->
                val state = mutableState.value
                val selected = state.selectedExerciseId.takeIf { id -> draft?.exercises?.any { it.exercise.id == id } == true }
                    ?: draft?.exercises?.firstOrNull()?.exercise?.id.orEmpty()
                savedStateHandle["selectedExerciseId"] = selected
                mutableState.value = state.copy(
                    draft = draft,
                    personalRecords = records,
                    exerciseReferences = references,
                    selectedExerciseId = selected,
                    loading = false,
                    draftLoading = false,
                    draftError = null,
                    error = null,
                )
            }
            .onFailure {
                mutableState.value = mutableState.value.copy(
                    draftLoading = false,
                    draftError = it.message ?: "Draft latihan gagal dimuat.",
                )
                fail(it)
            }
    }

    fun refreshAnalytics(days: Int = 30) {
        val requestId = analyticsRequest.incrementAndGet()
        analyticsJob?.cancel()
        mutableState.value = mutableState.value.copy(analyticsLoading = true, analyticsError = null)
        analyticsJob = viewModelScope.launch(Dispatchers.IO) {
            capture { repository.analytics(userId, days, ZoneId.systemDefault(), clock.nowMillis()) }
                .onSuccess {
                    if (analyticsRequest.get() == requestId) mutableState.value = mutableState.value.copy(
                        analytics = it,
                        analyticsLoading = false,
                        analyticsError = null,
                    )
                }
                .onFailure {
                    if (analyticsRequest.get() == requestId) {
                        mutableState.value = mutableState.value.copy(
                            analyticsLoading = false,
                            analyticsError = it.message ?: "Analytics gagal dimuat.",
                        )
                        fail(it)
                    }
                }
        }
    }

    fun refreshHomeAnalytics() = viewModelScope.launch(Dispatchers.IO) {
        mutableState.value = mutableState.value.copy(homeAnalyticsLoading = true, homeAnalyticsError = null)
        capture { repository.analytics(userId, 7, ZoneId.systemDefault(), clock.nowMillis(), calendarWeek = true) }
            .onSuccess { mutableState.value = mutableState.value.copy(homeAnalytics = it, homeAnalyticsLoading = false, homeAnalyticsError = null) }
            .onFailure {
                mutableState.value = mutableState.value.copy(homeAnalyticsLoading = false, homeAnalyticsError = it.message ?: "Ringkasan Home gagal dimuat.")
                fail(it)
            }
    }

    fun refreshProfile() = viewModelScope.launch(Dispatchers.IO) {
        mutableState.value = mutableState.value.copy(profileLoading = true, profileError = null)
        capture { repository.getProfile(userId) }
            .onSuccess { profile ->
                if (profile != null) mutableState.value = mutableState.value.copy(
                    profileName = profile.displayName,
                    profileEmail = profile.email,
                    profileLoading = false,
                    profileError = null,
                ) else mutableState.value = mutableState.value.copy(profileLoading = false)
            }
            .onFailure {
                mutableState.value = mutableState.value.copy(profileLoading = false, profileError = it.message ?: "Profil gagal dimuat.")
                fail(it)
            }
    }

    fun refreshReferences() = viewModelScope.launch(Dispatchers.IO) {
        capture { referenceMap(mutableState.value.catalog) }
            .onSuccess { mutableState.value = mutableState.value.copy(exerciseReferences = it) }
            .onFailure { fail(it) }
    }

    fun updateQuickStartName(value: String) = updateSaved("quickStartName", value) { copy(quickStartName = value) }
    fun updateLibrarySearch(value: String) = updateSaved("librarySearch", value) { copy(librarySearch = value) }
    fun updatePendingLibraryFilter(value: String) = updateSaved("pendingLibraryFilter", value) { copy(pendingLibraryFilter = value) }
    fun applyLibraryFilter() = updateSaved("libraryFilter", mutableState.value.pendingLibraryFilter) {
        copy(libraryFilter = pendingLibraryFilter)
    }

    fun resetLibraryFilter() {
        updateSaved("pendingLibraryFilter", "Semua") { copy(pendingLibraryFilter = "Semua") }
        updateSaved("libraryFilter", "Semua") { copy(libraryFilter = "Semua") }
    }

    fun selectExercise(exerciseId: String) {
        savedStateHandle["selectedExerciseId"] = exerciseId
        clearSetForm()
        mutableState.value = mutableState.value.copy(selectedExerciseId = exerciseId)
    }

    fun updateSetInputs(reps: String, weight: String) {
        savedStateHandle["repsInput"] = reps
        savedStateHandle["weightInput"] = weight
        mutableState.value = mutableState.value.copy(repsInput = reps, weightInput = weight)
    }

    fun editSetInputs(setId: String, reps: String, displayWeight: String, originalKg: String) {
        savedStateHandle["editingSetId"] = setId
        savedStateHandle["repsInput"] = reps
        savedStateHandle["weightInput"] = displayWeight
        savedStateHandle["editingOriginalDisplay"] = displayWeight
        savedStateHandle["editingOriginalKg"] = originalKg
        mutableState.value = mutableState.value.copy(
            editingSetId = setId,
            repsInput = reps,
            weightInput = displayWeight,
            editingOriginalDisplay = displayWeight,
            editingOriginalKg = originalKg,
        )
    }

    fun clearSetForm() {
        listOf("editingSetId", "editingOriginalDisplay", "editingOriginalKg").forEach { savedStateHandle.remove<String>(it) }
        savedStateHandle["repsInput"] = ""
        savedStateHandle["weightInput"] = ""
        mutableState.value = mutableState.value.copy(
            repsInput = "",
            weightInput = "",
            editingSetId = null,
            editingOriginalDisplay = null,
            editingOriginalKg = null,
        )
    }

    private fun updateSaved(key: String, value: String, update: WorkoutUiState.() -> WorkoutUiState) {
        savedStateHandle[key] = value
        mutableState.value = mutableState.value.update()
    }

    suspend fun updateProfileName(name: String): Boolean = withContext(Dispatchers.IO) {
        if (!beginMutation()) return@withContext false
        try {
            capture { repository.updateProfileName(userId, name); repository.getProfile(userId) }
                .fold(
                    onSuccess = { profile ->
                        if (profile != null) mutableState.value = mutableState.value.copy(
                            profileName = profile.displayName,
                            profileEmail = profile.email,
                            profileLoading = false,
                            profileError = null,
                            error = null,
                        )
                        true
                    },
                    onFailure = {
                        mutableState.value = mutableState.value.copy(profileLoading = false, profileError = it.message ?: "Nama profil gagal disimpan.")
                        fail(it)
                        false
                    },
                )
        } finally {
            endMutation()
        }
    }

    suspend fun loadWorkoutDetail(sessionId: String): CompletedWorkout? {
        val requestId = detailRequest.incrementAndGet()
        mutableState.value = mutableState.value.copy(
            selectedWorkoutDetail = null,
            workoutDetailLoading = true,
            workoutDetailError = null,
        )
        return withContext(Dispatchers.IO) {
            try {
                val detail = repository.getWorkoutDetail(userId, sessionId)
                if (detailRequest.get() == requestId) {
                    mutableState.value = mutableState.value.copy(
                        selectedWorkoutDetail = detail,
                        workoutDetailLoading = false,
                        workoutDetailError = null,
                    )
                }
                detail
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (detailRequest.get() == requestId) {
                    mutableState.value = mutableState.value.copy(
                        workoutDetailLoading = false,
                        workoutDetailError = error.message ?: "Detail sesi gagal dimuat.",
                    )
                }
                null
            }
        }
    }

    suspend fun start(name: String? = null): String? = withContext(Dispatchers.IO) {
        if (!beginMutation()) return@withContext null
        try {
            capture { repository.startOrResume(userId, name, clock.nowMillis()) }
                .onSuccess {
                    val selected = mutableState.value.selectedExerciseId.takeIf { id -> it.exercises.any { e -> e.exercise.id == id } }
                        ?: it.exercises.firstOrNull()?.exercise?.id.orEmpty()
                    savedStateHandle["selectedExerciseId"] = selected
                    mutableState.value = mutableState.value.copy(draft = it, selectedExerciseId = selected, error = null)
                }
                .onFailure { fail(it) }
                .getOrNull()?.session?.id
        } finally {
            endMutation()
        }
    }

    suspend fun addExercise(sessionId: String, exerciseId: String) {
        addExerciseResult(sessionId, exerciseId)
    }

    suspend fun addExerciseResult(sessionId: String, exerciseId: String): Boolean = mutateResult {
        repository.addExercise(userId, sessionId, exerciseId)
        null
    }

    suspend fun removeExercise(sessionId: String, exerciseId: String) = mutate {
        repository.removeExercise(userId, sessionId, exerciseId)
        repository.getDraft(userId)
    }

    suspend fun saveSet(sessionId: String, exerciseId: String, setId: String?, reps: String, weight: String, unit: WeightUnit): Boolean {
        val saved = mutateResult {
            repository.saveSet(userId, sessionId, exerciseId, setId, reps, weight, unit)
            null
        }
        if (saved) clearSetForm()
        return saved
    }

    suspend fun deleteSet(sessionId: String, setId: String) = mutate {
        repository.deleteSet(userId, sessionId, setId)
    }

    suspend fun finish(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        if (!beginMutation()) return@withContext false
        try {
            capture {
                val draft = repository.enterReview(userId, sessionId, clock.nowMillis())
                val records = repository.previewRecords(userId, draft)
                mutableState.value = mutableState.value.copy(draft = draft, personalRecords = records, error = null)
            }.fold(onSuccess = { true }, onFailure = { fail(it); false })
        } finally {
            endMutation()
        }
    }

    suspend fun resume(sessionId: String) {
        resumeResult(sessionId)
    }

    suspend fun resumeResult(sessionId: String): Boolean = mutateResult {
        repository.resume(userId, sessionId)
        null
    }

    suspend fun save(sessionId: String): Boolean = mutateResult {
        repository.finalizeWorkout(userId, sessionId)
        refreshHomeAnalytics()
        refreshAnalytics()
        refreshReferences()
        null
    }

    suspend fun discard(sessionId: String) {
        discardResult(sessionId)
    }

    suspend fun discardResult(sessionId: String): Boolean = mutateResult {
        repository.discard(userId, sessionId)
        null
    }

    private suspend fun mutate(block: suspend () -> WorkoutDraft?): Unit = withContext(Dispatchers.IO) {
        if (!beginMutation()) return@withContext Unit
        try {
            capture { block() }
                .onSuccess { draft -> mutableState.value = mutableState.value.copy(draft = draft, error = null) }
                .onFailure { fail(it) }
        } finally {
            endMutation()
        }
    }

    private suspend fun mutateResult(block: suspend () -> Any?): Boolean = withContext(Dispatchers.IO) {
        if (!beginMutation()) return@withContext false
        try {
            capture {
                val result = block()
                val draft = repository.getDraft(userId)
                result to draft
            }.fold(
                onSuccess = { (_, draft) -> mutableState.value = mutableState.value.copy(draft = draft, error = null); true },
                onFailure = { fail(it); false },
            )
        } finally {
            endMutation()
        }
    }

    private fun beginMutation(): Boolean {
        if (!mutationMutex.tryLock()) return false
        mutableState.value = mutableState.value.copy(busy = true, error = null)
        return true
    }

    private fun endMutation() {
        mutableState.value = mutableState.value.copy(busy = false)
        mutationMutex.unlock()
    }

    private fun fail(error: Throwable) {
        mutableState.value = mutableState.value.copy(loading = false, error = error.message ?: "Terjadi kesalahan.")
    }

    private suspend fun referenceMap(draftExercises: List<Exercise>): Map<String, id.ac.ub.athra.domain.model.ExerciseReference> {
        val knownIds = (mutableState.value.catalog + draftExercises).map { it.id }.toSet()
        return repository.latestReferences(userId).filterKeys { it in knownIds }
    }
}

private suspend fun <T> capture(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Result.failure(error)
}

class WorkoutViewModelFactory(
    private val userId: String,
    private val repository: WorkoutRepository,
    private val savedStateEnabled: Boolean = false,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        WorkoutViewModel(userId, repository, savedStateHandle = SavedStateHandle()) as T

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        WorkoutViewModel(
            userId,
            repository,
            savedStateHandle = if (savedStateEnabled) extras.createSavedStateHandle() else SavedStateHandle(),
        ) as T
}
