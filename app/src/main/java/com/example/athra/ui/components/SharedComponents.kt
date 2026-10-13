package com.example.athra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.athra.ui.theme.*

val CardShape  = RoundedCornerShape(16.dp)
val ChipShape  = RoundedCornerShape(8.dp)
val ButtonShape = RoundedCornerShape(20.dp)

// ─── Primary CTA Button ──────────────────────────────────────────────────────

@Composable
fun AthraButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(ButtonShape)
            .background(Accent)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = Background,
                fontSize = 15.sp,
            ),
        )
    }
}

// ─── Surface Card ────────────────────────────────────────────────────────────

@Composable
fun AthraCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Surface)
            .border(1.dp, Border, CardShape)
            .padding(16.dp),
        content = content,
    )
}

// ─── Pill / Badge Chip ───────────────────────────────────────────────────────

@Composable
fun AthraChip(
    label: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Forest,
    textColor: Color = Accent,
) {
    Box(
        modifier = modifier
            .clip(ChipShape)
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

// ─── Dot Indicator ───────────────────────────────────────────────────────────

@Composable
fun StatusDot(color: Color = Accent, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(RoundedCornerShape(50))
            .background(color),
    )
}

// ─── Section Header Row ──────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(color = Ink),
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Accent,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = Modifier.clickable(onClick = onAction),
            )
        }
    }
}