package com.flux.bank.ui.officer.customers

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.flux.bank.R
import com.flux.bank.databinding.FragmentCreateCustomerBinding
import com.flux.bank.domain.NewCustomerInput
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout

/** Nhân viên tạo khách hàng mới; xong hiện mật khẩu tạm một lần kèm nút sao chép. */
class CreateCustomerFragment : Fragment() {

    private var _binding: FragmentCreateCustomerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CreateCustomerViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateCustomerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.header.tvHeaderTitle.setText(R.string.create_title)
        binding.header.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnGenerate.setOnClickListener { binding.etPassword.setText(CreateCustomerViewModel.generatePassword()) }
        binding.btnCreate.setOnClickListener {
            viewModel.submit(
                NewCustomerInput(
                    fullName = binding.etName.text.toString(),
                    email = binding.etEmail.text.toString(),
                    phone = binding.etPhone.text.toString(),
                    nationalId = binding.etNationalId.text.toString(),
                    address = binding.etAddress.text.toString(),
                    tempPassword = binding.etPassword.text.toString()
                )
            )
        }
        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.btnCreate.isEnabled = !state.saving
            fieldViews().forEach { (field, til) -> til.error = state.fieldErrors[field]?.let(::getString) }
            state.errorRes?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show() }
        }
        viewModel.created.observe(viewLifecycleOwner) { event -> event.consume()?.let(::showTempPassword) }
    }

    private fun fieldViews(): Map<CustomerField, TextInputLayout> = mapOf(
        CustomerField.NAME to binding.tilName,
        CustomerField.EMAIL to binding.tilEmail,
        CustomerField.PHONE to binding.tilPhone,
        CustomerField.NATIONAL_ID to binding.tilNationalId,
        CustomerField.ADDRESS to binding.tilAddress,
        CustomerField.PASSWORD to binding.tilPassword
    )

    private fun showTempPassword(created: CreatedCustomer) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.create_done_title)
            .setMessage(getString(R.string.create_done_body, created.user.fullName) + "\n\n" + created.tempPassword)
            .setCancelable(false)
            .setNeutralButton(R.string.create_copy) { _, _ ->
                requireContext().getSystemService<ClipboardManager>()
                    ?.setPrimaryClip(ClipData.newPlainText("password", created.tempPassword))
                Toast.makeText(requireContext(), R.string.create_copied, Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .setPositiveButton(R.string.action_ok) { _, _ -> parentFragmentManager.popBackStack() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
