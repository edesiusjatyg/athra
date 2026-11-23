package id.ac.ub.athra.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import id.ac.ub.athra.domain.calculation.calculateTotals
import id.ac.ub.athra.domain.calculation.displayNumber
import id.ac.ub.athra.domain.calculation.displayWeight
import id.ac.ub.athra.domain.model.SessionStatus
import id.ac.ub.athra.domain.model.PersonalRecordStatus
import id.ac.ub.athra.domain.model.WeightUnit
import id.ac.ub.athra.domain.model.WorkoutDraft
import id.ac.ub.athra.domain.time.SystemWorkoutClock
import id.ac.ub.athra.domain.time.WorkoutClock
import id.ac.ub.athra.domain.time.elapsedSeconds
import id.ac.ub.athra.ui.components.AthraCard
import id.ac.ub.athra.ui.components.AthraChip
import id.ac.ub.athra.ui.theme.Background
import id.ac.ub.athra.ui.theme.Forest
import id.ac.ub.athra.ui.theme.Ink
import id.ac.ub.athra.ui.theme.Secondary
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun WorkoutFlowScreen(
    draft: WorkoutDraft,
    state: WorkoutUiState,
    onSaveSet: suspend (String, String, String?, String, String, WeightUnit) -> Boolean,
    onDeleteSet: suspend (String, String) -> Unit,
    onAddExercise: suspend (String, String) -> Unit,
    onRemoveExercise: suspend (String, String) -> Unit,
    onFinish: suspend (String) -> Boolean,
    onResume: suspend (String) -> Unit,
    onSave: suspend (String) -> Boolean,
    onDiscard: suspend (String) -> Unit,
    onExit: () -> Unit,
    onDiscardResult: suspend (String) -> Boolean = { id -> onDiscard(id); true },
    onOpenLibrary: () -> Unit = {},
    onOpenExerciseDetails: (String) -> Unit = {},
    onUpdateSetInputs: (String, String) -> Unit = { _, _ -> },
    onSelectExercise: (String) -> Unit = {},
    onEditSetInputs: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onClearSetForm: () -> Unit = {},
    weightUnit: WeightUnit = WeightUnit.KG,
    clock: WorkoutClock = SystemWorkoutClock,
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val largeFont = density.fontScale >= 1.5f
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    var exerciseId by rememberSaveable(draft.session.id) {
        mutableStateOf(state.selectedExerciseId.ifBlank { draft.exercises.firstOrNull()?.exercise?.id.orEmpty() })
    }
    var reps by rememberSaveable(draft.session.id) { mutableStateOf(state.repsInput) }
    var weight by rememberSaveable(draft.session.id) { mutableStateOf(state.weightInput) }
    var editingSetId by rememberSaveable(draft.session.id) { mutableStateOf<String?>(null) }
    var editingOriginalDisplay by rememberSaveable { mutableStateOf<String?>(null) }
    var editingOriginalKg by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingRemove by remember { mutableStateOf<id.ac.ub.athra.domain.model.WorkoutExercise?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmLeaveWorkout by remember { mutableStateOf(false) }
    var confirmUnsavedFinish by remember { mutableStateOf(false) }
    var savingSet by remember(draft.session.id) { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(clock.nowMillis()) }
    LaunchedEffect(draft.session.startedAt, draft.session.status) {
        while (draft.session.status == SessionStatus.ACTIVE) {
            now = clock.nowMillis()
            kotlinx.coroutines.delay(1000)
        }
    }
    LaunchedEffect(state.selectedExerciseId) {
        if (state.selectedExerciseId.isNotBlank()) exerciseId = state.selectedExerciseId
    }
    LaunchedEffect(state.repsInput, state.weightInput) {
        reps = state.repsInput
        weight = state.weightInput
    }
    LaunchedEffect(state.editingSetId, state.editingOriginalDisplay, state.editingOriginalKg) {
        editingSetId = state.editingSetId
        editingOriginalDisplay = state.editingOriginalDisplay
        editingOriginalKg = state.editingOriginalKg
    }
    val totals = calculateTotals(draft)
    val elapsed = elapsedSeconds(draft.session.startedAt, draft.session.endedAt ?: now)
    val timer = "%02d:%02d:%02d".format(elapsed / 3600, elapsed % 3600 / 60, elapsed % 60)
    val selected = draft.exercises.firstOrNull { it.exercise.id == exerciseId }
    val selectedExerciseId = selected?.exercise?.id
    val selectedIndex = draft.exercises.indexOfFirst { it.exercise.id == exerciseId }
    val nextExercise = if (selectedIndex >= 0) draft.exercises.getOrNull(selectedIndex + 1) else null

    fun saveCurrentSet() {
        val exerciseIdToSave = selectedExerciseId ?: return
        if (savingSet || state.busy) return
        savingSet = true
        scope.launch {
            try {
                val unchangedWeight = editingSetId != null && weight == editingOriginalDisplay
                val saved = onSaveSet(
                    draft.session.id,
                    exerciseIdToSave,
                    editingSetId,
                    reps,
                    if (unchangedWeight) editingOriginalKg.orEmpty() else weight,
                    if (unchangedWeight) WeightUnit.KG else weightUnit,
                )
                if (saved) {
                    reps = ""; weight = ""; editingSetId = null; editingOriginalDisplay = null; editingOriginalKg = null
                    onClearSetForm()
                }
            } finally {
                savingSet = false
            }
        }
    }

    Scaffold(containerColor = Background, topBar = {
        TopAppBar(title = { Text(if (draft.session.status == SessionStatus.REVIEW) "Ringkasan" else "Workout aktif") },
            navigationIcon = { TextButton(onClick = { confirmLeaveWorkout = true }) { Text("Kembali") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background))
    }, bottomBar = {
        if (draft.session.status == SessionStatus.ACTIVE) {
            Column(
                modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (imeVisible && selectedExerciseId != null) {
                    Button(
                        onClick = ::saveCurrentSet,
                        enabled = !state.busy && !savingSet,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (savingSet) "Menyimpan…" else "Simpan set") }
                } else {
                    Button(
                        onClick = {
                            if (reps.isNotBlank() || weight.isNotBlank()) confirmUnsavedFinish = true
                            else scope.launch { onFinish(draft.session.id) }
                        },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Selesai dan tinjau") }
                }
            }
        }
    }) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).imePadding().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text(draft.session.name, style = MaterialTheme.typography.headlineMedium, color = Ink)
            val muscleGroups = draft.exercises.map { it.exercise.primaryMuscleGroup }.distinct().joinToString(" · ")
            if (muscleGroups.isNotBlank()) {
                Text(muscleGroups.uppercase(), style = MaterialTheme.typography.labelSmall, color = Secondary)
            }
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Forest),
            ) {
                if (largeFont) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("DURASI BERJALAN", style = MaterialTheme.typography.labelSmall, color = Secondary)
                        Text(timer, style = MaterialTheme.typography.headlineMedium, color = Ink)
                        Text("${totals.setCount} set tersimpan", style = MaterialTheme.typography.titleMedium, color = Ink)
                    }
                } else Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Column {
                        Text("DURASI BERJALAN", style = MaterialTheme.typography.labelSmall, color = Secondary)
                        Text(timer, style = MaterialTheme.typography.headlineMedium, color = Ink)
                    }
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                        Text("SET TERSIMPAN", style = MaterialTheme.typography.labelSmall, color = Secondary)
                        Text(totals.setCount.toString(), style = MaterialTheme.typography.headlineMedium, color = id.ac.ub.athra.ui.theme.Accent)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${totals.repCount} repetisi · ${displayNumber(displayWeight(totals.volumeKg, weightUnit))} ${weightUnit.name.lowercase()} volume",
                color = Secondary,
            )
            Spacer(Modifier.height(20.dp))

            if (draft.session.status == SessionStatus.REVIEW) {
                Text("${totals.exerciseCount} gerakan", style = MaterialTheme.typography.titleMedium)
                draft.exercises.filter { it.sets.isNotEmpty() }.forEach { item ->
                    val record = state.personalRecords.firstOrNull { it.exerciseId == item.exercise.id }
                    val status = when (record?.status) {
                        PersonalRecordStatus.BASELINE -> "Catatan awal"
                        PersonalRecordStatus.NEW_RECORD -> "Rekor baru"
                        PersonalRecordStatus.MATCHED -> "Menyamai rekor"
                        PersonalRecordStatus.BELOW_RECORD -> "Belum melampaui rekor"
                        null -> ""
                    }
                    Text(
                        "${item.exercise.name}: ${displayNumber(displayWeight(record?.currentMaxKg ?: item.sets.maxOf { it.weightKg }, weightUnit))} ${weightUnit.name.lowercase()} · $status" +
                            (record?.previousMaxKg?.let { " · sebelumnya ${displayNumber(displayWeight(it, weightUnit))} ${weightUnit.name.lowercase()}" } ?: ""),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(24.dp))
                Button(onClick = { scope.launch { if (onSave(draft.session.id)) onExit() } }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Simpan sesi") }
                TextButton(onClick = { scope.launch { onResume(draft.session.id) } }, enabled = !state.busy) { Text("Kembali memperbaiki") }
                TextButton(onClick = { confirmDiscard = true }, enabled = !state.busy) { Text("Buang sesi") }
            } else {
                Text("Gerakan aktif", style = MaterialTheme.typography.headlineSmall, color = Ink)
                Spacer(Modifier.height(8.dp))
                draft.exercises.forEach { item ->
                    AthraCard(Modifier.padding(bottom = 12.dp)) {
                    val selectExercise = {
                        exerciseId = item.exercise.id
                        onSelectExercise(item.exercise.id)
                        editingSetId = null
                        editingOriginalDisplay = null
                        editingOriginalKg = null
                        reps = ""
                        weight = ""
                        onClearSetForm()
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Text(
                            item.exercise.primaryMuscleGroup.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Secondary,
                        )
                        AthraChip(label = item.exercise.equipment.uppercase())
                    }
                    if (largeFont) {
                        Column(Modifier.fillMaxWidth()) {
                            TextButton(onClick = selectExercise, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text(item.exercise.name)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { onOpenExerciseDetails(item.exercise.id) }, enabled = !state.busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Detail") }
                                TextButton(onClick = { pendingRemove = item }, enabled = !state.busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Hapus gerakan") }
                            }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = selectExercise, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text(item.exercise.name) }
                            TextButton(onClick = { onOpenExerciseDetails(item.exercise.id) }, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Detail") }
                            TextButton(onClick = { pendingRemove = item }, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Hapus gerakan") }
                        }
                    }
                    item.sets.forEach { set ->
                        val setLabel = "${set.number}.  ${displayNumber(displayWeight(set.weightKg, weightUnit))} ${weightUnit.name.lowercase()} × ${set.reps}"
                        val editSet = {
                                    exerciseId = item.exercise.id
                                    onSelectExercise(item.exercise.id)
                                    editingSetId = set.id
                                    reps = set.reps.toString()
                                    weight = displayWeight(set.weightKg, weightUnit).stripTrailingZeros().toPlainString()
                                    editingOriginalDisplay = weight
                                    editingOriginalKg = set.weightKg.stripTrailingZeros().toPlainString()
                                    onEditSetInputs(set.id, reps, weight, editingOriginalKg.orEmpty())
                        }
                        val deleteSet: () -> Unit = {
                            scope.launch {
                                    onDeleteSet(draft.session.id, set.id)
                                    if (editingSetId == set.id) {
                                        editingSetId = null
                                        editingOriginalDisplay = null
                                        editingOriginalKg = null
                                        onClearSetForm()
                                    }
                                }
                        }
                        if (largeFont) {
                            Column(Modifier.fillMaxWidth()) {
                                TextButton(onClick = editSet, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(setLabel) }
                                TextButton(onClick = deleteSet, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Hapus set") }
                            }
                        } else {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(onClick = editSet, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text(setLabel) }
                                TextButton(onClick = deleteSet, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Hapus set") }
                            }
                        }
                    }
                    }
                }
                if (nextExercise != null) {
                    Text("Berikutnya", style = MaterialTheme.typography.titleMedium, color = Ink)
                    AthraCard(Modifier.padding(top = 8.dp, bottom = 12.dp)) {
                        Text(nextExercise.exercise.name, style = MaterialTheme.typography.titleMedium, color = Ink)
                        Text(
                            "${nextExercise.exercise.primaryMuscleGroup} · ${nextExercise.exercise.equipment}",
                            color = Secondary,
                        )
                    }
                }
                TextButton(onClick = onOpenLibrary, enabled = !state.busy) { Text("+ Tambah gerakan") }
                if (selected != null) {
                    AthraCard {
                    Text("Catat set · ${selected.exercise.name}", style = MaterialTheme.typography.titleMedium, color = Ink)
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it; onUpdateSetInputs(reps, weight) },
                        label = { Text("Repetisi") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it; onUpdateSetInputs(reps, weight) },
                        label = { Text("Beban (${weightUnit.name.lowercase()})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (!imeVisible) {
                        Button(
                            onClick = ::saveCurrentSet,
                            enabled = !state.busy && !savingSet,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (editingSetId == null) "Simpan set" else "Simpan perubahan set") }
                    }
                    }
                    val reference = state.exerciseReferences[selected.exercise.id]
                    Spacer(Modifier.height(12.dp))
                    AthraCard {
                    Text("Riwayat latihan terakhir", style = MaterialTheme.typography.titleSmall, color = Ink)
                    if (reference == null) {
                        Text("Belum ada riwayat untuk gerakan ini.", color = Secondary)
                    } else {
                        Text("${reference.session.name} · ${reference.sets.size} set", color = Secondary)
                        reference.sets.forEach { previous ->
                            Text("Set ${previous.number} · ${previous.reps} repetisi · ${displayNumber(displayWeight(previous.weightKg, weightUnit))} ${weightUnit.name.lowercase()}", color = Ink)
                        }
                        selected.sets.forEach { current ->
                            val previous = reference.sets.firstOrNull { it.number == current.number }
                            if (previous == null) Text("Set ${current.number}: Tidak ada pembanding")
                            else {
                                val weightDelta = displayNumber(displayWeight(current.weightKg.subtract(previous.weightKg), weightUnit))
                                val weightSign = if (current.weightKg > previous.weightKg) "+" else ""
                                val repsDelta = current.reps - previous.reps
                                val repsSign = if (repsDelta > 0) "+" else ""
                                Text("Set ${current.number}: $weightSign$weightDelta ${weightUnit.name.lowercase()} · $repsSign$repsDelta repetisi")
                            }
                        }
                        if (selected.sets.isNotEmpty()) {
                            val setDelta = selected.sets.size - reference.sets.size
                            val setSign = if (setDelta > 0) "+" else ""
                            Text("Set: ${selected.sets.size} vs ${reference.sets.size} · Δ $setSign$setDelta")
                            val currentVolume = selected.sets.fold(java.math.BigDecimal.ZERO) { total, set ->
                                total.add(set.weightKg.multiply(java.math.BigDecimal.valueOf(set.reps.toLong())))
                            }
                            val volumeDelta = currentVolume.subtract(reference.volumeKg)
                            val volumeSign = if (volumeDelta.signum() > 0) "+" else ""
                            Text("Volume gerakan: ${displayNumber(displayWeight(currentVolume, weightUnit))} ${weightUnit.name.lowercase()} · Δ $volumeSign${displayNumber(displayWeight(volumeDelta, weightUnit))}")
                        }
                    }
                    }
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { confirmDiscard = true }, enabled = !state.busy) { Text("Buang sesi") }
            }
        }
    }

    pendingRemove?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingRemove = null },
            title = { Text("Hapus gerakan?") },
            text = { Text("${item.exercise.name} dan ${item.sets.size} set-nya akan dihapus dari draft.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        onRemoveExercise(draft.session.id, item.exercise.id)
                        if (exerciseId == item.exercise.id) { exerciseId = ""; reps = ""; weight = ""; editingSetId = null }
                        pendingRemove = null
                    }
                }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { pendingRemove = null }) { Text("Batal") } },
        )
    }

    BackHandler(enabled = draft.session.status == SessionStatus.ACTIVE) { confirmLeaveWorkout = true }

    if (confirmLeaveWorkout) AlertDialog(
        onDismissRequest = { confirmLeaveWorkout = false },
        title = { Text("Tinggalkan workout?") },
        text = { Text("Draft tersimpan dan dapat dilanjutkan nanti.") },
        confirmButton = {
            TextButton(onClick = { confirmLeaveWorkout = false }) { Text("Tetap latihan") }
        },
        dismissButton = {
            Column {
                TextButton(onClick = { confirmLeaveWorkout = false; onExit() }) { Text("Kembali ke Home") }
                TextButton(onClick = { confirmLeaveWorkout = false; confirmDiscard = true }, enabled = !state.busy) { Text("Buang sesi") }
            }
        },
    )

    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false },
        title = { Text("Buang sesi?") },
        text = { Text("Sesi dan semua set di dalam draft akan dihapus permanen.") },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    val discarded = onDiscardResult(draft.session.id)
                    confirmDiscard = false
                    if (discarded) onExit()
                }
            }, enabled = !state.busy) { Text("Buang sesi") }
        },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Lanjutkan latihan") } },
    )

    if (confirmUnsavedFinish) AlertDialog(
        onDismissRequest = { confirmUnsavedFinish = false },
        title = { Text("Input belum disimpan") },
        text = { Text("Simpan set ini sebelum membuka ringkasan, atau hapus input yang belum tersimpan?") },
        confirmButton = {
            TextButton(onClick = {
                reps = ""; weight = ""; editingSetId = null; editingOriginalDisplay = null; editingOriginalKg = null
                onClearSetForm()
                confirmUnsavedFinish = false
                scope.launch { onFinish(draft.session.id) }
            }) { Text("Hapus input dan tinjau") }
        },
        dismissButton = { TextButton(onClick = { confirmUnsavedFinish = false }) { Text("Kembali ke input") } },
    )
}
