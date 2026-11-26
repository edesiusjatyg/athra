package id.ac.ub.athra.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.ac.ub.athra.domain.calculation.displayNumber
import id.ac.ub.athra.domain.calculation.displayWeight
import id.ac.ub.athra.domain.model.*
import id.ac.ub.athra.ui.components.*
import id.ac.ub.athra.ui.theme.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val IndonesianLocale = Locale.forLanguageTag("id-ID")

@Composable
fun HomeScreen(
    userName: String,
    draft: WorkoutDraft?,
    recentSessions: List<WorkoutSession>,
    analytics: AnalyticsSummary?,
    onStartWorkout: () -> Unit = {},
    onResumeWorkout: () -> Unit = {},
    onViewAll: () -> Unit = {},
    onNavigate: (Int) -> Unit = {},
    onOpenSession: (String) -> Unit = {},
    weightUnit: WeightUnit = WeightUnit.KG,
    loading: Boolean = false,
    draftLoading: Boolean = false,
    draftError: String? = null,
    error: String? = null,
    onRetry: () -> Unit = {},
) {
    Scaffold(containerColor = Background, bottomBar = { AthraBottomNav(0, onNavigate) }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(12.dp))
            AthraBrandHeader(onProfile = { onNavigate(3) })
            Spacer(Modifier.height(16.dp))
            val today = DateTimeFormatter.ofPattern("EEEE, d MMMM", IndonesianLocale)
                .format(Instant.now().atZone(ZoneId.systemDefault()))
            Text(today.uppercase(IndonesianLocale), style = MaterialTheme.typography.labelSmall, color = Secondary)
            Spacer(Modifier.height(8.dp))
            Text("Halo, $userName", style = MaterialTheme.typography.headlineLarge, color = Ink)
            Spacer(Modifier.height(4.dp))
            Text("Jaga semangat latihanmu.", style = MaterialTheme.typography.bodyMedium, color = Secondary)
            Spacer(Modifier.height(18.dp))

            val hasContent = draft != null || analytics != null || recentSessions.isNotEmpty()
            if (loading && !hasContent) {
                Box(Modifier.fillMaxWidth().height(144.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (error != null && !hasContent) {
                Text(error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text("Coba lagi") }
            } else {
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry) { Text("Coba lagi") }
                }
                NextWorkoutCard(draft, draftLoading, draftError)
                Spacer(Modifier.height(18.dp))
                AthraPrimaryButton(
                    label = when {
                        draftLoading -> "Memuat…"
                        draftError != null -> "Coba lagi"
                        draft != null -> "Lanjutkan sesi"
                        else -> "Mulai workout"
                    },
                    onClick = when {
                        draftError != null -> onRetry
                        draft != null -> onResumeWorkout
                        else -> onStartWorkout
                    },
                    enabled = !draftLoading,
                )
                Spacer(Modifier.height(26.dp))
                ThisWeekSection(analytics, weightUnit)
                Spacer(Modifier.height(24.dp))
                SectionHeader("Sesi terbaru", "Lihat semua", onViewAll)
                Spacer(Modifier.height(8.dp))
                if (recentSessions.isEmpty()) {
                    AthraCard {
                        Text("Belum ada sesi latihan", style = MaterialTheme.typography.titleSmall, color = Ink)
                        Spacer(Modifier.height(4.dp))
                        Text("Mulai workout pertamamu untuk melihat riwayat di sini.", style = MaterialTheme.typography.bodyMedium, color = Secondary)
                    }
                } else recentSessions.take(3).forEach { session ->
                    RecentSessionRow(session, onClick = { onOpenSession(session.id) })
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NextWorkoutCard(draft: WorkoutDraft?, loading: Boolean, error: String?) {
    Box(Modifier.fillMaxWidth().heightIn(min = 144.dp).clip(RoundedCornerShape(20.dp)).background(Forest)) {
        Canvas(Modifier.align(Alignment.TopEnd).size(144.dp)) {
            val center = Offset(size.width * 1.04f, size.height * .32f)
            listOf(34.dp, 60.dp, 88.dp).forEach { radius ->
                drawCircle(Secondary.copy(alpha = .5f), radius.toPx(), center, style = Stroke(1.dp.toPx()))
            }
        }
        Column(Modifier.padding(20.dp)) {
            Text(
                when {
                    loading -> "MEMERIKSA SESI"
                    error != null -> "SESI BELUM TERBACA"
                    draft != null -> "SESI BELUM SELESAI"
                    else -> "LATIHAN BERIKUTNYA"
                },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = Accent,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                when {
                    loading -> "Memuat sesi tersimpan"
                    error != null -> "Periksa sesi tersimpan"
                    draft != null -> draft.session.name
                    else -> "Siap berlatih?"
                },
                style = MaterialTheme.typography.headlineMedium, color = Ink,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    loading -> "Memeriksa draft latihanmu."
                    error != null -> "Muat ulang untuk memeriksa draft."
                    draft != null -> "${draft.exercises.size} gerakan  ·  ${draft.exercises.sumOf { it.sets.size }} set tersimpan"
                    else -> "Mulai sesi  ·  Pilih gerakanmu"
                }, style = MaterialTheme.typography.bodyMedium, color = Secondary,
            )
        }
    }
}

@Composable
private fun ThisWeekSection(analytics: AnalyticsSummary?, weightUnit: WeightUnit) {
    val formatter = DateTimeFormatter.ofPattern("d MMM", IndonesianLocale)
    val range = analytics?.let {
        val zone = ZoneId.systemDefault()
        val first = Instant.ofEpochMilli(it.startEpochMs).atZone(zone)
        val last = Instant.ofEpochMilli(it.endEpochMs - 1).atZone(zone)
        "${formatter.format(first)} – ${formatter.format(last)}".uppercase(IndonesianLocale)
    }.orEmpty()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Minggu ini", style = MaterialTheme.typography.headlineSmall, color = Ink, modifier = Modifier.weight(1f))
        Text(range, style = MaterialTheme.typography.labelSmall, color = Secondary)
    }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard("%02d".format(analytics?.sessionCount ?: 0), "Sesi", Modifier.weight(1f))
        StatCard("%02d".format(analytics?.setCount ?: 0), "Set", Modifier.weight(1f))
        val volume = analytics?.volumeKg?.let { displayWeight(it, weightUnit) } ?: BigDecimal.ZERO
        StatCard(compactVolume(volume), "Volume · ${weightUnit.name.lowercase()}", Modifier.weight(1f))
    }
}

private fun compactVolume(value: BigDecimal): String = if (value >= BigDecimal("10000")) {
    "${displayNumber(value.divide(BigDecimal("1000"), 1, RoundingMode.HALF_UP))}k"
} else displayNumber(value)

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier) {
    Column(modifier.heightIn(min = 104.dp).clip(CardShape).background(Surface).padding(14.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = Ink)
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Secondary)
    }
}

