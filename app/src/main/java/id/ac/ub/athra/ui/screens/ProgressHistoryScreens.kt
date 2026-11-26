package id.ac.ub.athra.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ac.ub.athra.domain.calculation.*
import id.ac.ub.athra.domain.model.*
import id.ac.ub.athra.ui.components.*
import id.ac.ub.athra.ui.theme.*
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProgressScreen(
    analytics: AnalyticsSummary?, periodDays: Int, weightUnit: WeightUnit,
    onPeriodChange: (Int) -> Unit, onNavigate: (Int) -> Unit,
    loading: Boolean = false, error: String? = null, onRetry: () -> Unit = {},
) {
    var chartPage by remember(analytics, periodDays) { mutableIntStateOf(Int.MAX_VALUE) }
    val unit = weightUnit.name.lowercase()
    val daily = analytics?.dailyVolumeKg.orEmpty()
    val daysPerPage = 8
    val pageCount = ((daily.size + daysPerPage - 1) / daysPerPage).coerceAtLeast(1)
    val page = chartPage.coerceIn(0, pageCount - 1)
    val pageEnd = (daily.size - (pageCount - 1 - page) * daysPerPage).coerceAtLeast(0)
    val visibleDays = daily.subList((pageEnd - daysPerPage).coerceAtLeast(0), pageEnd)
    Scaffold(containerColor = Background, bottomBar = { AthraBottomNav(2, onNavigate) }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(12.dp))
            AthraBrandHeader(onProfile = { onNavigate(3) })
            Spacer(Modifier.height(16.dp))
            Text("Progres latihanmu", style = MaterialTheme.typography.headlineLarge, color = Ink)
            Spacer(Modifier.height(8.dp))
            Text("Bangun kekuatan lewat konsistensi.", style = MaterialTheme.typography.bodyLarge, color = Secondary)
            Spacer(Modifier.height(26.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 30, 90).forEach { days ->
                    FilterChip(
                        selected = periodDays == days, onClick = { onPeriodChange(days) },
                        modifier = Modifier.semantics { contentDescription = "$days hari" },
                        label = { Text("${days}H", fontWeight = FontWeight.SemiBold) },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(containerColor = Surface, selectedContainerColor = Forest, labelColor = Secondary, selectedLabelColor = Ink),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = periodDays == days, borderColor = Border, selectedBorderColor = Forest),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            val placeholder = if (loading || error != null) "—" else "0"
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProgressMetric("Sesi", analytics?.sessionCount?.toString() ?: placeholder, "$periodDays hari terakhir", Modifier.weight(1f))
                ProgressMetric("Volume · $unit", analytics?.volumeKg?.let { compactNumber(displayWeight(it, weightUnit)) } ?: placeholder, "Total beban terangkat", Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
            Column(Modifier.fillMaxWidth().clip(CardShape).background(Surface).border(1.dp, Border, CardShape).padding(16.dp)) {
                Text("Volume latihan", style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(Modifier.height(4.dp))
                Text("$periodDays hari terakhir · beban ($unit)", style = MaterialTheme.typography.bodySmall, color = Secondary)
                Spacer(Modifier.height(4.dp))
                if (loading && analytics == null) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (analytics == null || analytics.sessionCount == 0) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { Text(if (error != null) "Data belum dapat dimuat" else "Belum ada data latihan", color = Secondary, style = MaterialTheme.typography.bodyMedium) }
                } else {
                    val max = daily.maxOfOrNull { it.second }?.toFloat()?.coerceAtLeast(1f) ?: 1f
                    val description = visibleDays.joinToString { (day, value) -> "${dayLabel(day)}: ${displayNumber(displayWeight(value, weightUnit))} $unit" }
                    Canvas(Modifier.fillMaxWidth().height(120.dp).semantics { contentDescription = description }) {
                        val columnWidth = size.width / visibleDays.size.coerceAtLeast(1)
                        val width = (columnWidth - 14.dp.toPx()).coerceAtLeast(1f)
                        visibleDays.forEachIndexed { index, (_, value) ->
                            val height = (size.height * value.toFloat() / max).coerceAtLeast(4.dp.toPx())
                            val x = index * columnWidth + (columnWidth - width) / 2
                            drawRoundRect(if (value.signum() > 0) Accent else Forest, Offset(x, size.height - height), Size(width, height), CornerRadius(6.dp.toPx()))
                        }
                        drawLine(Border, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceAround) {
                        visibleDays.forEach { (day, _) -> Text(dayLabel(day), color = Secondary, style = MaterialTheme.typography.labelSmall) }
                    }
                }
                Row(Modifier.fillMaxWidth().height(24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { chartPage = page - 1 }, enabled = page > 0) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Hari sebelumnya", tint = if (page > 0) Accent else Forest, modifier = Modifier.size(20.dp)) }
                    IconButton(onClick = { chartPage = page + 1 }, enabled = page < pageCount - 1) { Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Hari berikutnya", tint = if (page < pageCount - 1) Accent else Forest, modifier = Modifier.size(20.dp)) }
                }
            }
            Spacer(Modifier.height(20.dp))
            Column(Modifier.fillMaxWidth().clip(CardShape).background(Surface).border(1.dp, Border, CardShape).padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Gerakan favorit", style = MaterialTheme.typography.headlineSmall, color = Ink)
                    Text("${analytics?.favoriteSessionCount ?: 0} sesi", style = MaterialTheme.typography.labelSmall, color = Secondary)
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).background(Forest, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.FitnessCenter, null, tint = Accent, modifier = Modifier.size(18.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(analytics?.favoriteExercise?.name ?: "Belum ada data", color = Ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text("${analytics?.setCount ?: 0} set · ${analytics?.repCount ?: 0} repetisi", color = Secondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (error != null) { Spacer(Modifier.height(12.dp)); Text(error, color = MaterialTheme.colorScheme.error); TextButton(onClick = onRetry) { Text("Coba lagi") } }
            Spacer(Modifier.height(20.dp))
        }
    }
}

private fun dayLabel(epoch: Long) = Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).dayOfMonth.toString()

private fun compactNumber(value: BigDecimal): String = if (value.abs() >= BigDecimal(10000)) "${displayNumber(value.divide(BigDecimal(1000)))}k" else displayNumber(value)

@Composable
private fun ProgressMetric(label: String, value: String, detail: String, modifier: Modifier) {
    Column(modifier.clip(CardShape).background(Surface).border(1.dp, Border, CardShape).padding(14.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Secondary)
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = Ink)
        Spacer(Modifier.height(10.dp))
        Text(detail, style = MaterialTheme.typography.labelSmall, color = Accent)
    }
}

@Composable
fun HistoryScreen(sessions: List<WorkoutSession>, onNavigate: (Int) -> Unit, onOpen: (String) -> Unit, loading: Boolean = false, error: String? = null, onRetry: () -> Unit = {}) {
    val formatter = remember { DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.forLanguageTag("id-ID")) }
    Scaffold(
        containerColor = Background,
        topBar = { AthraScreenHeader("Riwayat latihan", { onNavigate(0) }) },
        bottomBar = { AthraPrimaryButton("Kembali ke beranda", { onNavigate(0) }, Modifier.navigationBarsPadding().padding(24.dp)) },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(20.dp))
            Text("Riwayat latihan", style = MaterialTheme.typography.headlineLarge, color = Ink)
            Spacer(Modifier.height(10.dp))
            Text("Sesi yang telah kamu selesaikan", style = MaterialTheme.typography.bodyLarge, color = Secondary)
            Spacer(Modifier.height(32.dp))
            if (loading && sessions.isEmpty()) CircularProgressIndicator()
            if (error != null) { Text(error, color = MaterialTheme.colorScheme.error); TextButton(onClick = onRetry) { Text("Coba lagi") } }
            if (!loading && sessions.isEmpty() && error == null) Text("Belum ada sesi latihan.", color = Secondary)
            sessions.forEach { session ->
                Row(Modifier.fillMaxWidth().clip(CardShape).background(Surface).clickable { onOpen(session.id) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(session.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Ink)
                        Spacer(Modifier.height(8.dp))
                        val duration = ((session.endedAt ?: session.startedAt) - session.startedAt).coerceAtLeast(0) / 60000
                        Text("${formatter.format(Instant.ofEpochMilli(session.endedAt ?: session.startedAt).atZone(ZoneId.systemDefault()))} · $duration mnt", style = MaterialTheme.typography.bodyMedium, color = Secondary)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = Accent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun WorkoutDetailScreen(workout: CompletedWorkout?, weightUnit: WeightUnit, onBack: () -> Unit, loading: Boolean = false, error: String? = null, onRetry: () -> Unit = {}) {
    Scaffold(containerColor = Background, topBar = { AthraScreenHeader("Detail latihan", onBack) }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(20.dp))
            if (loading && workout == null) CircularProgressIndicator()
            else if (workout == null) { Text(error ?: "Detail sesi tidak tersedia.", color = Secondary); if (error != null) TextButton(onClick = onRetry) { Text("Coba lagi") } }
            else {
                val draft = workout.draft
                val totals = calculateTotals(draft)
                Text(draft.session.name, style = MaterialTheme.typography.headlineLarge, color = Ink)
                Spacer(Modifier.height(10.dp))
                val seconds = (((draft.session.endedAt ?: draft.session.startedAt) - draft.session.startedAt).coerceAtLeast(0) / 1000)
                val completedAt = Instant.ofEpochMilli(draft.session.endedAt ?: draft.session.startedAt).atZone(ZoneId.systemDefault())
                Text(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale.forLanguageTag("id-ID")).format(completedAt), color = Secondary, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(26.dp))
                AthraCard {
                    Text("Ringkasan sesi", style = MaterialTheme.typography.headlineSmall, color = Ink)
                    Spacer(Modifier.height(12.dp))
                    Text("Durasi %02d:%02d:%02d".format(seconds / 3600, seconds % 3600 / 60, seconds % 60), color = Ink)
                    Text("${totals.setCount} set · ${totals.repCount} repetisi · ${displayNumber(displayWeight(totals.volumeKg, weightUnit))} ${weightUnit.name.lowercase()}", color = Secondary, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(24.dp))
                draft.exercises.forEach { item ->
                    AthraCard {
                        Text(item.exercise.name, style = MaterialTheme.typography.headlineSmall, color = Ink)
                        Spacer(Modifier.height(6.dp))
                        Text("${item.exercise.primaryMuscleGroup} · ${item.exercise.equipment}", style = MaterialTheme.typography.bodyMedium, color = Secondary)
                        Spacer(Modifier.height(14.dp))
                        item.sets.forEach { set ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Set ${set.number}", style = MaterialTheme.typography.bodyMedium, color = Secondary)
                                Text("${set.reps} × ${displayNumber(displayWeight(set.weightKg, weightUnit))} ${weightUnit.name.lowercase()}", style = MaterialTheme.typography.bodyMedium, color = Ink)
                            }
                        }
                        workout.personalRecords.firstOrNull { it.exerciseId == item.exercise.id }?.let { record ->
                            val status = when (record.status) {
                                PersonalRecordStatus.BASELINE -> "Catatan awal"
                                PersonalRecordStatus.NEW_RECORD -> "Rekor baru"
                                PersonalRecordStatus.MATCHED -> "Menyamai rekor"
                                PersonalRecordStatus.BELOW_RECORD -> "Belum melampaui rekor"
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("$status · ${displayNumber(displayWeight(record.currentMaxKg, weightUnit))} ${weightUnit.name.lowercase()}", color = Accent, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun PreferencesScreen(initialUnit: WeightUnit, onBack: () -> Unit, onSave: (WeightUnit) -> Unit) {
    var selected by rememberSaveable(initialUnit) { mutableStateOf(initialUnit) }
    Scaffold(
        containerColor = Background, topBar = { AthraScreenHeader("Preferensi", onBack) },
        bottomBar = { AthraPrimaryButton("Simpan preferensi", { onSave(selected) }, Modifier.navigationBarsPadding().padding(24.dp)) },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(20.dp))
            Text("Preferensi", style = MaterialTheme.typography.headlineLarge, color = Ink)
            Spacer(Modifier.height(10.dp))
            Text("Sesuaikan ATHRA untukmu", style = MaterialTheme.typography.bodyLarge, color = Secondary)
            Spacer(Modifier.height(32.dp))
            ReadOnlySetting("Tampilan", "Mode gelap")
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().clip(CardShape).background(Surface).padding(16.dp)) {
                Text("Satuan beban", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Ink)
                Spacer(Modifier.height(8.dp))
                listOf(WeightUnit.KG, WeightUnit.LB).forEach { unit ->
                    Row(Modifier.fillMaxWidth().selectable(selected == unit, onClick = { selected = unit }, role = Role.RadioButton).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected == unit, onClick = null)
                        Text(if (unit == WeightUnit.KG) "Kilogram (kg)" else "Pound (lb)", style = MaterialTheme.typography.bodyMedium, color = if (selected == unit) Ink else Secondary)
                    }
                }
                Text("Contoh: 32 kg = 70.55 lb", color = Secondary, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
            ReadOnlySetting("Privasi dan data", "Riwayat latihan disimpan di perangkat ini")
        }
    }
}

@Composable
private fun ReadOnlySetting(title: String, value: String) {
    Column(Modifier.fillMaxWidth().clip(CardShape).background(Surface).padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Ink)
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Secondary)
    }
}

@Composable
fun EditProfileScreen(initialName: String, email: String, error: String?, onBack: () -> Unit, onSave: (String) -> Unit, busy: Boolean = false) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    val focus = LocalFocusManager.current
    Scaffold(
        containerColor = Background, topBar = { AthraScreenHeader("Edit profil", onBack) },
        bottomBar = { AthraPrimaryButton(if (busy) "Menyimpan…" else "Simpan profil", { focus.clearFocus(); onSave(name.trim()) }, Modifier.navigationBarsPadding().imePadding().padding(24.dp), enabled = name.isNotBlank() && !busy) },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(20.dp))
            Text("Edit profil", style = MaterialTheme.typography.headlineLarge, color = Ink)
            Spacer(Modifier.height(10.dp))
            Text("Informasi pribadi", style = MaterialTheme.typography.bodyLarge, color = Secondary)
            Spacer(Modifier.height(32.dp))
            Column(Modifier.fillMaxWidth().clip(CardShape).background(Surface).padding(16.dp)) {
                Text("Nama tampilan", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Ink)
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = name, onValueChange = { name = it }, singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Secondary),
                    cursorBrush = SolidColor(Accent),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Nama tampilan" },
                    decorationBox = { field ->
                        Box {
                            if (name.isEmpty()) Text("Nama", style = MaterialTheme.typography.bodyMedium, color = Secondary)
                            field()
                        }
                    },
                )
            }
            Spacer(Modifier.height(12.dp))
            ReadOnlySetting("Email", email)
            Spacer(Modifier.height(12.dp))
            ReadOnlySetting("Privasi", "Data latihan tersimpan di perangkat ini")
            if (error != null) { Spacer(Modifier.height(12.dp)); Text(error, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
fun ProfileScreen(
    userName: String, userEmail: String, analytics: AnalyticsSummary?, weightUnit: WeightUnit,
    loading: Boolean, error: String?, onRetry: () -> Unit,
    onNavigate: (Int) -> Unit, onEdit: () -> Unit, onPreferences: () -> Unit, onHistory: () -> Unit, onSignOut: () -> Unit,
    debugContent: @Composable ColumnScope.() -> Unit = {},
) {
    Scaffold(containerColor = Background, bottomBar = { AthraBottomNav(3, onNavigate) }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(16.dp))
            Text("ATHRA", style = MaterialTheme.typography.headlineMedium, color = Ink, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(30.dp))
            Text("Profil", style = MaterialTheme.typography.headlineLarge, color = Ink)
            Spacer(Modifier.height(8.dp))
            Text("Latihanmu, caramu.", style = MaterialTheme.typography.bodyLarge, color = Secondary)
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth().clip(CardShape).background(Surface).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(78.dp).background(Forest, CircleShape), contentAlignment = Alignment.Center) { Icon(AthraIcons.Profile, null, tint = Accent, modifier = Modifier.size(36.dp)) }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(userName, style = MaterialTheme.typography.headlineMedium, color = Ink)
                    Spacer(Modifier.height(6.dp))
                    Text("Anggota ATHRA", style = MaterialTheme.typography.bodyMedium, color = Secondary)
                    Spacer(Modifier.height(10.dp))
                    FilledTonalButton(onClick = onEdit, shape = RoundedCornerShape(50), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp), modifier = Modifier.heightIn(min = 32.dp), colors = ButtonDefaults.filledTonalButtonColors(containerColor = Forest, contentColor = Accent)) { Text("Edit profil", style = MaterialTheme.typography.labelSmall) }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileStat("${analytics?.sessionCount ?: 0}", "Sesi", Modifier.weight(1f))
                ProfileStat("${analytics?.setCount ?: 0}", "Set", Modifier.weight(1f))
                ProfileStat(analytics?.volumeKg?.let { volume ->
                    val displayed = displayWeight(volume, weightUnit)
                    if (displayed.abs() >= BigDecimal(10000)) "${displayed.divideToIntegralValue(BigDecimal(1000)).toBigInteger()}k" else displayNumber(displayed)
                } ?: "0", "Volume (${weightUnit.name.lowercase()})", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text("Statistik minggu ini", style = MaterialTheme.typography.labelSmall, color = Secondary)
            if (loading) CircularProgressIndicator(Modifier.size(24.dp))
            if (error != null) { Text(error, color = MaterialTheme.colorScheme.error); TextButton(onClick = onRetry) { Text("Coba lagi") } }
            Spacer(Modifier.height(24.dp))
            Text("Latihanmu", style = MaterialTheme.typography.headlineSmall, color = Ink)
            Spacer(Modifier.height(12.dp))
            ProfileAction(Icons.Outlined.History, "Riwayat latihan", "Lihat semua sesi tersimpan", onHistory)
            Spacer(Modifier.height(24.dp))
            Text("Akun & preferensi", style = MaterialTheme.typography.headlineSmall, color = Ink)
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().clip(CardShape).background(Surface)) {
                ProfileAction(Icons.Outlined.Scale, "Satuan beban", if (weightUnit == WeightUnit.KG) "Kilogram (kg)" else "Pound (lb)", onPreferences)
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Border)
                ProfileAction(Icons.Outlined.Logout, "Keluar", userEmail, onSignOut)
            }
            Spacer(Modifier.height(16.dp))
            Text("Data latihan tersimpan di perangkat ini.", style = MaterialTheme.typography.bodyMedium, color = Secondary)
            debugContent()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String, modifier: Modifier) {
    Column(modifier.height(90.dp).clip(CardShape).background(Surface).padding(12.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp), color = Ink, maxLines = 1)
        Spacer(Modifier.height(10.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Secondary, maxLines = 1)
    }
}

@Composable
private fun ProfileAction(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(CardShape).background(Surface).clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Secondary)
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = Secondary, modifier = Modifier.size(20.dp))
    }
}
