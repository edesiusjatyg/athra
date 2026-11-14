package id.ac.ub.athra.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import id.ac.ub.athra.domain.calculation.calculateTotals
import id.ac.ub.athra.domain.calculation.displayNumber
import id.ac.ub.athra.domain.calculation.displayWeight
import id.ac.ub.athra.domain.model.Exercise
import id.ac.ub.athra.domain.model.ExerciseReference
import id.ac.ub.athra.domain.model.PersonalRecordStatus
import id.ac.ub.athra.domain.model.SessionStatus
import id.ac.ub.athra.domain.model.WeightUnit
import id.ac.ub.athra.domain.model.WorkoutDraft
import id.ac.ub.athra.ui.theme.Background
import id.ac.ub.athra.ui.theme.Ink
import id.ac.ub.athra.ui.theme.Secondary
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun QuickStartScreen(
    name: String,
    busy: Boolean,
    error: String?,
    onNameChange: (String) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Mulai cepat") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Kembali") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
            )
        },
        bottomBar = {
            Button(onClick = onStart, enabled = !busy, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                if (busy) CircularProgressIndicator() else Text("Mulai sesi")
            }
        },
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Nama sesi (opsional)", style = MaterialTheme.typography.titleLarge, color = Ink)
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Nama workout") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Jika dikosongkan, nama dibuat dari waktu mulai.", color = Secondary)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ExerciseLibraryScreen(
    state: WorkoutUiState,
    onSearchChange: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onOpenDetails: (String) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val matches = state.catalog.filter { exercise ->
        exercise.name.contains(state.librarySearch.trim(), ignoreCase = true) &&
            (state.libraryFilter == "Semua" || exercise.primaryMuscleGroup.equals(state.libraryFilter, ignoreCase = true))
    }
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Pilih gerakan") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Kembali") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
            )
        },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).imePadding().padding(20.dp)) {
            OutlinedTextField(
                value = state.librarySearch,
                onValueChange = onSearchChange,
                label = { Text("Cari gerakan") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {}),
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onOpenFilters, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Filter kelompok otot · ${state.libraryFilter}")
            }
            state.catalogError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text("Coba lagi") }
            }
            when {
                state.catalogLoading && state.catalog.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                matches.isEmpty() -> Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    Text(
                        if (state.catalogError != null) "Katalog gerakan belum tersedia."
                        else if (state.catalog.isEmpty()) "Katalog gerakan belum tersedia."
                        else "Gerakan tidak ditemukan.",
                        color = Secondary,
                    )
                    if (state.catalogError == null) TextButton(onClick = onRetry) { Text("Muat ulang") }
                }
                else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(matches, key = { it.id }) { exercise ->
                        ExerciseLibraryRow(exercise, onClick = { onOpenDetails(exercise.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseLibraryRow(exercise: Exercise, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(exercise.name, color = Ink, style = MaterialTheme.typography.titleMedium)
            Text("${exercise.primaryMuscleGroup} · ${exercise.equipment}", color = Secondary)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ExerciseFiltersScreen(
    selected: String,
    onSelect: (String) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Filter gerakan") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Kembali") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
            )
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("Reset") }
                Button(onClick = onApply, modifier = Modifier.weight(1f)) { Text("Terapkan") }
            }
        },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(20.dp)) {
            listOf("Semua", "Back", "Chest", "Legs", "Arms", "Shoulders", "Core").forEach { group ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                        selected = selected == group,
                        onClick = { onSelect(group) },
                        role = Role.RadioButton,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected == group, onClick = null)
                    Text(group)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ExerciseDetailsScreen(
    exercise: Exercise?,
    reference: ExerciseReference?,
    weightUnit: WeightUnit,
    busy: Boolean,
    error: String?,
    onAdd: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Detail gerakan") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Kembali") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
            )
        },
        bottomBar = {
            if (exercise != null) Button(
                onClick = onAdd,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().padding(20.dp),
            ) { Text(if (busy) "Menambahkan…" else "Tambah ke sesi") }
        },
    ) { insets ->
        if (exercise == null) {
            Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.Center) {
                Text("Detail gerakan tidak tersedia.", color = Secondary)
            }
        } else Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(exercise.name, style = MaterialTheme.typography.headlineMedium, color = Ink)
            Text("${exercise.primaryMuscleGroup} · ${exercise.equipment}", color = Secondary)
            Text(exercise.instructions)
            Text("Riwayat terakhir", style = MaterialTheme.typography.titleMedium, color = Ink)
            if (reference == null) Text("Belum ada riwayat untuk gerakan ini.", color = Secondary)
            else {
                reference.sets.forEach { set ->
                    Text("Set ${set.number} · ${set.reps} repetisi · ${displayNumber(displayWeight(set.weightKg, weightUnit))} ${weightUnit.name.lowercase()}")
                }
                Text("Volume ${displayNumber(displayWeight(reference.volumeKg, weightUnit))} ${weightUnit.name.lowercase()}", color = Secondary)
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun WorkoutSummaryScreen(
    draft: WorkoutDraft,
    state: WorkoutUiState,
    weightUnit: WeightUnit,
    onResume: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    var confirmDiscard by remember { mutableStateOf(false) }
    val totals = calculateTotals(draft)
    val durationSeconds = (((draft.session.endedAt ?: draft.session.startedAt) - draft.session.startedAt).coerceAtLeast(0) / 1_000)
    BackHandler { onResume() }
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Ringkasan workout") },
                navigationIcon = {
                    TextButton(onClick = onResume) { Text("Kembali") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
            )
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                Button(
                    onClick = onSave,
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (state.busy) "Menyimpan…" else "Simpan sesi") }
            TextButton(onClick = { confirmDiscard = true }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Buang sesi")
                }
            }
        },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text(draft.session.name, style = MaterialTheme.typography.headlineMedium, color = Ink)
            Text("Durasi %02d:%02d:%02d".format(durationSeconds / 3_600, durationSeconds % 3_600 / 60, durationSeconds % 60))
            Text(
                "${totals.exerciseCount} gerakan · ${totals.setCount} set · ${totals.repCount} repetisi · " +
                    "${displayNumber(displayWeight(totals.volumeKg, weightUnit))} ${weightUnit.name.lowercase()}",
                color = Secondary,
            )
            Spacer(Modifier.height(20.dp))
            draft.exercises.filter { it.sets.isNotEmpty() }.forEach { item ->
                Text(item.exercise.name, style = MaterialTheme.typography.titleLarge, color = Ink)
                item.sets.forEach { set ->
                    Text("Set ${set.number} · ${set.reps} repetisi · ${displayNumber(displayWeight(set.weightKg, weightUnit))} ${weightUnit.name.lowercase()}")
                }
                val record = state.personalRecords.firstOrNull { it.exerciseId == item.exercise.id }
                if (record != null) {
                    val status = when (record.status) {
                        PersonalRecordStatus.BASELINE -> "Catatan awal"
                        PersonalRecordStatus.NEW_RECORD -> "Rekor baru"
                        PersonalRecordStatus.MATCHED -> "Menyamai rekor"
                        PersonalRecordStatus.BELOW_RECORD -> "Belum melampaui rekor"
                    }
                    Text(
                        "$status · ${displayNumber(displayWeight(record.currentMaxKg, weightUnit))} ${weightUnit.name.lowercase()}" +
                            (record.previousMaxKg?.let { " · sebelumnya ${displayNumber(displayWeight(it, weightUnit))} ${weightUnit.name.lowercase()}" } ?: ""),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false },
        title = { Text("Buang sesi?") },
        text = { Text("Sesi dan semua set di dalam draft akan dihapus permanen.") },
        confirmButton = {
            TextButton(onClick = { confirmDiscard = false; onDiscard() }, enabled = !state.busy) { Text("Buang sesi") }
        },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Kembali") } },
    )
}