@Composable
private fun RecentSessionRow(session: WorkoutSession, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(CardShape).background(Surface).border(1.dp, Border, CardShape)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(Forest), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.FitnessCenter, null, tint = Accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(session.name, style = MaterialTheme.typography.titleSmall, color = Ink)
            Spacer(Modifier.height(4.dp))
            val date = DateTimeFormatter.ofPattern("d MMM", IndonesianLocale).format(
                Instant.ofEpochMilli(session.endedAt ?: session.startedAt).atZone(ZoneId.systemDefault()),
            )
            val minutes = ((session.endedAt ?: session.startedAt) - session.startedAt).coerceAtLeast(0) / 60_000
            Text("$date · $minutes menit", style = MaterialTheme.typography.labelSmall, color = Secondary)
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = Secondary, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun AthraBottomNav(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        "Beranda" to Icons.Outlined.Home,
        "Latihan" to Icons.Outlined.FitnessCenter,
        "Progress" to Icons.AutoMirrored.Outlined.ShowChart,
        "Profil" to Icons.Outlined.PersonOutline,
    )
    Column(Modifier.fillMaxWidth().background(Color(0xFF121A15)).navigationBarsPadding()) {
        HorizontalDivider(color = Border, thickness = 1.dp)
        Row(Modifier.fillMaxWidth().height(72.dp).selectableGroup()) {
            items.forEachIndexed { index, (label, icon) ->
                Column(
                    Modifier.weight(1f).fillMaxHeight().selectable(
                        selected = selected == index, role = Role.Tab, onClick = { onSelect(index) },
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.width(26.dp).height(3.dp).clip(CircleShape).background(if (selected == index) Accent else Color.Transparent))
                    Spacer(Modifier.height(12.dp))
                    Icon(icon, contentDescription = null, tint = if (selected == index) Forest else Secondary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(5.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (selected == index) FontWeight.SemiBold else FontWeight.Normal), color = if (selected == index) Accent else Secondary)
                }
            }
        }
    }
}
