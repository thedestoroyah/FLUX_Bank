package com.flux.bank.data.repository

import com.flux.bank.data.model.User
import com.flux.bank.data.model.UserRole
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Đọc/ghi hồ sơ người dùng (collection `users`). */
interface UserRepository {
    suspend fun getUser(uid: String): Result<User>
    /** Ghi hồ sơ và đăng ký SĐT vào `phoneIndex` trong một batch. */
    suspend fun createProfile(user: User): Result<Unit>
    suspend fun updateFields(uid: String, fields: Map<String, Any?>): Result<Unit>
    /** Chỉ dùng để rollback khi tạo khách hàng thất bại giữa chừng (xoá cả mục phoneIndex). */
    suspend fun deleteProfile(uid: String): Result<Unit>
    /** Đổi SĐT: chuyển mục `phoneIndex` từ số cũ sang số mới và cập nhật hồ sơ trong một batch. */
    suspend fun changePhone(uid: String, oldPhone: String, newPhone: String): Result<Unit>
    suspend fun listCustomers(): Result<List<User>>
    /** Tra uid sở hữu SĐT qua `phoneIndex/{phone}` (ai đăng nhập cũng đọc được từng mục). */
    suspend fun findUidByPhone(phone: String): Result<String?>
    suspend fun findByNationalId(nationalId: String): Result<User?>
    suspend fun countCustomers(): Result<Int>
}

class FirestoreUserRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    private val users get() = db.collection(COLLECTION)
    private val phoneIndex get() = db.collection(PHONE_INDEX)

    override suspend fun getUser(uid: String): Result<User> = runCatching {
        users.document(uid).get().await().toObject(User::class.java)
            ?: error("Không tìm thấy hồ sơ người dùng")
    }

    override suspend fun createProfile(user: User): Result<Unit> = runCatching {
        db.batch()
            .set(users.document(user.uid), user)
            .set(phoneIndex.document(user.phoneNumber), mapOf("uid" to user.uid))
            .commit().await()
        Unit
    }

    override suspend fun updateFields(uid: String, fields: Map<String, Any?>): Result<Unit> = runCatching {
        users.document(uid).update(fields).await()
        Unit
    }

    override suspend fun deleteProfile(uid: String): Result<Unit> = runCatching {
        val phone = users.document(uid).get().await().getString("phoneNumber")
        val batch = db.batch().delete(users.document(uid))
        if (!phone.isNullOrEmpty()) batch.delete(phoneIndex.document(phone))
        batch.commit().await()
        Unit
    }

    override suspend fun changePhone(uid: String, oldPhone: String, newPhone: String): Result<Unit> = runCatching {
        val batch = db.batch()
            .set(phoneIndex.document(newPhone), mapOf("uid" to uid))
            .update(users.document(uid), "phoneNumber", newPhone)
        if (oldPhone.isNotEmpty() && oldPhone != newPhone) batch.delete(phoneIndex.document(oldPhone))
        batch.commit().await()
        Unit
    }

    // shortcut: tải toàn bộ khách hàng rồi lọc phía client; chuyển sang index/dịch vụ tìm kiếm khi vượt vài trăm khách
    override suspend fun listCustomers(): Result<List<User>> = runCatching {
        users.whereEqualTo("role", UserRole.CUSTOMER.name).get().await().toObjects(User::class.java)
    }

    override suspend fun findUidByPhone(phone: String): Result<String?> = runCatching {
        phoneIndex.document(phone).get().await().getString("uid")
    }

    override suspend fun findByNationalId(nationalId: String): Result<User?> = findOne("nationalId", nationalId)

    override suspend fun countCustomers(): Result<Int> = runCatching {
        users.whereEqualTo("role", UserRole.CUSTOMER.name).count().get(AggregateSource.SERVER).await().count.toInt()
    }

    private suspend fun findOne(field: String, value: String): Result<User?> = runCatching {
        users.whereEqualTo(field, value).limit(1).get().await().toObjects(User::class.java).firstOrNull()
    }

    companion object {
        const val COLLECTION = "users"
        const val PHONE_INDEX = "phoneIndex"
    }
}
