package com.flux.bank.data.repository

import com.flux.bank.data.model.KycStatus
import com.flux.bank.data.model.User
import com.flux.bank.data.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Signs in user with email and password via Firebase Authentication.
     */
    suspend fun login(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user ?: throw Exception("Authentication returned no user.")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves User document from Firestore collection "users" based on uid.
     */
    suspend fun getUserProfile(uid: String): Result<User> {
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            if (snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                if (user != null) {
                    Result.success(user)
                } else {
                    Result.failure(Exception("Failed to parse user document."))
                }
            } else {
                Result.failure(Exception("User profile not found in database for UID: $uid"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Saves or updates a User document in Firestore collection "users".
     */
    suspend fun saveUserProfile(user: User): Result<Unit> {
        return try {
            firestore.collection("users").document(user.uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Submits eKYC documents (Citizen ID photo & Face selfie) and sets status to PENDING.
     */
    suspend fun submitEkyc(uid: String, idCardBase64: String, faceBase64: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "idCardBase64" to idCardBase64,
                "faceImageBase64" to faceBase64,
                "kycStatus" to KycStatus.PENDING
            )
            firestore.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves list of users who have submitted eKYC documents awaiting Bank Officer review.
     */
    suspend fun getPendingEkycUsers(): Result<List<User>> {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("kycStatus", KycStatus.PENDING)
                .get()
                .await()
            val users = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Bank Officer review action to either approve (VERIFIED) or reject (REJECTED) an eKYC submission.
     */
    suspend fun reviewEkyc(uid: String, isApproved: Boolean): Result<Unit> {
        return try {
            val newStatus = if (isApproved) KycStatus.VERIFIED else KycStatus.REJECTED
            firestore.collection("users").document(uid).update("kycStatus", newStatus).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates biometric preference on Firestore.
     */
    suspend fun setBiometricEnabled(uid: String, enabled: Boolean): Result<Unit> {
        return try {
            firestore.collection("users").document(uid).update("isBiometricEnabled", enabled).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Returns the currently signed in Firebase user, if any.
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /**
     * Signs out the current user.
     */
    fun logout() {
        auth.signOut()
    }
}
