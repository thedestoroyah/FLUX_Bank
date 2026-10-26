package com.flux.bank.data.repository

import com.flux.bank.data.model.User
import com.flux.bank.data.model.UserRole
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Đọc/ghi hồ sơ người dùng (collection `users`). */
interface UserRepository {
    suspend fun getUser(uid: String): Result<User>
    suspend fun createProfile(user: User): Result<Unit>
    suspend fun updateFields(uid: String, fields: Map<String, Any?>): Result<Unit>
    /** Chỉ dùng để rollback khi tạo khách hàng thất bại giữa chừng. */
    suspend fun deleteProfile(uid: String): Result<Unit>
    suspend fun listCustomers(): Result<List<User>>
    suspend fun findByPhone(phone: String): Result<User?>
    suspend fun findByNationalId(nationalId: String): Result<User?>
    suspend fun countCustomers(): Result<Int>
}

class FirestoreUserRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    private val users get() = db.collection(COLLECTION)

    override suspend fun getUser(uid: String): Result<User> = runCatching {
        users.document(uid).get().await().toObject(User::class.java)
            ?: error("Không tìm thấy hồ sơ người dùng")
    }

    override suspend fun createProfile(user: User): Result<Unit> = runCatching {
        users.document(user.uid).set(user).await()
        Unit
    }

    override suspend fun updateFields(uid: String, fields: Map<String, Any?>): Result<Unit> = runCatching {
        users.document(uid).update(fields).await()
        Unit
    }

    override suspend fun deleteProfile(uid: String): Result<Unit> = runCatching {
        users.document(uid).delete().await()
        Unit
    }

    // shortcut: tải toàn bộ khách hàng rồi lọc phía client; chuyển sang index/dịch vụ tìm kiếm khi vượt vài trăm khách
    override suspend fun listCustomers(): Result<List<User>> = runCatching {
        users.whereEqualTo("role", UserRole.CUSTOMER.name).get().await().toObjects(User::class.java)
    }

    override suspend fun findByPhone(phone: String): Result<User?> = findOne("phoneNumber", phone)

    override suspend fun findByNationalId(nationalId: String): Result<User?> = findOne("nationalId", nationalId)

    override suspend fun countCustomers(): Result<Int> = runCatching {
        users.whereEqualTo("role", UserRole.CUSTOMER.name).count().get(AggregateSource.SERVER).await().count.toInt()
    }

    private suspend fun findOne(field: String, value: String): Result<User?> = runCatching {
        users.whereEqualTo(field, value).limit(1).get().await().toObjects(User::class.java).firstOrNull()
    }

    companion object {
        const val COLLECTION = "users"
    }
}
