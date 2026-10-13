package com.example.athra.ui.screens

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.athra.ui.components.*
import com.example.athra.ui.theme.*

// ─── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    userName: String = "Widodo",
    onStartWorkout: () -> Unit = {},
    onViewAll: () -> Unit = {},
) {
    Scaffold(
        containerColor = Background,
        bottomBar = { AthraBottomNav(selected = 0) },
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
            HomeTopBar()

            Spacer(Modifier.height(20.dp))

            // ── Greeting ──
            Text(
                text = "TUESDAY, 13 OCTOBER",
                style = MaterialTheme.typography.labelSmall.copy(color = Secondary),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Good morning, $userName",
                style = MaterialTheme.typography.headlineLarge.copy(color = Ink),
            )
            Text(
                text = "Keep your momentum going.",
                style = MaterialTheme.typography.bodyMedium.copy(color = Secondary),
            )

            Spacer(Modifier.height(20.dp))

            // ── Next workout card ──
            NextWorkoutCard(onStartWorkout = onStartWorkout)

            Spacer(Modifier.height(24.dp))

            // ── This week stats ──
            ThisWeekSection()

            Spacer(Modifier.height(24.dp))

            // ── Recent sessions ──
            SectionHeader(title = "Recent sessions", action = "View all", onAction = onViewAll)
            Spacer(Modifier.height(12.dp))
            RecentSessionRow(
                name = "Evening Pull Day",
                meta = "Today · 72 min · 5 exercises",
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Top Bar ─────────────────────────────────────────────────────────────────

@Composable
private fun HomeTopBar() {
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
        // Avatar placeholder
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Muted),
        )
    }
}

// ─── Next Workout Card ───────────────────────────────────────────────────────

@Composable
private fun NextWorkoutCard(onStartWorkout: () -> Unit) {
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
                text = "YOUR NEXT WORKOUT",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Accent,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                ),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Evening Pull Day",
                style = MaterialTheme.typography.headlineMedium.copy(color = Ink),
            )
            Text(
                text = "5 exercises · Back & biceps",
                style = MaterialTheme.typography.bodyMedium.copy(color = Secondary),
            )
            Spacer(Modifier.height(16.dp))
            AthraButton(label = "Start workout", onClick = onStartWorkout)
        }
    }
}

// ─── This Week Section ───────────────────────────────────────────────────────

@Composable
private fun ThisWeekSection() {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "This week",
                style = MaterialTheme.typography.headlineSmall.copy(color = Ink),
            )
            Text(
                text = "OCT 12 – 18",
                style = MaterialTheme.typography.labelSmall.copy(color = Secondary),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(value = "04", label = "Workouts",   modifier = Modifier.weight(1f))
            StatCard(value = "12", label = "Day streak", modifier = Modifier.weight(1f))
            StatCard(value = "24.8k", label = "Volume · kg", modifier = Modifier.weight(1f))
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
private fun RecentSessionRow(name: String, meta: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Surface)
            .border(1.dp, Border, CardShape)
            .clickable {}
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
fun AthraBottomNav(selected: Int) {
    val items = listOf("Home", "Train", "Progress", "Profile")
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
                onClick = {},
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