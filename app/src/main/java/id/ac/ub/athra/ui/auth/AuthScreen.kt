package id.ac.ub.athra.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ac.ub.athra.ui.theme.Accent
import id.ac.ub.athra.ui.theme.Background
import id.ac.ub.athra.ui.theme.Ink
import id.ac.ub.athra.ui.theme.Secondary
import id.ac.ub.athra.ui.theme.Surface

@Composable
fun AuthScreen(
    state: AuthUiState,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String, String, String) -> Unit,
    startInRegistration: Boolean = false,
    onSwitchMode: (Boolean) -> Unit = {},
) {
    var registering by rememberSaveable { mutableStateOf(startInRegistration) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }

    Scaffold(containerColor = Background) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("ATHRA", style = MaterialTheme.typography.labelLarge.copy(color = Accent, letterSpacing = 2.sp))
            Spacer(Modifier.height(20.dp))
            Text(
                if (registering) "Buat akun" else "Selamat datang kembali",
                style = MaterialTheme.typography.headlineLarge.copy(color = Ink, fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(8.dp))
            Text("Catat latihanmu dan lihat perkembangan dari waktu ke waktu.", color = Secondary)
            Spacer(Modifier.height(24.dp))

            if (registering) {
                AuthField("Nama", name, { name = it }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }
            AuthField("Email", email, { email = it }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            AuthField("Password", password, { password = it }, Modifier.fillMaxWidth(), secret = true)
            if (registering) {
                Spacer(Modifier.height(12.dp))
                AuthField("Konfirmasi password", confirmation, { confirmation = it }, Modifier.fillMaxWidth(), secret = true)
            }

            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (registering) onSignUp(name, email, password, confirmation) else onSignIn(email, password)
                },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background),
            ) {
                Text(if (state.busy) "Memproses…" else if (registering) "Daftar" else "Masuk", fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = { registering = !registering; onSwitchMode(registering) }) {
                Text(
                    if (registering) "Sudah punya akun? Masuk" else "Belum punya akun? Daftar",
                    color = Secondary,
                )
            }
            Text("Workout disimpan di perangkat ini.", color = Secondary, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    secret: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedTextColor = Ink,
            unfocusedTextColor = Ink,
            focusedContainerColor = Surface,
            unfocusedContainerColor = Surface,
            focusedLabelColor = Accent,
            unfocusedLabelColor = Secondary,
        ),
    )
}
