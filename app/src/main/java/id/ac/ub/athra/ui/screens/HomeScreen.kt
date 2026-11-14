package id.ac.ub.athra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ac.ub.athra.ui.components.*
import id.ac.ub.athra.ui.theme.*
import id.ac.ub.athra.domain.model.AnalyticsSummary
import id.ac.ub.athra.domain.model.WorkoutDraft
import id.ac.ub.athra.domain.model.WorkoutSession
import id.ac.ub.athra.domain.calculation.displayNumber
import id.ac.ub.athra.domain.calculation.displayWeight
import id.ac.ub.athra.domain.model.WeightUnit
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─── Screen ──────────────────────────────────────────────────────────────────

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
) {
    Scaffold(
        containerColor = Background,
        bottomBar = { AthraBottomNav(selected = 0, onSelect = onNavigate) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Top bar ──
            HomeTopBar(userName = userName, onOpenProfile = { onNavigate(3) })

            Spacer(Modifier.height(20.dp))

            // ── Greeting ──
            val today = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("id", "ID"))
                .format(Instant.now().atZone(ZoneId.systemDefault()))
            Text(text = today.uppercase(Locale("id", "ID")), style = MaterialTheme.typography.labelSmall.copy(color = Secondary))
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Halo, $userName",
                style = MaterialTheme.typography.headlineLarge.copy(color = Ink),
            )
            Text(
                text = "Latihanmu tersimpan di perangkat ini.",
                style = MaterialTheme.typography.bodyMedium.copy(color = Secondary),
            )

            Spacer(Modifier.height(20.dp))

            // ── Next workout card ──
            NextWorkoutCard(
                hasDraft = draft != null,
                onStartWorkout = if (draft != null) onResumeWorkout else onStartWorkout,
            )

            Spacer(Modifier.height(24.dp))

            // ── This week stats ──
            ThisWeekSection(analytics, weightUnit)

            Spacer(Modifier.height(24.dp))

            // ── Recent sessions ──
            SectionHeader(title = "Sesi terbaru", action = "Lihat semua", onAction = onViewAll)
            Spacer(Modifier.height(12.dp))
            if (recentSessions.isEmpty()) {
                Text("Belum ada sesi. Mulai latihan pertamamu.", color = Secondary)
            } else recentSessions.take(3).forEach { session ->
                RecentSessionRow(
                    sessionId = session.id,
                    name = session.name,
                    meta = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id", "ID"))
                        .format(Instant.ofEpochMilli(session.endedAt ?: session.startedAt).atZone(ZoneId.systemDefault())),
                    onClick = { onOpenSession(session.id) },
                )
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Top Bar ─────────────────────────────────────────────────────────────────

@Composable
private fun HomeTopBar(userName: String, onOpenProfile: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Accent),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Background,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "ATHRA",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                ),
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Muted)
                .semantics { contentDescription = "Buka profil" }
                .clickable(role = Role.Button, onClick = onOpenProfile),
            contentAlignment = Alignment.Center,
        ) { Text(userName.take(1).uppercase(), color = Ink, style = MaterialTheme.typography.titleMedium) }
    }
}

// ─── Next Workout Card ───────────────────────────────────────────────────────

@Composable
private fun NextWorkoutCard(hasDraft: Boolean, onStartWorkout: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Forest, SoftGreen),
                )
            )
            .border(1.dp, Border, CardShape)
            .padding(16.dp),
    ) {
        Column {
            Text(
                text = if (hasDraft) "SESI BELUM SELESAI" else "MULAI LATIHAN",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Accent,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                ),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (hasDraft) "Lanjutkan workout" else "Siap berlatih?",
                style = MaterialTheme.typography.headlineMedium.copy(color = Ink),
            )
            Text(
                text = if (hasDraft) "Draft tersimpan di perangkat ini" else "Buat sesi dan pilih gerakan",
                style = MaterialTheme.typography.bodyMedium.copy(color = Secondary),
            )
            Spacer(Modifier.height(16.dp))
            AthraButton(label = if (hasDraft) "Lanjutkan sesi" else "Mulai workout", onClick = onStartWorkout)
        }
    }
}

// ─── This Week Section ───────────────────────────────────────────────────────

@Composable
private fun ThisWeekSection(analytics: AnalyticsSummary?, weightUnit: WeightUnit) {
    Column {
        val stackHeader = LocalDensity.current.fontScale >= 1.5f
        if (stackHeader) {
            Column {
                Text("Minggu ini", style = MaterialTheme.typography.headlineSmall.copy(color = Ink))
                Text("Sesi, set, dan volume", style = MaterialTheme.typography.labelSmall.copy(color = Secondary))
            }
        } else Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Minggu ini",
                style = MaterialTheme.typography.headlineSmall.copy(color = Ink),
            )
            Text("Sesi, set, dan volume", style = MaterialTheme.typography.labelSmall.copy(color = Secondary))
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(value = "%02d".format(analytics?.sessionCount ?: 0), label = "Sesi", modifier = Modifier.weight(1f))
            StatCard(value = "%02d".format(analytics?.setCount ?: 0), label = "Set", modifier = Modifier.weight(1f))
            StatCard(value = analytics?.volumeKg?.let { displayNumber(displayWeight(it, weightUnit)) } ?: "0", label = "Volume · ${weightUnit.name.lowercase()}", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(Surface)
            .border(1.dp, Border, CardShape)
            .padding(16.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(color = Ink),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = Secondary),
        )
    }
}

// ─── Recent Session Row ──────────────────────────────────────────────────────

@Composable
private fun RecentSessionRow(sessionId: String, name: String, meta: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Surface)
            .border(1.dp, Border, CardShape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Muted),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Ink,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Secondary),
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Secondary,
        )
    }
}

// ─── Bottom Navigation ───────────────────────────────────────────────────────

@Composable
fun AthraBottomNav(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf("Beranda", "Latihan", "Progress", "Profil")
    NavigationBar(
        containerColor = Surface,
        tonalElevation = 0.dp,
        modifier = Modifier.border(
            width = 1.dp,
            color = Border,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        ),
    ) {
        items.forEachIndexed { index, label ->
            NavigationBarItem(
                selected = index == selected,
                onClick = { onSelect(index) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                icon = {},
                colors = NavigationBarItemDefaults.colors(
                    selectedTextColor   = Accent,
                    unselectedTextColor = Secondary,
                    indicatorColor      = Color.Transparent,
                ),
            )
        }
    }
}
