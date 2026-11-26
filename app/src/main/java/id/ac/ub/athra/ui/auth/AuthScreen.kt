package id.ac.ub.athra.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import id.ac.ub.athra.ui.components.AthraBrandHeader
import id.ac.ub.athra.ui.components.AthraPrimaryButton
import id.ac.ub.athra.ui.theme.*

@Composable
fun AuthScreen(
    state: AuthUiState,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String, String, String) -> Unit,
    startInRegistration: Boolean = false,
    onSwitchMode: (Boolean) -> Unit = {},
    onClearError: () -> Unit = {},
) {
    var registering by rememberSaveable { mutableStateOf(startInRegistration) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    Scaffold(containerColor = Background) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp).padding(top = 20.dp, bottom = 32.dp),
        ) {
            AthraBrandHeader()
            Spacer(Modifier.height(if (registering) 32.dp else 56.dp))
            Text("LANGKAH BARUMU", style = MaterialTheme.typography.labelSmall, color = Accent)
            Spacer(Modifier.height(10.dp))
            Text(if (registering) "Buat akun." else "Selamat datang kembali.", style = MaterialTheme.typography.headlineLarge, color = Ink)
            Spacer(Modifier.height(10.dp))
            Text("Catat latihanmu dan terus jaga semangat berlatih.", style = MaterialTheme.typography.bodyLarge, color = Secondary)
            Spacer(Modifier.height(if (registering) 28.dp else 56.dp))
            if (registering) {
                AuthField("Nama", "Nama tampilan", name, { name = it; onClearError() }, error = state.nameError)
                Spacer(Modifier.height(18.dp))
            }
            AuthField("Email", "kamu@example.com", email, { email = it; onClearError() }, error = state.emailError)
            Spacer(Modifier.height(20.dp))
            AuthField("Password", "Masukkan password", password, { password = it; onClearError() }, secret = true, error = state.passwordError)
            if (registering) {
                Spacer(Modifier.height(18.dp))
                AuthField("Konfirmasi password", "Ulangi password", confirmation, { confirmation = it; onClearError() }, secret = true, error = state.confirmationError)
            }
            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(if (registering) 28.dp else 76.dp))
            AthraPrimaryButton(
                label = if (state.busy) "Memproses…" else if (registering) "Daftar" else "Masuk",
                onClick = { if (registering) onSignUp(name, email, password, confirmation) else onSignIn(email, password) },
                enabled = !state.busy,
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(if (registering) "Sudah punya akun?" else "Baru di ATHRA?", color = Secondary, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = {
                    registering = !registering
                    password = ""
                    confirmation = ""
                    onClearError()
                    onSwitchMode(registering)
                }) {
                    Text(if (registering) "Masuk" else "Buat akun", color = Accent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(44.dp))
            Text("DATANG. JADI LEBIH KUAT. ULANGI.", color = Secondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun AuthField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    secret: Boolean = false,
    error: String? = null,
) {
    var visible by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Ink)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            isError = error != null,
            supportingText = if (error == null) null else { { Text(error) } },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else if (label == "Email") KeyboardType.Email else KeyboardType.Text),
            visualTransformation = if (secret && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (!secret) null else { {
                IconButton(onClick = { visible = !visible }) {
                    Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (visible) "Sembunyikan password" else "Tampilkan password", tint = Secondary, modifier = Modifier.size(20.dp))
                }
            } },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Ink, unfocusedTextColor = Ink,
                focusedContainerColor = Surface, unfocusedContainerColor = Surface,
                focusedBorderColor = Accent, unfocusedBorderColor = Border,
                focusedPlaceholderColor = Secondary, unfocusedPlaceholderColor = Secondary,
            ),
        )
    }
}
