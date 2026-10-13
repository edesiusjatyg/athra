package com.example.athra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.athra.ui.components.*
import com.example.athra.ui.theme.*

// ─── Data models ─────────────────────────────────────────────────────────────

data class WorkoutSet(
    val setNumber: Int,
    val weight: String,
    val reps: Int,
    val isCompleted: Boolean,
)

// ─── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun LiveWorkoutScreen(
    elapsedTime: String = "00:42:18",
    setsCompleted: Int = 11,
    totalSets: Int = 15,
    currentExercise: String = "Pull up",
    currentMuscles: String = "Back · Biceps",
    sets: List<WorkoutSet> = defaultSets(),
    upNextExercise: String = "Barbell Deadlift",
    upNextMeta: String = "Posterior chain · 4 sets",
    onBack: () -> Unit = {},
    onFinishWorkout: () -> Unit = {},
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
            LiveWorkoutTopBar(onBack = onBack)

            Spacer(Modifier.height(20.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {

                // ── Workout title ──
                Text(
                    text = "Evening Pull Day",
                    style = MaterialTheme.typography.headlineLarge.copy(color = Ink),
                )
                Text(
                    text = "BACK + BICEPS · STRENGTH",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Secondary,
                        letterSpacing = 0.8.sp,
                    ),
                )

                Spacer(Modifier.height(20.dp))

                // ── Timer card ──
                TimerCard(
                    elapsedTime = elapsedTime,
                    setsCompleted = setsCompleted,
                    totalSets = totalSets,
                )

                Spacer(Modifier.height(24.dp))

                // ── Current exercise ──
                Text(
                    text = "Current exercise",
                    style = MaterialTheme.typography.headlineSmall.copy(color = Ink),
                )

                Spacer(Modifier.height(12.dp))

                CurrentExerciseCard(
                    name = currentExercise,
                    muscles = currentMuscles,
                    sets = sets,
                )

                Spacer(Modifier.height(24.dp))

                // ── Up next ──
                Text(
                    text = "Up next",
                    style = MaterialTheme.typography.headlineSmall.copy(color = Ink),
                )

                Spacer(Modifier.height(12.dp))

                UpNextCard(
                    exerciseName = upNextExercise,
                    meta = upNextMeta,
                )

                Spacer(Modifier.height(32.dp))
            }

            // ── Finish button ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                AthraButton(label = "Finish workout", onClick = onFinishWorkout)
            }
        }
    }
}

// ─── Top Bar ─────────────────────────────────────────────────────────────────

@Composable
private fun LiveWorkoutTopBar(onBack: () -> Unit) {
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
            text = "Active workout",
            style = MaterialTheme.typography.headlineSmall.copy(color = Ink),
        )
    }
}

// ─── Timer Card ──────────────────────────────────────────────────────────────

@Composable
private fun TimerCard(
    elapsedTime: String,
    setsCompleted: Int,
    totalSets: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SoftGreen)
            .border(1.dp, Border, CardShape)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "ELAPSED TIME",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Secondary,
                    letterSpacing = 0.8.sp,
                ),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = elapsedTime,
                style = MaterialTheme.typography.displayLarge.copy(
                    color = Accent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                    fontSize = 36.sp,
                ),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "SETS COMPLETED",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Secondary,
                    letterSpacing = 0.8.sp,
                ),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$setsCompleted / $totalSets",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Ink,
                    fontSize = 24.sp,
                ),
            )
        }
    }
}

// ─── Current Exercise Card ───────────────────────────────────────────────────

@Composable
private fun CurrentExerciseCard(
    name: String,
    muscles: String,
    sets: List<WorkoutSet>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Surface)
            .border(1.dp, Border, CardShape)
            .padding(16.dp),
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineMedium.copy(color = Ink),
                )
                Text(
                    text = muscles,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Secondary),
                )
            }
            AthraChip(label = "BODYWEIGHT")
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Border, thickness = 1.dp)
        Spacer(Modifier.height(12.dp))

        // Set table header
        SetTableHeader()

        Spacer(Modifier.height(8.dp))

        // Set rows
        sets.forEach { set ->
            SetRow(set = set)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SetTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        listOf("SET", "WEIGHT", "REPS").forEach { header ->
            Text(
                text = header,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Secondary,
                    letterSpacing = 0.8.sp,
                ),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.width(24.dp)) // checkmark column
    }
}

@Composable
private fun SetRow(set: WorkoutSet) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (set.isCompleted) Muted else Color.Transparent)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "%02d".format(set.setNumber),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = if (set.isCompleted) Secondary else Ink,
                fontWeight = FontWeight.Medium,
            ),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = set.weight,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = if (set.isCompleted) Secondary else Ink,
            ),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = set.reps.toString(),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = if (set.isCompleted) Secondary else Ink,
            ),
            modifier = Modifier.weight(1f),
        )
        if (set.isCompleted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Completed",
                tint = Accent,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Spacer(Modifier.width(18.dp))
        }
    }
}

// ─── Up Next Card ────────────────────────────────────────────────────────────

@Composable
private fun UpNextCard(exerciseName: String, meta: String) {
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
        Column {
            Text(
                text = exerciseName,
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
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Secondary,
        )
    }
}

// ─── Default data ────────────────────────────────────────────────────────────

private fun defaultSets() = listOf(
    WorkoutSet(setNumber = 1, weight = "BW",   reps = 12, isCompleted = true),
    WorkoutSet(setNumber = 2, weight = "+5 kg", reps = 10, isCompleted = true),
    WorkoutSet(setNumber = 3, weight = "+10 kg", reps = 8, isCompleted = false),
)