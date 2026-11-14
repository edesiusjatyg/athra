package id.ac.ub.athra.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.ac.ub.athra.data.auth.AuthRepository
import id.ac.ub.athra.data.auth.AuthUser
import id.ac.ub.athra.data.repository.WorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AuthUiState(
    val restoring: Boolean = true,
    val busy: Boolean = false,
    val user: AuthUser? = null,
    val error: String? = null,
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeUser().collectLatest { user ->
                if (user == null) {
                    mutableState.value = AuthUiState(restoring = false)
                } else {
                    mutableState.value = AuthUiState(restoring = true, user = user)
                    runCatching {
                        withContext(Dispatchers.IO) {
                            workoutRepository.ensureProfile(user.uid, user.displayName, user.email)
                            workoutRepository.seedCatalog()
                        }
                    }.fold(
                        onSuccess = { mutableState.value = AuthUiState(restoring = false, user = user) },
                        onFailure = { error -> mutableState.value = AuthUiState(restoring = false, user = user, error = error.message ?: "Data lokal gagal dimuat.") },
                    )
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (mutableState.value.busy) return
        val cleanEmail = email.trim()
        val validation = when {
            !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() -> "Masukkan email yang valid."
            password.isEmpty() -> "Password wajib diisi."
            else -> null
        }
        if (validation != null) {
            mutableState.value = mutableState.value.copy(error = validation)
            return
        }
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(busy = true, error = null)
            runCatching { authRepository.signIn(cleanEmail, password) }
                .onFailure { mutableState.value = mutableState.value.copy(error = it.message ?: "Gagal masuk. Periksa koneksi lalu coba lagi.") }
            mutableState.value = mutableState.value.copy(busy = false)
        }
    }

    fun signUp(name: String, email: String, password: String, confirmation: String) {
        if (mutableState.value.busy) return
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val validation = when {
            cleanName.isEmpty() -> "Nama wajib diisi."
            !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() -> "Masukkan email yang valid."
            password.length < 6 -> "Password minimal 6 karakter."
            password != confirmation -> "Konfirmasi password belum cocok."
            else -> null
        }
        if (validation != null) {
            mutableState.value = mutableState.value.copy(error = validation)
            return
        }
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(busy = true, error = null)
            runCatching {
                val user = authRepository.signUp(cleanName, cleanEmail, password)
                withContext(Dispatchers.IO) {
                    workoutRepository.ensureProfile(user.uid, cleanName, cleanEmail)
                    workoutRepository.seedCatalog()
                }
            }.onFailure { mutableState.value = mutableState.value.copy(error = it.message ?: "Pendaftaran gagal. Coba lagi.") }
            mutableState.value = mutableState.value.copy(busy = false)
        }
    }

    fun clearError() {
        mutableState.value = mutableState.value.copy(error = null)
    }

    fun retryLocalSetup() {
        val user = mutableState.value.user ?: return
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(restoring = true, error = null)
            runCatching {
                withContext(Dispatchers.IO) {
                    workoutRepository.ensureProfile(user.uid, user.displayName, user.email)
                    workoutRepository.seedCatalog()
                }
            }.fold(
                onSuccess = { mutableState.value = mutableState.value.copy(restoring = false, error = null) },
                onFailure = { mutableState.value = mutableState.value.copy(restoring = false, error = it.message ?: "Data lokal gagal dimuat.") },
            )
        }
    }

    fun signOut() {
        runCatching { authRepository.signOut() }
            .onFailure { mutableState.value = mutableState.value.copy(error = it.message ?: "Gagal keluar.") }
    }
}

class AuthViewModelFactory(
    private val authRepository: AuthRepository,
    private val workoutRepository: WorkoutRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AuthViewModel(authRepository, workoutRepository) as T
}
