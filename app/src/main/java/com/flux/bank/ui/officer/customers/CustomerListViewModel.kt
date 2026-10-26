package com.flux.bank.ui.officer.customers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.bank.FluxApp
import com.flux.bank.data.model.KycStatus
import com.flux.bank.data.model.User
import com.flux.bank.data.repository.UserRepository
import kotlinx.coroutines.launch
import java.text.Normalizer

enum class KycFilter { ALL, PENDING, VERIFIED }

/** Danh sách khách hàng cho nhân viên: tìm theo tên (không dấu), SĐT, email và lọc theo trạng thái eKYC. */
class CustomerListViewModel(
    private val users: UserRepository = FluxApp.instance.userRepository
) : ViewModel() {

    private var all: List<User> = emptyList()
    private var query = ""
    private var kycFilter = KycFilter.ALL

    private val _visible = MutableLiveData<List<User>>(emptyList())
    val visible: LiveData<List<User>> = _visible

    private val _loading = MutableLiveData(true)
    val loading: LiveData<Boolean> = _loading

    fun load() {
        viewModelScope.launch {
            all = users.listCustomers().getOrDefault(emptyList()).sortedBy { normalize(it.fullName) }
            _loading.value = false
            apply()
        }
    }

    fun search(text: String) {
        query = normalize(text.trim())
        apply()
    }

    fun filter(value: KycFilter) {
        kycFilter = value
        apply()
    }

    private fun apply() {
        _visible.value = all.filter { user ->
            val statusOk = when (kycFilter) {
                KycFilter.ALL -> true
                KycFilter.PENDING -> user.kycStatus == KycStatus.PENDING
                KycFilter.VERIFIED -> user.kycStatus == KycStatus.VERIFIED
            }
            statusOk && (query.isEmpty() || normalize(user.fullName).contains(query) ||
                user.phoneNumber.contains(query) || user.email.lowercase().contains(query))
        }
    }

    companion object {
        private val MARKS = Regex("\\p{Mn}+")

        /** Bỏ dấu tiếng Việt và viết thường: "Đặng Thu" -> "dang thu". */
        fun normalize(text: String): String =
            MARKS.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "")
                .replace('đ', 'd').replace('Đ', 'D').lowercase()
    }
}
