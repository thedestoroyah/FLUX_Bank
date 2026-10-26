package com.flux.bank.ui.officer.customers

import androidx.annotation.StringRes
import com.flux.bank.R
import com.flux.bank.domain.OnboardingError

/** Các ô nhập trên form khách hàng, để gắn lỗi đúng ô. */
enum class CustomerField { NAME, EMAIL, PHONE, NATIONAL_ID, ADDRESS, PASSWORD }

/** Ánh xạ lỗi nghiệp vụ sang (ô nhập, thông báo); null ô = lỗi chung. */
fun OnboardingError.toFieldError(): Pair<CustomerField?, Int> = when (this) {
    OnboardingError.EmptyName -> CustomerField.NAME to R.string.err_name_required
    OnboardingError.InvalidEmail -> CustomerField.EMAIL to R.string.err_email_invalid
    OnboardingError.InvalidPhone -> CustomerField.PHONE to R.string.err_phone_invalid
    OnboardingError.PhoneTaken -> CustomerField.PHONE to R.string.err_phone_taken
    OnboardingError.InvalidNationalId -> CustomerField.NATIONAL_ID to R.string.err_national_id_invalid
    OnboardingError.NationalIdTaken -> CustomerField.NATIONAL_ID to R.string.err_national_id_taken
    OnboardingError.WeakPassword -> CustomerField.PASSWORD to R.string.err_password_weak
    // Firebase báo email đã tồn tại bằng FirebaseAuthUserCollisionException
    is OnboardingError.Failed ->
        if (cause.javaClass.simpleName.contains("Collision")) CustomerField.EMAIL to R.string.err_email_taken
        else null to R.string.err_generic
}

data class CustomerFormState(
    val fieldErrors: Map<CustomerField, Int> = emptyMap(),
    @StringRes val errorRes: Int? = null,
    val saving: Boolean = false,
    val saved: Boolean = false
)
