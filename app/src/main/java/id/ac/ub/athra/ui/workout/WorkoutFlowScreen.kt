package id.ac.ub.athra.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
import id.ac.ub.athra.ui.theme.Accent
import id.ac.ub.athra.ui.theme.Border
import id.ac.ub.athra.ui.theme.Surface
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
    var showSetForm by rememberSaveable(draft.session.id, selectedExerciseId) {
        mutableStateOf(selected?.sets.isNullOrEmpty() || state.repsInput.isNotBlank() || state.weightInput.isNotBlank())
    }
    var showReference by rememberSaveable(draft.session.id, selectedExerciseId) { mutableStateOf(false) }
    var showExerciseMenu by remember { mutableStateOf(false) }
    val selectExercise: (String) -> Unit = { id ->
        exerciseId = id
        onSelectExercise(id)
        editingSetId = null
        editingOriginalDisplay = null
        editingOriginalKg = null
        reps = ""
        weight = ""
        onClearSetForm()
    }

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

    Scaffold(
        containerColor = Background,
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(64.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { confirmLeaveWorkout = true }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Ink, modifier = Modifier.size(20.dp))
                }
                Text(
                    if (draft.session.status == SessionStatus.REVIEW) "Ringkasan" else "Workout aktif",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                )
            }
        },
        bottomBar = {
            if (draft.session.status == SessionStatus.ACTIVE) {
                Column(
                    Modifier.fillMaxWidth().background(Background).navigationBarsPadding().imePadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                ) {
                    Button(
                        onClick = {
                            if (imeVisible && selectedExerciseId != null) saveCurrentSet()
                            else if (reps.isNotBlank() || weight.isNotBlank()) confirmUnsavedFinish = true
                            else scope.launch { onFinish(draft.session.id) }
                        },
                        enabled = !state.busy && !savingSet,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background),
                    ) {
                        Text(
                            if (imeVisible && selectedExerciseId != null) {
                                if (savingSet) "Menyimpan…" else "Simpan set"
                            } else "Selesai dan tinjau",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        },
    ) { inset ->
        Column(
            Modifier.fillMaxSize().padding(inset).imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(draft.session.name, style = MaterialTheme.typography.headlineLarge, color = Ink)
            val muscleGroups = draft.exercises.map { it.exercise.primaryMuscleGroup }.distinct().joinToString(" · ")
            if (muscleGroups.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(muscleGroups.uppercase(), style = MaterialTheme.typography.labelSmall, color = Secondary)
            }
            Spacer(Modifier.height(24.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF294B36)).padding(18.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("DURASI BERJALAN", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB0C5B4))
                        Spacer(Modifier.height(8.dp))
                        Text(timer, style = MaterialTheme.typography.titleLarge.copy(fontSize = 30.sp, lineHeight = 36.sp), color = Ink)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("SET TERSIMPAN", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB0C5B4))
                        Spacer(Modifier.height(8.dp))
                        Text(totals.setCount.toString(), style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp), color = Accent)
                    }
                }
                Spacer(Modifier.height(14.dp))
                val completedExercises = draft.exercises.count { it.sets.isNotEmpty() }
                val progress = if (draft.exercises.isEmpty()) 0f else completedExercises.toFloat() / draft.exercises.size
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(5.dp).semantics {
                        contentDescription = "$completedExercises dari ${draft.exercises.size} gerakan memiliki set tersimpan"
                    },
                    color = Accent,
                    trackColor = Color(0xFF48664E),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                    drawStopIndicator = {},
                )
            }
            Spacer(Modifier.height(24.dp))

            if (draft.session.status == SessionStatus.REVIEW) {
                Text("${totals.exerciseCount} gerakan", style = MaterialTheme.typography.titleMedium, color = Ink)
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
                        modifier = Modifier.padding(top = 8.dp), color = Ink,
                    )
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(24.dp))
                Button(onClick = { scope.launch { if (onSave(draft.session.id)) onExit() } }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Simpan sesi") }
                TextButton(onClick = { scope.launch { onResume(draft.session.id) } }, enabled = !state.busy) { Text("Kembali memperbaiki") }
                TextButton(onClick = { confirmDiscard = true }, enabled = !state.busy) { Text("Buang sesi") }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Gerakan aktif", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 18.sp), color = Ink)
                    IconButton(onClick = onOpenLibrary, enabled = !state.busy, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah gerakan", tint = Accent, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (selected == null) {
                    AthraCard {
                        Text("Pilih gerakan pertamamu", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Text("Tambahkan gerakan untuk mulai mencatat set.", style = MaterialTheme.typography.bodyMedium, color = Secondary)
                        TextButton(onClick = onOpenLibrary, enabled = !state.busy) { Text("+ Tambah gerakan") }
                    }
                } else {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface)
                            .border(1.dp, Border, RoundedCornerShape(20.dp)).padding(18.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(selected.exercise.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, color = Ink)
                            AthraChip(label = selected.exercise.equipment.uppercase())
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(selected.exercise.primaryMuscleGroup, style = MaterialTheme.typography.bodyMedium, color = Secondary)
                        Spacer(Modifier.height(18.dp))
                        HorizontalDivider(color = Border)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("SET", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, color = Secondary)
                            Text("BEBAN", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, color = Secondary)
                            Text("REPETISI", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = Secondary)
                            Spacer(Modifier.width(28.dp))
                        }
                        selected.sets.forEachIndexed { index, set ->
                            val editing = editingSetId == set.id
                            Row(
                                Modifier.fillMaxWidth().padding(top = 2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (editing || index == selected.sets.lastIndex) Forest else Color.Transparent)
                                    .clickable(enabled = !state.busy) {
                                        editingSetId = set.id
                                        reps = set.reps.toString()
                                        weight = displayWeight(set.weightKg, weightUnit).stripTrailingZeros().toPlainString()
                                        editingOriginalDisplay = weight
                                        editingOriginalKg = set.weightKg.stripTrailingZeros().toPlainString()
                                        onEditSetInputs(set.id, reps, weight, editingOriginalKg.orEmpty())
                                        showSetForm = true
                                    }
                                    .heightIn(min = 34.dp).padding(horizontal = 8.dp)
                                    .semantics { contentDescription = "Set ${set.number}: ${displayNumber(displayWeight(set.weightKg, weightUnit))} ${weightUnit.name.lowercase()} × ${set.reps}. Ketuk untuk edit" },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("%02d".format(set.number), modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodyMedium, color = Ink)
                                Text("${displayNumber(displayWeight(set.weightKg, weightUnit))} ${weightUnit.name.lowercase()}", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodyMedium, color = Ink)
                                Text(set.reps.toString(), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = Ink)
                                Icon(Icons.Default.Check, contentDescription = "Set tersimpan", tint = if (index == selected.sets.lastIndex) Secondary else Forest, modifier = Modifier.size(18.dp))
                            }
                        }
                        if (selected.sets.isEmpty()) {
                            Text("Belum ada set tersimpan", style = MaterialTheme.typography.bodyMedium, color = Secondary, modifier = Modifier.padding(vertical = 10.dp))
                        }
                        if (showSetForm) {
                            Spacer(Modifier.height(14.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = weight,
                                    onValueChange = { weight = it; onUpdateSetInputs(reps, weight) },
                                    label = { Text("Beban (${weightUnit.name.lowercase()})") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = reps,
                                    onValueChange = { reps = it; onUpdateSetInputs(reps, weight) },
                                    label = { Text("Repetisi") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            if (!imeVisible) Button(
                                onClick = ::saveCurrentSet,
                                enabled = !state.busy && !savingSet,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Forest, contentColor = Accent),
                                shape = RoundedCornerShape(10.dp),
                            ) { Text(if (savingSet) "Menyimpan…" else if (editingSetId == null) "Simpan set" else "Simpan perubahan set") }
                            if (editingSetId != null) TextButton(onClick = {
                                val id = editingSetId
                                scope.launch {
                                    if (id != null) onDeleteSet(draft.session.id, id)
                                    editingSetId = null
                                    editingOriginalDisplay = null
                                    editingOriginalKg = null
                                    reps = ""; weight = ""
                                    onClearSetForm()
                                }
                            }, enabled = !state.busy) { Text("Hapus set", color = MaterialTheme.colorScheme.error) }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { showSetForm = !showSetForm }, enabled = !state.busy, contentPadding = PaddingValues(horizontal = 0.dp)) {
                                Text(if (showSetForm) "Tutup input" else "+ Tambah set", style = MaterialTheme.typography.labelMedium, color = Accent)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { onOpenExerciseDetails(selected.exercise.id) }, enabled = !state.busy, contentPadding = PaddingValues(horizontal = 6.dp)) {
                                    Text("Detail", style = MaterialTheme.typography.labelMedium, color = Secondary)
                                }
                                Box {
                                    IconButton(onClick = { showExerciseMenu = true }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.MoreHoriz, contentDescription = "Opsi gerakan", tint = Secondary, modifier = Modifier.size(20.dp))
                                    }
                                    DropdownMenu(expanded = showExerciseMenu, onDismissRequest = { showExerciseMenu = false }) {
                                        DropdownMenuItem(text = { Text("Riwayat latihan terakhir") }, onClick = { showReference = !showReference; showExerciseMenu = false })
                                        DropdownMenuItem(text = { Text("Hapus gerakan") }, onClick = { pendingRemove = selected; showExerciseMenu = false }, enabled = !state.busy)
                                    }
                                }
                            }
                        }
                    }
                    if (showReference) {
                        val reference = state.exerciseReferences[selected.exercise.id]
                        Spacer(Modifier.height(12.dp))
                        AthraCard {
                            Text("Riwayat latihan terakhir", style = MaterialTheme.typography.titleSmall, color = Ink)
                            if (reference == null) Text("Belum ada riwayat untuk gerakan ini.", color = Secondary)
                            else {
                                Text("${reference.session.name} · ${reference.sets.size} set", color = Secondary)
                                reference.sets.forEach { previous ->
                                    Text("Set ${previous.number} · ${previous.reps} repetisi · ${displayNumber(displayWeight(previous.weightKg, weightUnit))} ${weightUnit.name.lowercase()}", color = Ink)
                                }
                                selected.sets.forEach { current ->
                                    val previous = reference.sets.firstOrNull { it.number == current.number }
                                    if (previous == null) Text("Set ${current.number}: Tidak ada pembanding", color = Secondary)
                                    else {
                                        val weightDelta = displayNumber(displayWeight(current.weightKg.subtract(previous.weightKg), weightUnit))
                                        val weightSign = if (current.weightKg > previous.weightKg) "+" else ""
                                        val repsDelta = current.reps - previous.reps
                                        val repsSign = if (repsDelta > 0) "+" else ""
                                        Text("Set ${current.number}: $weightSign$weightDelta ${weightUnit.name.lowercase()} · $repsSign$repsDelta repetisi", color = Ink)
                                    }
                                }
                                if (selected.sets.isNotEmpty()) {
                                    val setDelta = selected.sets.size - reference.sets.size
                                    val setSign = if (setDelta > 0) "+" else ""
                                    Text("Set: ${selected.sets.size} vs ${reference.sets.size} · Δ $setSign$setDelta", color = Ink)
                                    val currentVolume = selected.sets.fold(java.math.BigDecimal.ZERO) { total, set -> total.add(set.weightKg.multiply(java.math.BigDecimal.valueOf(set.reps.toLong()))) }
                                    val volumeDelta = currentVolume.subtract(reference.volumeKg)
                                    val volumeSign = if (volumeDelta.signum() > 0) "+" else ""
                                    Text("Volume gerakan: ${displayNumber(displayWeight(currentVolume, weightUnit))} ${weightUnit.name.lowercase()} · Δ $volumeSign${displayNumber(displayWeight(volumeDelta, weightUnit))}", color = Ink)
                                }
                            }
                        }
                    }
                }
                val otherExercises = draft.exercises.filter { it.exercise.id != selectedExerciseId }
                if (otherExercises.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    Text("Berikutnya", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 18.sp), color = Ink)
                    Spacer(Modifier.height(16.dp))
                    val ordered = if (nextExercise == null) otherExercises else listOf(nextExercise) + otherExercises.filter { it.exercise.id != nextExercise.exercise.id }
                    ordered.forEach { item ->
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 10.dp)
                                .clip(RoundedCornerShape(16.dp)).background(Surface)
                                .border(1.dp, Border, RoundedCornerShape(16.dp))
                                .clickable(enabled = !state.busy) { selectExercise(item.exercise.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(Modifier.size(42.dp).clip(CircleShape).background(Forest), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Accent.copy(alpha = 0.65f), modifier = Modifier.size(20.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(item.exercise.name, style = MaterialTheme.typography.titleSmall, color = Ink)
                                Spacer(Modifier.height(4.dp))
                                Text("${item.exercise.primaryMuscleGroup} · ${item.sets.size} set", style = MaterialTheme.typography.bodyMedium, color = Secondary)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Secondary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { confirmDiscard = true }, enabled = !state.busy) { Text("Buang sesi", color = Secondary, style = MaterialTheme.typography.labelMedium) }
            }
            Spacer(Modifier.height(12.dp))
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
