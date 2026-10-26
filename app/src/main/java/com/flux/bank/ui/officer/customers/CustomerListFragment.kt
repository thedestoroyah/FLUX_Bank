package com.flux.bank.ui.officer.customers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.flux.bank.R
import com.flux.bank.databinding.FragmentCustomerListBinding
import com.flux.bank.ui.common.screenHost

/** Tab Khách hàng: tìm kiếm, lọc theo eKYC, mở chi tiết hoặc tạo mới. */
class CustomerListFragment : Fragment() {

    private var _binding: FragmentCustomerListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CustomerListViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCustomerListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = CustomerAdapter { screenHost.openScreen(CustomerDetailFragment.newInstance(it.uid)) }
        binding.rvCustomers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCustomers.adapter = adapter
        binding.etSearch.doAfterTextChanged { viewModel.search(it?.toString().orEmpty()) }
        binding.chips.setOnCheckedStateChangeListener { _, ids ->
            viewModel.filter(
                when (ids.firstOrNull()) {
                    R.id.chipPending -> KycFilter.PENDING
                    R.id.chipVerified -> KycFilter.VERIFIED
                    else -> KycFilter.ALL
                }
            )
        }
        binding.fabCreate.setOnClickListener { screenHost.openScreen(CreateCustomerFragment()) }
        viewModel.loading.observe(viewLifecycleOwner) { binding.progress.isVisible = it }
        viewModel.visible.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            val loaded = viewModel.loading.value == false
            binding.cardList.isVisible = list.isNotEmpty()
            binding.tvEmpty.isVisible = loaded && list.isEmpty()
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
