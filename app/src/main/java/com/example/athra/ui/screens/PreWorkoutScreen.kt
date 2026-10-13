package com.example.athra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.athra.ui.components.*
import com.example.athra.ui.theme.*

// ─── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun PreWorkoutScreen(
    onBack: () -> Unit = {},
    onBeginSession: () -> Unit = {},
    onQuickStart: () -> Unit = {},
) {
    Scaffold(
        containerColor = Background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Top bar ──
            PreWorkoutTopBar(onBack = onBack)

            Spacer(Modifier.height(20.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {

                // ── Header ──
                Text(
                    text = "Start a workout",
                    style = MaterialTheme.typography.headlineLarge.copy(color = Ink),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Choose how you'd like to train today.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Secondary),
                )

                Spacer(Modifier.height(24.dp))

                // ── Suggested workout card ──
                SuggestedWorkoutCard(onClick = {})

                Spacer(Modifier.height(12.dp))

                // ── Quick start card ──
                QuickStartCard(onClick = onQuickStart)

                Spacer(Modifier.height(32.dp))

                // ── Before you begin ──
                BeforeYouBeginSection()

                Spacer(Modifier.height(32.dp))
            }

            // ── CTA pinned at bottom ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                AthraButton(label = "Begin session", onClick = onBeginSession)
            }
        }
    }
}

// ─── Top Bar ─────────────────────────────────────────────────────────────────

@Composable
private fun PreWorkoutTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Back",
            tint = Ink,
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onBack),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Workout",
            style = MaterialTheme.typography.headlineSmall.copy(color = Ink),
        )
    }
}

// ─── Suggested Workout Card ──────────────────────────────────────────────────

@Composable
private fun SuggestedWorkoutCard(onClick: () -> Unit) {
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
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Column {
            // Badge
            AthraChip(label = "SUGGESTED")

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Evening Pull Day",
                style = MaterialTheme.typography.headlineMedium.copy(color = Ink),
            )
            Text(
                text = "Back & biceps · Strength",
                style = MaterialTheme.typography.bodyMedium.copy(color = Secondary),
            )

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(color = Border, thickness = 1.dp)

            Spacer(Modifier.height(12.dp))

            // Meta row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    WorkoutMeta(icon = "💪", text = "5 exercises")
                    WorkoutMeta(icon = "⏱", text = "60–75 min")
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Secondary,
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "18 sets planned",
                style = MaterialTheme.typography.bodyMedium.copy(color = Secondary),
            )
        }
    }
}

@Composable
private fun WorkoutMeta(icon: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = icon, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(color = Secondary),
        )
    }
}

// ─── Quick Start Card ────────────────────────────────────────────────────────

@Composable
private fun QuickStartCard(onClick: () -> Unit) {
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Muted)
                    .border(1.dp, Border, CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column {
                Text(
                    text = "Quick start",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Ink,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Text(
                    text = "Build a workout from scratch",
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

// ─── Before You Begin Section ────────────────────────────────────────────────

@Composable
private fun BeforeYouBeginSection() {
    Column {
        Text(
            text = "Before you begin",
            style = MaterialTheme.typography.headlineSmall.copy(color = Ink),
        )

        Spacer(Modifier.height(16.dp))

        BeforeYouBeginItem(
            title = "Set a simple goal",
            description = "Progress follows consistency.",
        )

        Spacer(Modifier.height(12.dp))

        BeforeYouBeginItem(
            title = "Warm up first",
            description = "Prepare for your first working set.",
        )
    }
}

@Composable
private fun BeforeYouBeginItem(title: String, description: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Dot indicator
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(Accent),
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Ink,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(color = Secondary),
            )
        }
    }
}