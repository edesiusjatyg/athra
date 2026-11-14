package id.ac.ub.athra.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

data class AuthUser(val uid: String, val email: String, val displayName: String)

class AuthConfigurationException : IllegalStateException("Konfigurasi Firebase belum tersedia.")

class AuthRepository(private val auth: FirebaseAuth?) {
    fun observeUser(): Flow<AuthUser?> = callbackFlow {
        val firebase = auth
        if (firebase == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { current ->
            trySend(current.currentUser?.toAuthUser())
        }
        firebase.addAuthStateListener(listener)
        awaitClose { firebase.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    suspend fun signIn(email: String, password: String): AuthUser = withFirebase { firebase ->
        try {
            firebase.signInWithEmailAndPassword(email, password).await().user?.toAuthUser()
                ?: error("Akun tidak tersedia.")
        } catch (error: Exception) {
            throw mapAuthError(error)
        }
    }

    suspend fun signUp(name: String, email: String, password: String): AuthUser = withFirebase { firebase ->
        try {
            val user = firebase.createUserWithEmailAndPassword(email, password).await().user
                ?: error("Akun tidak tersedia.")
            user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()).await()
            user.toAuthUser()
        } catch (error: Exception) {
            throw mapAuthError(error)
        }
    }

    fun signOut() {
        (auth ?: throw AuthConfigurationException()).signOut()
    }

    private suspend fun <T> withFirebase(block: suspend (FirebaseAuth) -> T): T =
        block(auth ?: throw AuthConfigurationException())

    private fun com.google.firebase.auth.FirebaseUser.toAuthUser() =
        AuthUser(uid, email.orEmpty(), displayName.orEmpty())

    private fun mapAuthError(error: Exception): Exception = when (error) {
        is FirebaseAuthUserCollisionException -> IllegalArgumentException("Email sudah terdaftar.", error)
        is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException ->
            IllegalArgumentException("Email atau password tidak cocok.", error)
        else -> error
    }
}
