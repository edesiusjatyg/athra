package id.ac.ub.athra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.ac.ub.athra.ui.components.*
import id.ac.ub.athra.ui.theme.*

@Composable
fun PreWorkoutScreen(
    onBack: () -> Unit = {},
    onBeginSession: () -> Unit = {},
    onQuickStart: () -> Unit = {},
    hasDraft: Boolean = false,
    onContinueDraft: () -> Unit = {},
    onDiscardDraft: () -> Unit = {},
    busy: Boolean = false,
    loading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = onBeginSession,
) {
    var confirmDiscard by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = Background,
        topBar = { AthraScreenHeader("Workout", onBack) },
        bottomBar = {
            AthraPrimaryButton(
                if (loading) "Memeriksa sesi…" else if (hasDraft) "Lanjutkan sesi" else "Mulai sesi",
                if (hasDraft) onContinueDraft else onBeginSession,
                Modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
                enabled = !busy && !loading && error == null,
            )
        },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(20.dp))
            Text(if (hasDraft) "Lanjutkan workout" else "Mulai workout", style = MaterialTheme.typography.headlineLarge, color = Ink)
            Spacer(Modifier.height(8.dp))
            Text("Pilih caramu berlatih hari ini.", style = MaterialTheme.typography.bodyLarge, color = Secondary)
            Spacer(Modifier.height(32.dp))
            Column(Modifier.fillMaxWidth().clip(CardShape).background(Surface).border(1.dp, Border, CardShape).padding(18.dp)) {
                AthraChip(if (hasDraft) "DRAFT TERSIMPAN" else "SESI BARU")
                Spacer(Modifier.height(18.dp))
                Text(if (hasDraft) "Sesi belum selesai" else "Latihan pilihanmu", style = MaterialTheme.typography.headlineMedium, color = Ink)
                Spacer(Modifier.height(6.dp))
                Text(if (hasDraft) "Lanjutkan gerakan dan set yang tersimpan." else "Pilih gerakan · Catat setiap set", color = Secondary, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(22.dp))
                HorizontalDivider(color = Border)
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.FitnessCenter, null, tint = Forest, modifier = Modifier.size(18.dp))
                    Text("Gerakan bebas", style = MaterialTheme.typography.bodyMedium, color = Ink)
                    Icon(Icons.Outlined.Schedule, null, tint = Forest, modifier = Modifier.size(18.dp))
                    Text("Tanpa batas", style = MaterialTheme.typography.bodyMedium, color = Ink)
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (hasDraft) "Draft tersimpan di perangkat" else "Susun sesi sesuai kebutuhanmu", style = MaterialTheme.typography.bodyMedium, color = Secondary)
                    Icon(Icons.Outlined.ChevronRight, null, tint = Ink, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth().clip(CardShape).background(Surface).border(1.dp, Border, CardShape)
                    .clickable(enabled = !busy && !loading && error == null) { if (hasDraft) confirmDiscard = true else onQuickStart() }.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(50.dp).background(Muted, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Add, null, tint = Forest) }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (hasDraft) "Mulai dari awal" else "Mulai cepat", style = MaterialTheme.typography.headlineSmall, color = Ink)
                    Spacer(Modifier.height(6.dp))
                    Text(if (hasDraft) "Buang draft dan buat sesi baru" else "Susun workout dari awal", style = MaterialTheme.typography.bodyMedium, color = Secondary)
                }
                Icon(Icons.Outlined.ChevronRight, null, tint = Secondary)
            }
            Spacer(Modifier.height(30.dp))
            Text("Sebelum mulai", style = MaterialTheme.typography.headlineSmall, color = Ink)
            Spacer(Modifier.height(16.dp))
            PreparationTip("Tentukan tujuan sederhana", "Kemajuan datang dari konsistensi.")
            Spacer(Modifier.height(22.dp))
            PreparationTip("Lakukan pemanasan", "Siapkan tubuh sebelum set pertama.")
            if (loading) { Spacer(Modifier.height(12.dp)); CircularProgressIndicator(Modifier.size(24.dp)) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = onRetry, enabled = !busy) { Text("Coba lagi") } }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false },
        title = { Text("Buang draft?") },
        text = { Text("Sesi dan semua set tersimpan di dalamnya akan dihapus.") },
        confirmButton = { TextButton(onClick = { confirmDiscard = false; onDiscardDraft() }) { Text("Buang dan mulai baru") } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Kembali") } },
    )
}

@Composable
private fun PreparationTip(title: String, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).background(Forest, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, null, tint = Accent, modifier = Modifier.size(16.dp)) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = Ink, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = Secondary)
        }
    }
}
