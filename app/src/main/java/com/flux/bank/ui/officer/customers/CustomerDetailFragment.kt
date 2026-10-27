package com.flux.bank.ui.officer.customers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.flux.bank.FluxApp
import com.flux.bank.R
import com.flux.bank.databinding.FragmentCustomerDetailBinding
import com.flux.bank.ui.common.screenHost
import com.flux.bank.ui.common.showKycPill
import com.flux.bank.ui.ekyc.EkycCaptureFragment
import com.google.android.material.snackbar.Snackbar

/** Nhân viên xem/sửa hồ sơ khách, gửi email đặt lại mật khẩu, chụp eKYC hộ. */
class CustomerDetailFragment : Fragment() {

    private var _binding: FragmentCustomerDetailBinding? = null
    private val binding get() = _binding!!
    private val uid by lazy { requireArguments().getString(ARG_UID)!! }
    private val viewModel: CustomerDetailViewModel by viewModels {
        viewModelFactory { initializer { CustomerDetailViewModel(accounts = FluxApp.instance.accountRepository) } }
    }
    private var filled = false
    private val accountAdapter = CustomerAccountAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCustomerDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.header.tvHeaderTitle.setText(R.string.detail_title)
        binding.header.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnSave.setOnClickListener {
            viewModel.save(
                binding.etName.text.toString(), binding.etPhone.text.toString(),
                binding.etNationalId.text.toString(), binding.etAddress.text.toString()
            )
        }
        binding.btnReset.setOnClickListener { viewModel.resetPassword() }
        binding.rvAccounts.adapter = accountAdapter
        binding.btnOpenAccount.setOnClickListener {
            val user = viewModel.state.value?.user ?: return@setOnClickListener
            screenHost.openScreen(OpenAccountFragment.newInstance(uid, user.fullName, user.phoneNumber))
        }
        binding.btnEkyc.setOnClickListener {
            val officer = FluxApp.instance.authRepository.currentUid() ?: return@setOnClickListener
            screenHost.openScreen(EkycCaptureFragment.newInstance(uid, submittedBy = officer))
        }
        viewModel.message.observe(viewLifecycleOwner) { e -> e.consume()?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show() } }
        viewModel.state.observe(viewLifecycleOwner, ::render)
        viewModel.load(uid)
    }

    private fun render(state: CustomerDetailState) {
        val user = state.user ?: return
        if (!filled) {
            filled = true
            binding.etName.setText(user.fullName)
            binding.etPhone.setText(user.phoneNumber)
            binding.etNationalId.setText(user.nationalId)
            binding.etAddress.setText(user.address)
            binding.etEmail.setText(user.email)
        }
        binding.tvKycPill.showKycPill(user.kycStatus)
        accountAdapter.submit(state.accounts)
        binding.tvNoAccount.isVisible = state.accounts.isEmpty()
        mapOf(
            CustomerField.NAME to binding.tilName,
            CustomerField.PHONE to binding.tilPhone,
            CustomerField.NATIONAL_ID to binding.tilNationalId
        ).forEach { (field, til) -> til.error = state.fieldErrors[field]?.let(::getString) }
        binding.btnSave.isEnabled = !state.saving
        state.errorRes?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show() }
        if (state.saved) Snackbar.make(binding.root, R.string.detail_saved, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_UID = "uid"
        fun newInstance(uid: String) = CustomerDetailFragment().apply { arguments = bundleOf(ARG_UID to uid) }
    }
}
