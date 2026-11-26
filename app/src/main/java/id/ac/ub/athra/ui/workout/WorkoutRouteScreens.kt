package id.ac.ub.athra.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.ac.ub.athra.domain.calculation.calculateTotals
import id.ac.ub.athra.domain.calculation.displayNumber
import id.ac.ub.athra.domain.calculation.displayWeight
import id.ac.ub.athra.domain.model.Exercise
import id.ac.ub.athra.domain.model.ExerciseReference
import id.ac.ub.athra.domain.model.PersonalRecordStatus
import id.ac.ub.athra.domain.model.WeightUnit
import id.ac.ub.athra.domain.model.WorkoutDraft
import id.ac.ub.athra.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
private fun WorkoutPageBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Ink, modifier = Modifier.size(20.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
    }
}

@Composable
private fun WorkoutPrimaryAction(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background),
    ) { Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun WorkoutFooter(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Background).navigationBarsPadding().imePadding().padding(horizontal = 24.dp, vertical = 16.dp),
        content = content,
    )
}

@Composable
private fun WorkoutInfoRow(title: String, subtitle: String, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (selected) Forest else Surface)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = Ink)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Secondary)
        }
        if (onClick != null) Icon(
            if (selected) Icons.Default.Check else Icons.Default.ChevronRight,
            contentDescription = null, tint = Accent, modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun QuickStartScreen(
    name: String,
    busy: Boolean,
    error: String?,
    onNameChange: (String) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    Scaffold(
        containerColor = Background,
        topBar = { WorkoutPageBar("Mulai cepat", onBack) },
        bottomBar = { WorkoutFooter { WorkoutPrimaryAction(if (busy) "Membuat sesi…" else "Pilih gerakan", !busy, onStart) } },
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(20.dp))
            Text("Mulai cepat", style = MaterialTheme.typography.headlineLarge, color = Ink)
            Spacer(Modifier.height(10.dp))
            Text("Buat sesi latihan", style = MaterialTheme.typography.bodyMedium, color = Secondary)
            Spacer(Modifier.height(32.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface).padding(16.dp)) {
                Text("Nama sesi", style = MaterialTheme.typography.headlineSmall, color = Ink)
                BasicTextField(
                    value = name,
                    onValueChange = onNameChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Ink),
                    cursorBrush = SolidColor(Accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 24.dp).focusRequester(focusRequester),
                    decorationBox = { inner ->
                        Box { if (name.isEmpty()) Text("Workout baru", style = MaterialTheme.typography.bodyMedium, color = Secondary); inner() }
                    },
                )
            }
            Spacer(Modifier.height(12.dp))
            WorkoutInfoRow("Tambah gerakan", "Pilih dari pustaka gerakan", onClick = { if (!busy) onStart() })
            Spacer(Modifier.height(16.dp))
            Text("Nama sesi dapat dikosongkan. Gerakan dipilih setelah sesi dibuat.", style = MaterialTheme.typography.bodyMedium, color = Secondary)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun ExerciseLibraryScreen(
    state: WorkoutUiState,
    onSearchChange: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onOpenDetails: (String) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAddExercise: (String) -> Unit = {},
    busy: Boolean = false,
    onFilterChange: (String) -> Unit = {},
    onNavigate: ((Int) -> Unit)? = null,
) {
    val matches = state.catalog.filter { exercise ->
        exercise.name.contains(state.librarySearch.trim(), ignoreCase = true) &&
            (state.libraryFilter == "Semua" || exercise.primaryMuscleGroup.equals(state.libraryFilter, ignoreCase = true))
    }
    Scaffold(
        containerColor = Background,
        topBar = { WorkoutPageBar("Latihan", onBack) },
        bottomBar = { onNavigate?.let { id.ac.ub.athra.ui.screens.AthraBottomNav(selected = 1, onSelect = it) } },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).imePadding().padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(20.dp))
            Text("Pustaka gerakan", style = MaterialTheme.typography.headlineLarge, color = Ink)
            Spacer(Modifier.height(10.dp))
            Text("Temukan gerakan untuk sesi latihanmu.", style = MaterialTheme.typography.bodyMedium, color = Secondary)
            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value = state.librarySearch,
                onValueChange = onSearchChange,
                placeholder = { Text("Cari gerakan", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Secondary, modifier = Modifier.size(20.dp)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Surface, focusedContainerColor = Surface,
                    unfocusedBorderColor = Border, focusedBorderColor = Accent,
                    focusedTextColor = Ink, unfocusedTextColor = Ink,
                    focusedPlaceholderColor = Secondary, unfocusedPlaceholderColor = Secondary,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Semua", "Back", "Chest", "Legs", "Arms").forEach { group ->
                    val selected = state.libraryFilter == group
                    Box(
                        Modifier.clip(CircleShape).background(if (selected) Color(0xFF345139) else Surface)
                            .border(1.dp, if (selected) Color.Transparent else Border, CircleShape)
                            .clickable(role = Role.Button) { onFilterChange(group) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) { Text(group, color = if (selected) Ink else Secondary, style = MaterialTheme.typography.labelMedium) }
                }
                IconButton(onClick = onOpenFilters, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = "Semua filter gerakan", tint = Secondary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Daftar gerakan", style = MaterialTheme.typography.headlineSmall, color = Ink)
                Text("${matches.size} gerakan", style = MaterialTheme.typography.labelSmall, color = Secondary)
            }
            Spacer(Modifier.height(16.dp))
            state.catalogError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text("Coba lagi") }
            }
            when {
                state.catalogLoading && state.catalog.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                matches.isEmpty() -> Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    Text(if (state.catalog.isEmpty()) "Katalog gerakan belum tersedia." else "Gerakan tidak ditemukan.", color = Secondary)
                    if (state.catalogError == null) TextButton(onClick = onRetry) { Text("Muat ulang") }
                }
                else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                    items(matches, key = { it.id }) { exercise ->
                        ExerciseLibraryRow(exercise, { onOpenDetails(exercise.id) }, { onAddExercise(exercise.id) }, busy)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseLibraryRow(exercise: Exercise, onClick: () -> Unit, onAdd: () -> Unit, busy: Boolean) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface).border(1.dp, Border, RoundedCornerShape(16.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(Forest), contentAlignment = Alignment.Center) {
            Text(exercise.name.split(' ').take(2).mapNotNull { it.firstOrNull() }.joinToString("").uppercase(), color = Accent, style = MaterialTheme.typography.labelLarge)
        }
        Column(Modifier.weight(1f).heightIn(min = 50.dp).clickable(role = Role.Button, onClick = onClick), verticalArrangement = Arrangement.Center) {
            Text(exercise.name, color = Ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("${exercise.equipment} · ${exercise.primaryMuscleGroup}", color = Secondary, style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = onAdd, enabled = !busy, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Forest)) {
            Icon(Icons.Default.Add, contentDescription = "Tambahkan ${exercise.name}", tint = Accent, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ExerciseFiltersScreen(
    selected: String,
    onSelect: (String) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = Background,
        topBar = { WorkoutPageBar("Filter gerakan", onBack) },
        bottomBar = { WorkoutFooter { WorkoutPrimaryAction("Lihat gerakan", onClick = onApply) } },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(20.dp))
            Text("Filter gerakan", style = MaterialTheme.typography.headlineLarge, color = Ink)
            Spacer(Modifier.height(10.dp))
            Text("Jelajahi kelompok otot", style = MaterialTheme.typography.bodyMedium, color = Secondary)
            Spacer(Modifier.height(32.dp))
            listOf("Back", "Chest", "Legs", "Arms", "Core", "Shoulders").forEach { group ->
                WorkoutInfoRow(group, when (group) {
                    "Back" -> "Punggung"
                    "Chest" -> "Dada"
                    "Legs" -> "Kaki"
                    "Arms" -> "Lengan"
                    "Core" -> "Otot inti"
                    else -> "Bahu"
                }, selected == group, onClick = { onSelect(group) })
                Spacer(Modifier.height(12.dp))
            }
            TextButton(onClick = onReset, modifier = Modifier.align(Alignment.End)) { Text("Reset filter") }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
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
        topBar = { WorkoutPageBar("Detail gerakan", onBack) },
        bottomBar = {
            if (exercise != null) WorkoutFooter {
                WorkoutPrimaryAction(if (busy) "Menambahkan…" else "Tambah ke sesi", !busy, onAdd)
            }
        },
    ) { insets ->
        if (exercise == null) Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.Center) {
            Text("Detail gerakan tidak tersedia.", color = Secondary)
        } else Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(20.dp))
            Text("Detail gerakan", style = MaterialTheme.typography.headlineLarge, color = Ink)
            Spacer(Modifier.height(10.dp))
            Text("Panduan gerakan dan performa", style = MaterialTheme.typography.bodyMedium, color = Secondary)
            Spacer(Modifier.height(32.dp))
            WorkoutInfoRow(exercise.name, "${exercise.equipment} · ${exercise.primaryMuscleGroup}")
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface).padding(16.dp)) {
                Text("Performa terakhir", style = MaterialTheme.typography.headlineSmall, color = Ink)
                Spacer(Modifier.height(8.dp))
                if (reference == null) Text("Belum ada riwayat untuk gerakan ini.", style = MaterialTheme.typography.bodyMedium, color = Secondary)
                else {
                    reference.sets.forEach { set ->
                        Text("Set ${set.number} · ${set.reps} repetisi · ${displayNumber(displayWeight(set.weightKg, weightUnit))} ${weightUnit.name.lowercase()}", style = MaterialTheme.typography.bodyMedium, color = Secondary)
                    }
                    Text("Volume ${displayNumber(displayWeight(reference.volumeKg, weightUnit))} ${weightUnit.name.lowercase()}", style = MaterialTheme.typography.bodyMedium, color = Secondary, modifier = Modifier.padding(top = 6.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            WorkoutInfoRow("Teknik", exercise.instructions)
            Spacer(Modifier.height(12.dp))
            WorkoutInfoRow("Target otot", exercise.primaryMuscleGroup)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
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
    val hours = durationSeconds / 3_600
    val minutes = durationSeconds % 3_600 / 60
    val durationLabel = if (hours > 0) "${hours}j ${minutes}m" else "${minutes}m"
    val dateLabel = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID")).format(
        Instant.ofEpochMilli(draft.session.endedAt ?: draft.session.startedAt).atZone(ZoneId.systemDefault()),
    )
    val muscleGroups = draft.exercises.map { it.exercise.primaryMuscleGroup }.distinct().joinToString(", ")
    BackHandler { onResume() }
    Scaffold(
        containerColor = Background,
        topBar = { WorkoutPageBar("Ringkasan", onResume) },
        bottomBar = { WorkoutFooter { WorkoutPrimaryAction(if (state.busy) "Menyimpan…" else "Simpan sesi", !state.busy, onSave) } },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(24.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(88.dp).clip(CircleShape).background(Forest), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFF345139)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Accent, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text("Workout selesai!", style = MaterialTheme.typography.headlineLarge, color = Ink)
                Spacer(Modifier.height(10.dp))
                Text("Satu sesi lagi, semakin kuat.", style = MaterialTheme.typography.bodyMedium, color = Secondary)
            }
            Spacer(Modifier.height(30.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface).border(1.dp, Border, RoundedCornerShape(16.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(draft.session.name, style = MaterialTheme.typography.headlineSmall, color = Ink)
                    Spacer(Modifier.height(8.dp))
                    Text(listOf(dateLabel, muscleGroups).filter(String::isNotBlank).joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = Secondary)
                }
                IconButton(onClick = onResume, enabled = !state.busy, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Perbaiki sesi", tint = Secondary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Ringkasan sesi", style = MaterialTheme.typography.headlineSmall, color = Ink)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryMetric("Durasi", durationLabel, Modifier.weight(1f))
                SummaryMetric("Volume · ${weightUnit.name.lowercase()}", summaryVolume(displayWeight(totals.volumeKg, weightUnit)), Modifier.weight(1f))
                SummaryMetric("Set selesai", totals.setCount.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Forest).padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Accent, modifier = Modifier.size(24.dp))
                Column {
                    Text("Sesi latihanmu siap disimpan.", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Ink)
                    Spacer(Modifier.height(6.dp))
                    Text("${draft.exercises.count { it.sets.isNotEmpty() }} gerakan · ${totals.repCount} repetisi tersimpan", style = MaterialTheme.typography.bodyMedium, color = Secondary)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Highlight gerakan", style = MaterialTheme.typography.headlineSmall, color = Ink)
            Spacer(Modifier.height(12.dp))
            draft.exercises.filter { it.sets.isNotEmpty() }.forEach { item ->
                var expanded by remember(item.exercise.id) { mutableStateOf(false) }
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { expanded = !expanded }.padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(item.exercise.name, style = MaterialTheme.typography.bodyLarge, color = Ink, modifier = Modifier.weight(1f))
                        Text("${item.sets.size} set · ${item.sets.sumOf { it.reps }} rep", style = MaterialTheme.typography.bodyMedium, color = Secondary)
                    }
                    if (expanded) item.sets.forEach { set ->
                        Text("Set ${set.number} · ${set.reps} repetisi · ${displayNumber(displayWeight(set.weightKg, weightUnit))} ${weightUnit.name.lowercase()}", style = MaterialTheme.typography.bodyMedium, color = Secondary, modifier = Modifier.padding(vertical = 4.dp))
                    }
                    state.personalRecords.firstOrNull { it.exerciseId == item.exercise.id }?.let { record ->
                        val status = when (record.status) {
                            PersonalRecordStatus.BASELINE -> "Catatan awal"
                            PersonalRecordStatus.NEW_RECORD -> "Rekor baru"
                            PersonalRecordStatus.MATCHED -> "Menyamai rekor"
                            PersonalRecordStatus.BELOW_RECORD -> "Belum melampaui rekor"
                        }
                        Text("$status · ${displayNumber(displayWeight(record.currentMaxKg, weightUnit))} ${weightUnit.name.lowercase()}" + (record.previousMaxKg?.let { " · sebelumnya ${displayNumber(displayWeight(it, weightUnit))} ${weightUnit.name.lowercase()}" } ?: ""), style = MaterialTheme.typography.bodyMedium, color = Accent, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onResume, enabled = !state.busy) { Text("Perbaiki sesi") }
                TextButton(onClick = { confirmDiscard = true }, enabled = !state.busy) { Text("Buang sesi", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false },
        title = { Text("Buang sesi?") },
        text = { Text("Sesi dan semua set di dalam draft akan dihapus permanen.") },
        confirmButton = { TextButton(onClick = { confirmDiscard = false; onDiscard() }, enabled = !state.busy) { Text("Buang sesi") } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Kembali") } },
    )
}

@Composable
private fun SummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Surface).border(1.dp, Border, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 18.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = Ink, maxLines = 1)
        Spacer(Modifier.height(10.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Secondary)
    }
}

private fun summaryVolume(value: java.math.BigDecimal): String =
    if (value >= java.math.BigDecimal("10000")) {
        "${displayNumber(value.divide(java.math.BigDecimal("1000"), 1, java.math.RoundingMode.HALF_UP))}k"
    } else displayNumber(value)
