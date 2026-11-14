package id.ac.ub.athra.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.ac.ub.athra.ui.components.AthraCard
import id.ac.ub.athra.ui.theme.Background
import id.ac.ub.athra.ui.theme.Ink
import id.ac.ub.athra.ui.theme.Secondary

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PreWorkoutScreen(
    onBack: () -> Unit = {},
    onBeginSession: () -> Unit = {},
    onQuickStart: () -> Unit = {},
    hasDraft: Boolean = false,
    onContinueDraft: () -> Unit = {},
    onDiscardDraft: () -> Unit = {},
    busy: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = onBeginSession,
) {
    var confirmDiscard by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Mulai latihan") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Kembali") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
            )
        },
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (hasDraft) {
                Text("Sesi latihan belum selesai", style = MaterialTheme.typography.headlineLarge, color = Ink)
                Text("Lanjutkan draft yang tersimpan atau buang sebelum membuat sesi baru.", color = Secondary)
                Button(onClick = onContinueDraft, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Lanjutkan sesi") }
                OutlinedButton(onClick = { confirmDiscard = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Buang draft dan mulai baru") }
            } else {
                Text("Siap berlatih?", style = MaterialTheme.typography.headlineLarge, color = Ink)
                Text("Sesi mulai dicatat saat kamu memilih salah satu aksi.", color = Secondary)
                AthraCard {
                    Text("Mulai workout", style = MaterialTheme.typography.titleLarge, color = Ink)
                    Text("Buat sesi baru, lalu tambahkan gerakan dan set.", color = Secondary)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onBeginSession, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Mulai workout") }
                }
                OutlinedButton(onClick = onQuickStart, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Mulai cepat")
                }
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry, enabled = !busy) { Text("Coba lagi") }
            }
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
