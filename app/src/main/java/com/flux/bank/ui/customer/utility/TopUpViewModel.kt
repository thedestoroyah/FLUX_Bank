package com.flux.bank.ui.customer.utility

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.flux.bank.FluxApp
import com.flux.bank.core.validation.Validators
import com.flux.bank.data.model.User
import com.flux.bank.data.repository.AccountRepository
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.data.repository.TransactionRepository
import com.flux.bank.data.repository.UserRepository
import com.flux.bank.domain.utility.Carrier
import com.flux.bank.domain.utility.Carriers
import com.flux.bank.domain.utility.ServicePayments

data class TopUpState(
    val phone: String = "",
    val carrier: Carrier? = null,
    val phoneError: Boolean = false,
    val amount: Long? = null
)

/** Nạp tiền điện thoại (mockup F5 màn 3): SĐT mặc định của khách, nhà mạng theo đầu số, mệnh giá cố định. */
class TopUpViewModel(
    accounts: AccountRepository = FluxApp.instance.accountRepository,
    transactions: TransactionRepository = FluxApp.instance.transactionRepository,
    users: UserRepository = FluxApp.instance.userRepository,
    auth: AuthRepository = FluxApp.instance.authRepository,
    now: () -> Long = System::currentTimeMillis
) : ServicePaymentViewModel(accounts, transactions, users, auth, now) {

    private val _topUp = MutableLiveData(TopUpState())
    val topUp: LiveData<TopUpState> = _topUp

    private var phoneEdited = false

    /** Báo lỗi ngay khi đã đủ 10 số mà không nhận được nhà mạng; số còn thiếu chỉ báo lúc bấm nạp. */
    fun setPhone(input: String) {
        phoneEdited = true
        applyPhone(input)
    }

    fun selectAmount(amount: Long) {
        if (amount !in Carriers.DENOMINATIONS) return
        _topUp.value = current().copy(amount = amount)
        revalidate()
    }

    override fun currentAmount(): Long? = current().amount

    override fun onLoaded(user: User?) {
        if (!phoneEdited && user != null) applyPhone(user.phoneNumber)
    }

    override fun draft(): Draft? {
        val s = current()
        val carrier = s.carrier ?: return run { _topUp.value = s.copy(phoneError = true); null }
        val amount = s.amount ?: return null
        return Draft(ServicePayments.topUp(s.phone, carrier, amount), txId = null)
    }

    private fun applyPhone(input: String) {
        val carrier = Carriers.detect(input)
        val complete = Validators.normalizePhone(input).length >= PHONE_LENGTH
        _topUp.value = current().copy(phone = input, carrier = carrier, phoneError = complete && carrier == null)
    }

    private fun current() = _topUp.value!!

    private companion object {
        const val PHONE_LENGTH = 10
    }
}
