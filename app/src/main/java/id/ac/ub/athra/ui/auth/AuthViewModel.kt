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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class AuthUiState(
    val restoring: Boolean = true,
    val busy: Boolean = false,
    val user: AuthUser? = null,
    val error: String? = null,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmationError: String? = null,
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState())
    val state = mutableState.asStateFlow()
    private val profileSetupMutex = Mutex()

    init {
        viewModelScope.launch {
            authRepository.observeUser().collectLatest { user ->
                if (user == null) {
                    mutableState.value = AuthUiState(restoring = false)
                } else {
                    mutableState.value = AuthUiState(restoring = true, user = user)
                    try {
                        val resolvedUser = profileSetupMutex.withLock {
                            withContext(Dispatchers.IO) {
                                val fallbackName = user.displayName.ifBlank { user.email.substringBefore('@') }
                                workoutRepository.ensureProfile(user.uid, fallbackName, user.email)
                                workoutRepository.seedCatalog()
                                val profile = workoutRepository.getProfile(user.uid)
                                user.copy(displayName = profile?.displayName?.ifBlank { fallbackName } ?: fallbackName)
                            }
                        }
                        mutableState.value = AuthUiState(restoring = false, user = resolvedUser)
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        mutableState.value = AuthUiState(
                            restoring = false,
                            user = user,
                            error = error.message ?: "Data lokal gagal dimuat.",
                        )
                    }
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (mutableState.value.busy) return
        val cleanEmail = email.trim()
        val emailError = if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) "Masukkan email yang valid." else null
        val passwordError = if (password.isEmpty()) "Password wajib diisi." else null
        if (emailError != null || passwordError != null) {
            mutableState.value = mutableState.value.copy(
                error = null,
                nameError = null,
                emailError = emailError,
                passwordError = passwordError,
                confirmationError = null,
            )
            return
        }
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(
                busy = true,
                error = null,
                nameError = null,
                emailError = null,
                passwordError = null,
                confirmationError = null,
            )
            try {
                authRepository.signIn(cleanEmail, password)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    error = error.message ?: "Gagal masuk. Periksa koneksi lalu coba lagi.",
                )
            } finally {
                mutableState.value = mutableState.value.copy(busy = false)
            }
        }
    }

    fun signUp(name: String, email: String, password: String, confirmation: String) {
        if (mutableState.value.busy) return
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val nameError = if (cleanName.isEmpty()) "Nama wajib diisi." else null
        val emailError = if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) "Masukkan email yang valid." else null
        val passwordError = if (password.length < 6) "Password minimal 6 karakter." else null
        val confirmationError = if (password != confirmation) "Konfirmasi password belum cocok." else null
        if (nameError != null || emailError != null || passwordError != null || confirmationError != null) {
            mutableState.value = mutableState.value.copy(
                error = null,
                nameError = nameError,
                emailError = emailError,
                passwordError = passwordError,
                confirmationError = confirmationError,
            )
            return
        }
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(
                busy = true,
                error = null,
                nameError = null,
                emailError = null,
                passwordError = null,
                confirmationError = null,
            )
            var createdUser: AuthUser? = null
            try {
                val user = profileSetupMutex.withLock {
                    val created = authRepository.signUp(cleanName, cleanEmail, password).copy(displayName = cleanName)
                    createdUser = created
                    withContext(Dispatchers.IO) {
                        workoutRepository.ensureProfile(created.uid, cleanName, cleanEmail)
                        workoutRepository.updateProfileName(created.uid, cleanName)
                        workoutRepository.seedCatalog()
                    }
                    created
                }
                mutableState.value = AuthUiState(restoring = false, user = user)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    restoring = false,
                    user = createdUser ?: mutableState.value.user,
                    error = error.message ?: "Pendaftaran gagal. Coba lagi.",
                )
            } finally {
                mutableState.value = mutableState.value.copy(busy = false)
            }
        }
    }

    fun clearError() {
        mutableState.value = mutableState.value.copy(
            error = null,
            nameError = null,
            emailError = null,
            passwordError = null,
            confirmationError = null,
        )
    }

    fun retryLocalSetup() {
        val user = mutableState.value.user ?: return
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(restoring = true, error = null)
            try {
                val resolvedUser = profileSetupMutex.withLock {
                    withContext(Dispatchers.IO) {
                        val fallbackName = user.displayName.ifBlank { user.email.substringBefore('@') }
                        workoutRepository.ensureProfile(user.uid, fallbackName, user.email)
                        if (workoutRepository.getProfile(user.uid)?.displayName.isNullOrBlank()) {
                            workoutRepository.updateProfileName(user.uid, fallbackName)
                        }
                        workoutRepository.seedCatalog()
                        val profile = workoutRepository.getProfile(user.uid)
                        user.copy(displayName = profile?.displayName?.ifBlank { fallbackName } ?: fallbackName)
                    }
                }
                mutableState.value = AuthUiState(restoring = false, user = resolvedUser)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    restoring = false,
                    error = error.message ?: "Data lokal gagal dimuat.",
                )
            }
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
