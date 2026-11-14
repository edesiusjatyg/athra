package id.ac.ub.athra.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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
import id.ac.ub.athra.ui.theme.Background
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
    weightUnit: WeightUnit = WeightUnit.KG,
    clock: WorkoutClock = SystemWorkoutClock,
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var exerciseId by rememberSaveable(draft.session.id) { mutableStateOf(draft.exercises.firstOrNull()?.exercise?.id.orEmpty()) }
    var reps by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var editingSetId by rememberSaveable(draft.session.id) { mutableStateOf<String?>(null) }
    var editingOriginalDisplay by rememberSaveable { mutableStateOf<String?>(null) }
    var editingOriginalKg by rememberSaveable { mutableStateOf<String?>(null) }
    var showLibrary by remember { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    var muscleFilter by rememberSaveable { mutableStateOf("Semua") }
    var pendingMuscleFilter by rememberSaveable { mutableStateOf("Semua") }
    var filterMenu by remember { mutableStateOf(false) }
    var detailExerciseId by remember { mutableStateOf<String?>(null) }
    var pendingRemove by remember { mutableStateOf<id.ac.ub.athra.domain.model.WorkoutExercise?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmUnsavedFinish by remember { mutableStateOf(false) }
    var savingSet by remember(draft.session.id) { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(clock.nowMillis()) }
    LaunchedEffect(draft.session.startedAt, draft.session.status) {
        while (draft.session.status == SessionStatus.ACTIVE) {
            now = clock.nowMillis()
            kotlinx.coroutines.delay(1000)
        }
    }
    val totals = calculateTotals(draft)
    val elapsed = elapsedSeconds(draft.session.startedAt, draft.session.endedAt ?: now)
    val timer = "%02d:%02d:%02d".format(elapsed / 3600, elapsed % 3600 / 60, elapsed % 60)
    val selected = draft.exercises.firstOrNull { it.exercise.id == exerciseId }

    Scaffold(containerColor = Background, topBar = {
        TopAppBar(title = { Text(if (draft.session.status == SessionStatus.REVIEW) "Ringkasan" else "Workout aktif") },
            navigationIcon = { TextButton(onClick = onExit) { Text("Kembali") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background))
    }) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).imePadding().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text(draft.session.name, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("Durasi  $timer", style = MaterialTheme.typography.titleLarge)
            Text("${totals.setCount} set · ${totals.repCount} repetisi · ${displayNumber(displayWeight(totals.volumeKg, weightUnit))} ${weightUnit.name.lowercase()}", color = MaterialTheme.colorScheme.secondary)
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
                Text("Gerakan", style = MaterialTheme.typography.titleMedium)
                draft.exercises.forEach { item ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(
                            onClick = { exerciseId = item.exercise.id; editingSetId = null; editingOriginalDisplay = null; editingOriginalKg = null; reps = ""; weight = "" },
                            enabled = !state.busy,
                        ) { Text(item.exercise.name) }
                        TextButton(onClick = { pendingRemove = item }, enabled = !state.busy) { Text("Hapus gerakan") }
                    }
                    item.sets.forEach { set ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(
                                onClick = {
                                    exerciseId = item.exercise.id
                                    editingSetId = set.id
                                    reps = set.reps.toString()
                                    weight = displayWeight(set.weightKg, weightUnit).stripTrailingZeros().toPlainString()
                                    editingOriginalDisplay = weight
                                    editingOriginalKg = set.weightKg.stripTrailingZeros().toPlainString()
                                },
                                enabled = !state.busy,
                            ) { Text("${set.number}.  ${displayNumber(displayWeight(set.weightKg, weightUnit))} ${weightUnit.name.lowercase()} × ${set.reps}") }
                            TextButton(
                                onClick = { scope.launch { onDeleteSet(draft.session.id, set.id); if (editingSetId == set.id) { editingSetId = null; editingOriginalDisplay = null; editingOriginalKg = null } } },
                                enabled = !state.busy,
                            ) { Text("Hapus") }
                        }
                    }
                }
                TextButton(onClick = { showLibrary = true }, enabled = !state.busy) { Text("+ Tambah gerakan") }
                if (selected != null) {
                    Text("Catat set · ${selected.exercise.name}", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it },
                        label = { Text("Repetisi") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Beban (${weightUnit.name.lowercase()})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            if (savingSet || state.busy) return@Button
                            savingSet = true
                            scope.launch {
                                try {
                                    val unchangedWeight = editingSetId != null && weight == editingOriginalDisplay
                                    val saved = onSaveSet(
                                        draft.session.id,
                                        selected.exercise.id,
                                        editingSetId,
                                        reps,
                                        if (unchangedWeight) editingOriginalKg.orEmpty() else weight,
                                        if (unchangedWeight) WeightUnit.KG else weightUnit,
                                    )
                                    if (saved) {
                                        reps = ""; weight = ""; editingSetId = null; editingOriginalDisplay = null; editingOriginalKg = null
                                    }
                                } finally {
                                    savingSet = false
                                }
                            }
                        },
                        enabled = !state.busy && !savingSet,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (editingSetId == null) "Simpan set" else "Simpan perubahan set") }
                    val reference = state.exerciseReferences[selected.exercise.id]
                    Spacer(Modifier.height(12.dp))
                    Text("Riwayat terakhir", style = MaterialTheme.typography.titleSmall)
                    if (reference == null) {
                        Text("Belum ada riwayat untuk gerakan ini.", color = MaterialTheme.colorScheme.secondary)
                    } else {
                        reference.sets.forEach { previous ->
                            Text("Set ${previous.number} · ${previous.reps} repetisi · ${displayNumber(displayWeight(previous.weightKg, weightUnit))} ${weightUnit.name.lowercase()}")
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
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    if (reps.isNotBlank() || weight.isNotBlank()) confirmUnsavedFinish = true
                    else scope.launch { onFinish(draft.session.id) }
                }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Selesai dan tinjau") }
                TextButton(onClick = { confirmDiscard = true }, enabled = !state.busy) { Text("Buang sesi") }
            }
        }
    }

    if (showLibrary) AlertDialog(
        onDismissRequest = { showLibrary = false },
        title = { Text(if (detailExerciseId == null) "Pilih gerakan" else "Detail gerakan") },
        text = {
            val detail = state.catalog.firstOrNull { it.id == detailExerciseId }
            if (detail != null) {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    Text(detail.name, style = MaterialTheme.typography.titleLarge)
                    Text("${detail.primaryMuscleGroup} · ${detail.equipment}", color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(12.dp))
                    Text(detail.instructions)
                    Spacer(Modifier.height(12.dp))
                    Text("Riwayat terakhir", style = MaterialTheme.typography.titleSmall)
                    val reference = state.exerciseReferences[detail.id]
                    if (reference == null) Text("Belum ada riwayat untuk gerakan ini.", color = MaterialTheme.colorScheme.secondary)
                    else reference.sets.forEach { set ->
                        Text("Set ${set.number} · ${set.reps} repetisi · ${displayNumber(displayWeight(set.weightKg, weightUnit))} ${weightUnit.name.lowercase()}")
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        scope.launch { onAddExercise(draft.session.id, detail.id); exerciseId = detail.id; detailExerciseId = null; showLibrary = false }
                    }) { Text("Tambah ke sesi") }
                    TextButton(onClick = { detailExerciseId = null }) { Text("Kembali ke katalog") }
                }
            } else Column {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Cari gerakan") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box {
                        TextButton(
                            onClick = { pendingMuscleFilter = muscleFilter; filterMenu = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Kelompok otot: $pendingMuscleFilter ▾", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        DropdownMenu(expanded = filterMenu, onDismissRequest = { filterMenu = false }) {
                            listOf("Semua", "Back", "Chest", "Legs", "Arms", "Shoulders", "Core").forEach { group ->
                                DropdownMenuItem(text = { Text(group) }, onClick = { pendingMuscleFilter = group; filterMenu = false })
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { pendingMuscleFilter = "Semua" }) { Text("Reset filter") }
                        TextButton(onClick = { muscleFilter = pendingMuscleFilter }) { Text("Terapkan") }
                    }
                }
                val matches = state.catalog.filter { exercise ->
                    exercise.name.contains(search.trim(), ignoreCase = true) &&
                        (muscleFilter == "Semua" || exercise.primaryMuscleGroup.equals(muscleFilter, ignoreCase = true))
                }
                if (matches.isEmpty()) {
                    Text("Gerakan tidak ditemukan.")
                } else LazyColumn(Modifier.heightIn(max = 280.dp)) {
                    items(matches) { exercise ->
                        Text(exercise.name, modifier = Modifier.fillMaxWidth().clickable {
                            detailExerciseId = exercise.id
                        }.padding(vertical = 12.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { detailExerciseId = null; showLibrary = false }) { Text("Tutup") } },
    )

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

    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false },
        title = { Text("Buang sesi?") },
        text = { Text("Sesi dan semua set di dalam draft akan dihapus permanen.") },
        confirmButton = {
            TextButton(onClick = { scope.launch { onDiscard(draft.session.id); confirmDiscard = false; onExit() } }) { Text("Buang sesi") }
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
                confirmUnsavedFinish = false
                scope.launch { onFinish(draft.session.id) }
            }) { Text("Hapus input dan tinjau") }
        },
        dismissButton = { TextButton(onClick = { confirmUnsavedFinish = false }) { Text("Kembali ke input") } },
    )
}
