package com.flux.bank.ui.officer.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.flux.bank.R
import com.flux.bank.databinding.FragmentOfficerOverviewBinding
import com.flux.bank.databinding.ItemCustomerBinding
import com.flux.bank.ui.common.screenHost
import com.flux.bank.ui.customer.home.HomeViewModel
import com.flux.bank.ui.officer.OfficerMainActivity
import com.flux.bank.ui.officer.customers.CreateCustomerFragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Tab Tổng quan: số liệu nhanh, nút tạo khách, các hồ sơ eKYC chờ duyệt mới nhất. */
class OfficerOverviewFragment : Fragment() {

    private var _binding: FragmentOfficerOverviewBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OfficerOverviewViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOfficerOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val host = requireActivity() as OfficerMainActivity
        binding.btnCreate.setOnClickListener { screenHost.openScreen(CreateCustomerFragment()) }
        binding.cardCustomers.setOnClickListener { host.showTab(OfficerMainActivity.OfficerTab.CUSTOMERS) }
        binding.cardPending.setOnClickListener { host.showTab(OfficerMainActivity.OfficerTab.EKYC) }
        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.tvName.text = state.officerName
            binding.tvCustomerCount.text = if (state.loading) "–" else state.customerCount.toString()
            binding.tvPendingCount.text = if (state.loading) "–" else state.pendingCount.toString()
            binding.listRecent.removeAllViews()
            state.recentPending.forEachIndexed { i, (user, record) ->
                val row = ItemCustomerBinding.inflate(layoutInflater, binding.listRecent, true)
                row.tvInitials.text = HomeViewModel.initials(user.fullName)
                row.tvName.text = user.fullName
                val time = SimpleDateFormat("HH:mm dd/MM", Locale.forLanguageTag("vi")).format(Date(record.submittedAt))
                row.tvSub.text = if (record.submittedBy != user.uid) "$time · ${getString(R.string.officer_assisted_tag)}" else time
                row.tvPill.isVisible = false
                row.divider.isVisible = i < state.recentPending.size - 1
                row.root.setOnClickListener { host.showTab(OfficerMainActivity.OfficerTab.EKYC) }
            }
            binding.tvNoPending.isVisible = !state.loading && state.recentPending.isEmpty()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
