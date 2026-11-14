package id.ac.ub.athra.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.unit.dp
import id.ac.ub.athra.domain.calculation.displayNumber
import id.ac.ub.athra.domain.calculation.calculateTotals
import id.ac.ub.athra.domain.calculation.displayWeight
import id.ac.ub.athra.domain.model.AnalyticsSummary
import id.ac.ub.athra.domain.model.CompletedWorkout
import id.ac.ub.athra.domain.model.PersonalRecordStatus
import id.ac.ub.athra.domain.model.WeightUnit
import id.ac.ub.athra.domain.model.WorkoutSession
import id.ac.ub.athra.ui.components.AthraCard
import id.ac.ub.athra.ui.theme.Accent
import id.ac.ub.athra.ui.theme.Background
import id.ac.ub.athra.ui.theme.Ink
import id.ac.ub.athra.ui.theme.Secondary
import id.ac.ub.athra.ui.theme.Surface
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ProgressScreen(
    analytics: AnalyticsSummary?,
    periodDays: Int,
    weightUnit: WeightUnit,
    onPeriodChange: (Int) -> Unit,
    onNavigate: (Int) -> Unit,
    loading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
) {
    Scaffold(
        containerColor = Background,
        bottomBar = { AthraBottomNav(selected = 2, onSelect = onNavigate) },
        topBar = { TopAppBar(title = { Text("Progress") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)) },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(20.dp)) {
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) { Text("Coba lagi") }
                Spacer(Modifier.height(12.dp))
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(7, 30, 90).forEachIndexed { index, days ->
                    SegmentedButton(
                        selected = periodDays == days,
                        onClick = { onPeriodChange(days) },
                        shape = SegmentedButtonDefaults.itemShape(index, 3),
                    ) { Text("$days hari") }
                }
            }
            Spacer(Modifier.height(20.dp))
            val metricPlaceholder = if (loading || error != null) "—" else "0"
            if (LocalDensity.current.fontScale >= 1.5f) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Metric("Sesi", analytics?.sessionCount?.toString() ?: metricPlaceholder, Modifier.fillMaxWidth())
                    Metric("Set", analytics?.setCount?.toString() ?: metricPlaceholder, Modifier.fillMaxWidth())
                    Metric("Repetisi", analytics?.repCount?.toString() ?: metricPlaceholder, Modifier.fillMaxWidth())
                    Metric("Volume", "${analytics?.volumeKg?.let { displayNumber(displayWeight(it, weightUnit)) } ?: metricPlaceholder} ${weightUnit.name.lowercase()}", Modifier.fillMaxWidth())
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Metric("Sesi", analytics?.sessionCount?.toString() ?: metricPlaceholder, Modifier.weight(1f))
                    Metric("Set", analytics?.setCount?.toString() ?: metricPlaceholder, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Metric("Repetisi", analytics?.repCount?.toString() ?: metricPlaceholder, Modifier.weight(1f))
                    Metric("Volume", "${analytics?.volumeKg?.let { displayNumber(displayWeight(it, weightUnit)) } ?: metricPlaceholder} ${weightUnit.name.lowercase()}", Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Volume harian", style = MaterialTheme.typography.titleLarge, color = Ink)
            Spacer(Modifier.height(10.dp))
            if (loading && analytics == null) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (analytics == null && error != null) {
                Text("Data latihan belum dapat dimuat.", color = Secondary, modifier = Modifier.padding(vertical = 24.dp))
            } else if (analytics == null || analytics.sessionCount == 0) {
                Text("Belum ada data latihan", color = Secondary, modifier = Modifier.padding(vertical = 24.dp))
            } else {
                val maxValue = analytics.dailyVolumeKg.maxOfOrNull { it.second }?.toFloat()?.coerceAtLeast(1f) ?: 1f
                val chartDescription = analytics.dailyVolumeKg.joinToString { (day, value) ->
                    "${Instant.ofEpochMilli(day).atZone(ZoneId.systemDefault()).toLocalDate()}: ${displayNumber(displayWeight(value, weightUnit))} ${weightUnit.name.lowercase()}"
                }
                Canvas(Modifier.fillMaxWidth().height(180.dp).background(Surface, RoundedCornerShape(16.dp)).padding(12.dp).semantics { contentDescription = chartDescription }) {
                    val gap = if (analytics.dailyVolumeKg.size > 45) 0f else 4.dp.toPx()
                    val barWidth = ((size.width - gap * (analytics.dailyVolumeKg.size - 1)) / analytics.dailyVolumeKg.size).coerceAtLeast(0.5f)
                    analytics.dailyVolumeKg.forEachIndexed { index, entry ->
                        if (entry.second.signum() == 0) return@forEachIndexed
                        val height = size.height * (entry.second.toFloat() / maxValue)
                        drawRoundRect(
                            color = Accent,
                            topLeft = androidx.compose.ui.geometry.Offset(index * (barWidth + gap), size.height - height),
                            size = androidx.compose.ui.geometry.Size(barWidth, height.coerceAtLeast(2.dp.toPx())),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Gerakan favorit", style = MaterialTheme.typography.titleLarge, color = Ink)
            Spacer(Modifier.height(8.dp))
            Text(
                analytics?.favoriteExercise?.let { "${it.name} · ${analytics.favoriteSessionCount} sesi" }
                    ?: if (loading || error != null) "—" else "Belum ada data",
                color = Secondary,
            )
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    AthraCard(modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = Ink)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Secondary)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HistoryScreen(
    sessions: List<WorkoutSession>,
    onNavigate: (Int) -> Unit,
    onOpen: (String) -> Unit,
    loading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
) {
    val formatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale("id", "ID")) }
    Scaffold(
        containerColor = Background,
        bottomBar = { AthraBottomNav(selected = 1, onSelect = onNavigate) },
        topBar = { TopAppBar(title = { Text("Riwayat latihan") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)) },
    ) { insets ->
        if (loading && sessions.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (sessions.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(insets).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(error ?: "Belum ada sesi latihan.", color = Secondary)
                if (error != null) TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) { Text("Coba lagi") }
            }
        } else Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(20.dp)) {
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) { Text("Coba lagi") }
            }
            sessions.forEach { session ->
                TextButton(onClick = { onOpen(session.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(session.name, color = Ink, style = MaterialTheme.typography.titleMedium)
                        Text(formatter.format(Instant.ofEpochMilli(session.endedAt ?: session.startedAt).atZone(ZoneId.systemDefault())), color = Secondary)
                    }
                }
                HorizontalDivider(color = Color.DarkGray)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun WorkoutDetailScreen(
    workout: CompletedWorkout?,
    weightUnit: WeightUnit,
    onBack: () -> Unit,
    loading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
) {
    Scaffold(
        containerColor = Background,
        topBar = { TopAppBar(title = { Text("Detail latihan") }, navigationIcon = { TextButton(onClick = onBack) { Text("Kembali") } }) },
    ) { insets ->
        val data = workout
        if (loading && data == null) Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        } else if (data == null) Column(
            Modifier.fillMaxSize().padding(insets).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(error ?: "Detail sesi tidak tersedia.", color = Secondary)
            if (error != null) TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) { Text("Coba lagi") }
        } else Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(20.dp)) {
            val draft = data.draft
            val totals = calculateTotals(draft)
            Text(draft.session.name, style = MaterialTheme.typography.headlineMedium, color = Ink)
            val durationSeconds = (((draft.session.endedAt ?: draft.session.startedAt) - draft.session.startedAt).coerceAtLeast(0) / 1_000)
            Text("Durasi %02d:%02d:%02d".format(durationSeconds / 3_600, durationSeconds % 3_600 / 60, durationSeconds % 60), color = Secondary)
            val completedAt = Instant.ofEpochMilli(draft.session.endedAt ?: draft.session.startedAt).atZone(ZoneId.systemDefault())
            Text(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale("id", "ID")).format(completedAt), color = Secondary)
            Text("${totals.setCount} set · ${totals.repCount} repetisi · ${displayNumber(displayWeight(totals.volumeKg, weightUnit))} ${weightUnit.name.lowercase()}", color = Secondary)
            Spacer(Modifier.height(20.dp))
            draft.exercises.forEach { item ->
                Text(item.exercise.name, style = MaterialTheme.typography.titleLarge, color = Ink)
                item.sets.forEach { set ->
                    Text("Set ${set.number} · ${set.reps} repetisi · ${displayNumber(displayWeight(set.weightKg, weightUnit))} ${weightUnit.name.lowercase()}")
                }
                data.personalRecords.firstOrNull { it.exerciseId == item.exercise.id }?.let { record ->
                    val status = when (record.status) {
                        PersonalRecordStatus.BASELINE -> "Catatan awal"
                        PersonalRecordStatus.NEW_RECORD -> "Rekor baru"
                        PersonalRecordStatus.MATCHED -> "Menyamai rekor"
                        PersonalRecordStatus.BELOW_RECORD -> "Belum melampaui rekor"
                    }
                    Text("$status · ${displayNumber(displayWeight(record.currentMaxKg, weightUnit))} ${weightUnit.name.lowercase()}", color = Accent)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PreferencesScreen(initialUnit: id.ac.ub.athra.domain.model.WeightUnit, onBack: () -> Unit, onSave: (id.ac.ub.athra.domain.model.WeightUnit) -> Unit) {
    var selected by remember(initialUnit) { mutableStateOf(initialUnit) }
    Scaffold(
        containerColor = Background,
        topBar = { TopAppBar(title = { Text("Preferensi") }, navigationIcon = { TextButton(onClick = onBack) { Text("Batal") } }) },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).padding(20.dp)) {
            Text("Satuan beban", style = MaterialTheme.typography.titleLarge, color = Ink)
            Spacer(Modifier.height(8.dp))
            listOf(id.ac.ub.athra.domain.model.WeightUnit.KG, id.ac.ub.athra.domain.model.WeightUnit.LB).forEach { unit ->
                Row(
                    modifier = Modifier.fillMaxWidth().selectable(
                        selected = selected == unit,
                        onClick = { selected = unit },
                        role = Role.RadioButton,
                    ).heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected == unit, onClick = null)
                    Text(if (unit == id.ac.ub.athra.domain.model.WeightUnit.KG) "Kilogram (kg)" else "Pound (lb)")
                }
            }
            Text("Contoh: 32 kg = 70.55 lb", color = Secondary, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.weight(1f))
            Button(onClick = { onSave(selected) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Simpan") }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EditProfileScreen(
    initialName: String,
    email: String,
    error: String?,
    onBack: () -> Unit,
    onSave: (String) -> Unit,
    busy: Boolean = false,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val focusManager = LocalFocusManager.current
    Scaffold(
        containerColor = Background,
        topBar = { TopAppBar(title = { Text("Edit profil") }, navigationIcon = { TextButton(onClick = onBack) { Text("Batal") } }) },
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(value = email, onValueChange = {}, label = { Text("Email") }, readOnly = true, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            Button(
                onClick = { focusManager.clearFocus(); onSave(name.trim()) },
                enabled = name.isNotBlank() && !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                if (busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Simpan")
            }
        }
    }
}
