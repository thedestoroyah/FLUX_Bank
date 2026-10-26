package com.flux.bank.ui.officer.overview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.bank.FluxApp
import com.flux.bank.data.model.KycRecord
import com.flux.bank.data.model.User
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.data.repository.KycRepository
import com.flux.bank.data.repository.UserRepository
import kotlinx.coroutines.launch

data class OverviewState(
    val loading: Boolean = true,
    val officerName: String = "",
    val customerCount: Int = 0,
    val pendingCount: Int = 0,
    val recentPending: List<Pair<User, KycRecord>> = emptyList()
)

/** Tab Tổng quan của nhân viên: số khách hàng, số hồ sơ eKYC chờ duyệt và vài hồ sơ mới nhất. */
class OfficerOverviewViewModel(
    private val auth: AuthRepository = FluxApp.instance.authRepository,
    private val users: UserRepository = FluxApp.instance.userRepository,
    private val kyc: KycRepository = FluxApp.instance.kycRepository
) : ViewModel() {

    private val _state = MutableLiveData(OverviewState())
    val state: LiveData<OverviewState> = _state

    fun load() {
        viewModelScope.launch {
            val name = auth.currentUid()?.let { users.getUser(it).getOrNull()?.fullName }.orEmpty()
            val queue = kyc.pendingQueue().getOrDefault(emptyList())
            _state.value = OverviewState(
                loading = false,
                officerName = name,
                customerCount = users.countCustomers().getOrDefault(0),
                pendingCount = queue.size,
                recentPending = queue.sortedByDescending { it.second.submittedAt }.take(RECENT_COUNT)
            )
        }
    }

    private companion object {
        const val RECENT_COUNT = 3
    }
}
