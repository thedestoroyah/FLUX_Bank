package com.flux.bank.domain

import com.flux.bank.core.validation.Validators
import com.flux.bank.data.model.User
import com.flux.bank.data.model.UserRole
import com.flux.bank.data.repository.AccountRepository
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.data.repository.UserRepository

/** Dữ liệu nhân viên nhập khi tạo khách hàng mới. */
data class NewCustomerInput(
    val fullName: String,
    val email: String,
    val phone: String,
    val nationalId: String,
    val address: String,
    val tempPassword: String
)

/** Lý do tạo/sửa khách hàng thất bại; UI ánh xạ mỗi loại sang lỗi của đúng ô nhập. */
sealed class OnboardingError {
    object InvalidEmail : OnboardingError()
    object InvalidPhone : OnboardingError()
    object InvalidNationalId : OnboardingError()
    object WeakPassword : OnboardingError()
    object EmptyName : OnboardingError()
    object PhoneTaken : OnboardingError()
    object NationalIdTaken : OnboardingError()
    data class Failed(val cause: Throwable) : OnboardingError()
}

class OnboardingException(val error: OnboardingError) : Exception(error.toString())

/**
 * Nghiệp vụ nhân viên tạo khách hàng: kiểm tra dữ liệu -> kiểm tra trùng -> tạo user Auth ->
 * tạo hồ sơ (bắt đổi mật khẩu lần đầu) -> cấp tài khoản Checking.
 * Nếu lỗi sau khi đã tạo user Auth thì xoá user đó (và hồ sơ nếu đã ghi) để không để lại dữ liệu mồ côi.
 *
 * @param officerUid uid nhân viên đang thao tác, lưu vào `createdBy`.
 */
class CustomerOnboardingService(
    private val auth: AuthRepository,
    private val users: UserRepository,
    private val accounts: AccountRepository,
    private val officerUid: () -> String
) {

    /** Tạo khách hàng. Thất bại trả về [OnboardingException] chứa [OnboardingError]. */
    suspend fun create(input: NewCustomerInput): Result<User> {
        val phone = Validators.normalizePhone(input.phone)
        val nationalId = input.nationalId.filterNot { it.isWhitespace() }
        validate(input, phone, nationalId)?.let { return fail(it) }
        duplicateError(excludeUid = null, phone = phone, nationalId = nationalId)?.let { return fail(it) }

        val uid = auth.createAuthUser(input.email.trim(), input.tempPassword)
            .getOrElse { return fail(OnboardingError.Failed(it)) }
        val user = User(
            uid = uid,
            email = input.email.trim(),
            fullName = input.fullName.trim(),
            phoneNumber = phone,
            nationalId = nationalId,
            address = input.address.trim(),
            role = UserRole.CUSTOMER,
            mustChangePassword = true,
            createdBy = officerUid()
        )
        users.createProfile(user).onFailure { return rollback(it, profileWritten = false, uid) }
        accounts.createChecking(uid).onFailure { return rollback(it, profileWritten = true, uid) }
        auth.releaseCreatedAuthUser()
        return Result.success(user)
    }

    /** Kiểm tra trùng khi sửa hồ sơ của [uid]; trùng với chính mình thì hợp lệ. Trả về null nếu hợp lệ. */
    suspend fun validateEdit(uid: String, phone: String, nationalId: String): OnboardingError? {
        val normalizedPhone = Validators.normalizePhone(phone)
        val normalizedId = nationalId.filterNot { it.isWhitespace() }
        if (!Validators.isValidVnPhone(normalizedPhone)) return OnboardingError.InvalidPhone
        if (!Validators.isValidNationalId(normalizedId)) return OnboardingError.InvalidNationalId
        return duplicateError(excludeUid = uid, phone = normalizedPhone, nationalId = normalizedId)
    }

    /** Khách tự đổi SĐT: chỉ kiểm tra định dạng và `phoneIndex` (khách không được tra CCCD người khác). */
    suspend fun validatePhoneChange(uid: String, phone: String): OnboardingError? {
        val normalized = Validators.normalizePhone(phone)
        if (!Validators.isValidVnPhone(normalized)) return OnboardingError.InvalidPhone
        val owner = users.findUidByPhone(normalized).getOrElse { return OnboardingError.Failed(it) }
        return if (owner != null && owner != uid) OnboardingError.PhoneTaken else null
    }

    private fun validate(input: NewCustomerInput, phone: String, nationalId: String): OnboardingError? = when {
        input.fullName.isBlank() -> OnboardingError.EmptyName
        !Validators.isValidEmail(input.email) -> OnboardingError.InvalidEmail
        !Validators.isValidVnPhone(phone) -> OnboardingError.InvalidPhone
        !Validators.isValidNationalId(nationalId) -> OnboardingError.InvalidNationalId
        !Validators.isStrongEnoughPassword(input.tempPassword) -> OnboardingError.WeakPassword
        else -> null
    }

    private suspend fun duplicateError(excludeUid: String?, phone: String, nationalId: String): OnboardingError? {
        val phoneOwner = users.findUidByPhone(phone).getOrElse { return OnboardingError.Failed(it) }
        if (phoneOwner != null && phoneOwner != excludeUid) return OnboardingError.PhoneTaken
        // Chỉ nhân viên được tra CCCD (rules); khách không tự đổi CCCD nên không gọi tới đây
        val byId = users.findByNationalId(nationalId).getOrElse { return OnboardingError.Failed(it) }
        if (byId != null && byId.uid != excludeUid) return OnboardingError.NationalIdTaken
        return null
    }

    private suspend fun rollback(cause: Throwable, profileWritten: Boolean, uid: String): Result<User> {
        if (profileWritten) users.deleteProfile(uid)
        auth.deleteCreatedAuthUser()
        return fail(OnboardingError.Failed(cause))
    }

    private fun fail(error: OnboardingError): Result<User> = Result.failure(OnboardingException(error))
}
