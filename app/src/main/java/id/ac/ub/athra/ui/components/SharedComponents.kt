package id.ac.ub.athra.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.ac.ub.athra.ui.theme.*

val CardShape = RoundedCornerShape(16.dp)
val ChipShape = RoundedCornerShape(50)
val ButtonShape = RoundedCornerShape(16.dp)

@Composable
fun AthraPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    ) { Text(label, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun AthraButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AthraPrimaryButton(label, onClick, modifier)
}

@Composable
fun AthraBrandMark(modifier: Modifier = Modifier) {
    Canvas(modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Forest)) {
        val w = size.width
        val h = size.height
        val star = Path().apply {
            moveTo(w * .5f, h * .19f)
            lineTo(w * .59f, h * .41f)
            lineTo(w * .81f, h * .5f)
            lineTo(w * .59f, h * .59f)
            lineTo(w * .5f, h * .81f)
            lineTo(w * .41f, h * .59f)
            lineTo(w * .19f, h * .5f)
            lineTo(w * .41f, h * .41f)
            close()
        }
        drawPath(star, Accent, style = androidx.compose.ui.graphics.drawscope.Stroke(1.8.dp.toPx()))
    }
}

@Composable
fun AthraBrandHeader(onProfile: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        AthraBrandMark()
        Spacer(Modifier.width(12.dp))
        Text("ATHRA", style = MaterialTheme.typography.headlineMedium, color = Ink)
        Spacer(Modifier.weight(1f))
        if (onProfile != null) {
            IconButton(onClick = onProfile, modifier = Modifier.size(48.dp)) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(Forest), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.PersonOutline, "Buka profil", tint = Accent, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun AthraScreenHeader(title: String, onBack: () -> Unit, action: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Kembali", tint = Ink, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(4.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun AthraCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(CardShape).background(Surface).border(1.dp, Border, CardShape).padding(16.dp),
        content = content,
    )
}

@Composable
fun AthraChip(label: String, modifier: Modifier = Modifier, backgroundColor: Color = Forest, textColor: Color = Accent) {
    Box(modifier.clip(ChipShape).background(backgroundColor).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = textColor)
    }
}

@Composable
fun StatusDot(color: Color = Accent, modifier: Modifier = Modifier) {
    Box(modifier.size(6.dp).clip(CircleShape).background(color))
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Ink, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Row(
                Modifier.clip(RoundedCornerShape(4.dp)).clickable(onClick = onAction).padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(action, style = MaterialTheme.typography.labelSmall, color = Accent)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, modifier = Modifier.size(14.dp))
            }
        }
    }
}
