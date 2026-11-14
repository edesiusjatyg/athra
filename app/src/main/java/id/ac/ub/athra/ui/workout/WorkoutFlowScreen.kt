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
import androidx.compose.ui.unit.dp
import id.ac.ub.athra.domain.calculation.calculateTotals
import id.ac.ub.athra.domain.calculation.displayNumber
import id.ac.ub.athra.domain.calculation.displayWeight
import id.ac.ub.athra.domain.model.SessionStatus
import id.ac.ub.athra.domain.model.PersonalRecordStatus
import id.ac.ub.athra.domain.model.WeightUnit
import id.ac.ub.athra.domain.model.WorkoutDraft
import id.ac.ub.athra.ui.theme.Background
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun WorkoutFlowScreen(
    draft: WorkoutDraft,
    state: WorkoutUiState,
    onSaveSet: suspend (String, String, String?, String, String, WeightUnit) -> Unit,
    onDeleteSet: suspend (String, String) -> Unit,
    onAddExercise: suspend (String, String) -> Unit,
    onFinish: suspend (String) -> Boolean,
    onResume: suspend (String) -> Unit,
    onSave: suspend (String) -> Boolean,
    onDiscard: suspend (String) -> Unit,
    onExit: () -> Unit,
    weightUnit: WeightUnit = WeightUnit.KG,
) {
    val scope = rememberCoroutineScope()
    var exerciseId by rememberSaveable(draft.session.id) { mutableStateOf(draft.exercises.firstOrNull()?.exercise?.id.orEmpty()) }
    var reps by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var editingSetId by rememberSaveable(draft.session.id) { mutableStateOf<String?>(null) }
    var showLibrary by remember { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    var muscleFilter by rememberSaveable { mutableStateOf("Semua") }
    var filterMenu by remember { mutableStateOf(false) }
    var detailExerciseId by remember { mutableStateOf<String?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(draft.session.startedAt, draft.session.status) {
        while (draft.session.status == SessionStatus.ACTIVE) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }
    val totals = calculateTotals(draft)
    val elapsed = ((draft.session.endedAt ?: now) - draft.session.startedAt).coerceAtLeast(0) / 1000
    val timer = "%02d:%02d:%02d".format(elapsed / 3600, elapsed % 3600 / 60, elapsed % 60)
    val selected = draft.exercises.firstOrNull { it.exercise.id == exerciseId }

    Scaffold(containerColor = Background, topBar = {
        TopAppBar(title = { Text(if (draft.session.status == SessionStatus.REVIEW) "Ringkasan" else "Workout aktif") },
            navigationIcon = { TextButton(onClick = onExit) { Text("Kembali") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background))
    }) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState()).padding(20.dp)) {
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
                Button(onClick = { scope.launch { if (onSave(draft.session.id)) onExit() } }, modifier = Modifier.fillMaxWidth()) { Text("Simpan sesi") }
                TextButton(onClick = { scope.launch { onResume(draft.session.id) } }) { Text("Kembali memperbaiki") }
                TextButton(onClick = { scope.launch { onDiscard(draft.session.id); onExit() } }) { Text("Buang sesi") }
            } else {
                Text("Gerakan", style = MaterialTheme.typography.titleMedium)
                draft.exercises.forEach { item ->
                    TextButton(onClick = { exerciseId = item.exercise.id; editingSetId = null; reps = ""; weight = "" }) { Text(item.exercise.name) }
                    item.sets.forEach { set ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = {
                                exerciseId = item.exercise.id
                                editingSetId = set.id
                                reps = set.reps.toString()
                                weight = displayWeight(set.weightKg, weightUnit).stripTrailingZeros().toPlainString()
                            }) { Text("${set.number}.  ${displayNumber(displayWeight(set.weightKg, weightUnit))} ${weightUnit.name.lowercase()} × ${set.reps}") }
                            TextButton(onClick = { scope.launch { onDeleteSet(draft.session.id, set.id); if (editingSetId == set.id) editingSetId = null } }) { Text("Hapus") }
                        }
                    }
                }
                TextButton(onClick = { showLibrary = true }) { Text("+ Tambah gerakan") }
                if (selected != null) {
                    Text("Catat set · ${selected.exercise.name}", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(reps, { reps = it }, label = { Text("Repetisi") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(weight, { weight = it }, label = { Text("Beban (${weightUnit.name.lowercase()})") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        scope.launch {
                            onSaveSet(draft.session.id, selected.exercise.id, editingSetId, reps, weight, weightUnit)
                            reps = ""; weight = ""; editingSetId = null
                        }
                }, modifier = Modifier.fillMaxWidth()) { Text(if (editingSetId == null) "Simpan set" else "Simpan perubahan set") }
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(12.dp))
                Button(onClick = { scope.launch { onFinish(draft.session.id) } }, modifier = Modifier.fillMaxWidth()) { Text("Selesai dan tinjau") }
                TextButton(onClick = { scope.launch { onDiscard(draft.session.id); onExit() } }) { Text("Buang sesi") }
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
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        scope.launch { onAddExercise(draft.session.id, detail.id); exerciseId = detail.id; detailExerciseId = null; showLibrary = false }
                    }) { Text("Tambah ke sesi") }
                    TextButton(onClick = { detailExerciseId = null }) { Text("Kembali ke katalog") }
                }
            } else Column {
                OutlinedTextField(search, { search = it }, label = { Text("Cari gerakan") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Box {
                    TextButton(onClick = { filterMenu = true }) { Text("Kelompok otot: $muscleFilter ▾") }
                    DropdownMenu(expanded = filterMenu, onDismissRequest = { filterMenu = false }) {
                        listOf("Semua", "Back", "Chest", "Legs", "Arms", "Shoulders", "Core").forEach { group ->
                            DropdownMenuItem(text = { Text(group) }, onClick = { muscleFilter = group; filterMenu = false })
                        }
                        DropdownMenuItem(text = { Text("Reset") }, onClick = { muscleFilter = "Semua"; search = ""; filterMenu = false })
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
}
