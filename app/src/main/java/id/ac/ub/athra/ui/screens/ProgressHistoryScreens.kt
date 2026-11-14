package id.ac.ub.athra.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import id.ac.ub.athra.domain.calculation.displayNumber
import id.ac.ub.athra.domain.model.AnalyticsSummary
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
    onPeriodChange: (Int) -> Unit,
    onNavigate: (Int) -> Unit,
) {
    Scaffold(
        containerColor = Background,
        bottomBar = { AthraBottomNav(selected = 2, onSelect = onNavigate) },
        topBar = { TopAppBar(title = { Text("Progress") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)) },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(20.dp)) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("Sesi", analytics?.sessionCount?.toString() ?: "0", Modifier.weight(1f))
                Metric("Set", analytics?.setCount?.toString() ?: "0", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("Repetisi", analytics?.repCount?.toString() ?: "0", Modifier.weight(1f))
                Metric("Volume", "${analytics?.volumeKg?.let(::displayNumber) ?: "0"} kg", Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
            Text("Volume harian", style = MaterialTheme.typography.titleLarge, color = Ink)
            Spacer(Modifier.height(10.dp))
            if (analytics == null || analytics.sessionCount == 0) {
                Text("Belum ada data latihan", color = Secondary, modifier = Modifier.padding(vertical = 24.dp))
            } else {
                val maxValue = analytics.dailyVolumeKg.maxOfOrNull { it.second }?.toFloat()?.coerceAtLeast(1f) ?: 1f
                Canvas(Modifier.fillMaxWidth().height(180.dp).background(Surface, RoundedCornerShape(16.dp)).padding(12.dp)) {
                    val gap = 4.dp.toPx()
                    val barWidth = ((size.width - gap * (analytics.dailyVolumeKg.size - 1)) / analytics.dailyVolumeKg.size).coerceAtLeast(1f)
                    analytics.dailyVolumeKg.forEachIndexed { index, entry ->
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
                analytics?.favoriteExercise?.let { "${it.name} · ${analytics.favoriteSessionCount} sesi" } ?: "Belum ada data",
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
fun HistoryScreen(sessions: List<WorkoutSession>, onNavigate: (Int) -> Unit, onOpen: (String) -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale("id", "ID")) }
    Scaffold(
        containerColor = Background,
        bottomBar = { AthraBottomNav(selected = 1, onSelect = onNavigate) },
        topBar = { TopAppBar(title = { Text("Riwayat latihan") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)) },
    ) { insets ->
        if (sessions.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.Center) { Text("Belum ada sesi latihan.", color = Secondary) }
        } else Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(20.dp)) {
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
