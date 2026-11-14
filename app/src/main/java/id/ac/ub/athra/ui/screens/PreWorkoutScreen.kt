package id.ac.ub.athra.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
) {
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
            Text("Siap berlatih?", style = MaterialTheme.typography.headlineLarge, color = Ink)
            Text("Sesi mulai dicatat saat kamu memilih salah satu aksi.", color = Secondary)
            AthraCard {
                Text("Mulai workout", style = MaterialTheme.typography.titleLarge, color = Ink)
                Text("Buat sesi baru, lalu tambahkan gerakan dan set.", color = Secondary)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onBeginSession, modifier = Modifier.fillMaxWidth()) { Text("Mulai workout") }
            }
            OutlinedButton(onClick = onQuickStart, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Mulai cepat")
            }
        }
    }
}
