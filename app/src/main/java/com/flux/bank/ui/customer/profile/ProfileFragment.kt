package com.flux.bank.ui.customer.profile

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.flux.bank.R
import com.flux.bank.databinding.FragmentProfileBinding
import com.flux.bank.databinding.ViewProfileRowBinding
import com.flux.bank.domain.FaceVerificationPolicy
import com.flux.bank.ui.auth.ChangePasswordActivity
import com.flux.bank.ui.auth.RoleRouter
import com.flux.bank.ui.auth.sheet.AuthSummary
import com.flux.bank.ui.auth.sheet.TransactionAuthSheet
import com.flux.bank.ui.common.screenHost
import com.flux.bank.ui.common.showKycPill
import com.flux.bank.ui.customer.home.HomeViewModel
import com.flux.bank.ui.ekyc.EkycCaptureFragment
import com.flux.bank.utils.BiometricHelper
import com.google.android.material.snackbar.Snackbar

/**
 * Tab Cá nhân. Khách: thông tin (chỉ xem) + bảo mật đầy đủ. Nhân viên ([ARG_OFFICER]): chỉ tên, email,
 * sinh trắc học, đổi mật khẩu và đăng xuất.
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private val isOfficer by lazy { arguments?.getBoolean(ARG_OFFICER) == true }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRows()
        binding.btnLogout.setOnClickListener { RoleRouter.signOut(requireActivity()) }
        childFragmentManager.setFragmentResultListener(FACE_CHECK_KEY, viewLifecycleOwner) { _, result ->
            if (result.getBoolean(TransactionAuthSheet.KEY_VERIFIED)) {
                Toast.makeText(requireContext(), R.string.profile_face_ok, Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.state.observe(viewLifecycleOwner, ::render)
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    private fun setupRows() = with(binding) {
        rowPhone.label(R.string.profile_phone)
        rowNationalId.label(R.string.profile_national_id)
        rowAddress.label(R.string.profile_address)
        rowEdit.action(R.string.profile_edit, R.drawable.ic_pencil, emerald = true) { screenHost.openScreen(EditProfileFragment()) }
        rowEdit.divider.isVisible = false

        rowBiometric.label(R.string.profile_biometric, R.drawable.ic_fingerprint)
        rowBiometric.switchToggle.isVisible = true
        rowFaceCheck.action(R.string.profile_face_check, R.drawable.ic_user_focus) { openFaceCheck() }
        rowEkyc.action(R.string.profile_ekyc, R.drawable.ic_id_card) { openEkyc() }
        rowEkyc.ivChevron.isVisible = false
        rowPassword.action(R.string.profile_change_password, R.drawable.ic_key) {
            startActivity(ChangePasswordActivity.intent(requireContext(), forced = false))
        }
        rowPassword.divider.isVisible = false

        val customerOnly = !isOfficer
        rowNationalId.root.isVisible = customerOnly
        rowAddress.root.isVisible = customerOnly
        rowEdit.root.isVisible = customerOnly
        rowFaceCheck.root.isVisible = customerOnly
        rowEkyc.root.isVisible = customerOnly
        tvKycPill.isVisible = customerOnly

        rowBiometric.switchToggle.isEnabled = biometricAvailable
        if (!biometricAvailable) {
            rowBiometric.root.setOnClickListener {
                Snackbar.make(root, R.string.profile_biometric_unavailable, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private val biometricAvailable by lazy { BiometricHelper.canAuthenticate(requireContext()) }

    private fun render(state: ProfileState) {
        val user = state.user ?: return
        binding.tvName.text = user.fullName
        binding.tvEmail.text = ProfileViewModel.maskEmail(user.email)
        binding.tvInitials.text = HomeViewModel.initials(user.fullName)
        binding.tvKycPill.showKycPill(user.kycStatus)
        renderAvatar(user.avatarBase64)
        binding.rowPhone.tvValue.text = user.phoneNumber
        binding.rowNationalId.tvValue.text = ProfileViewModel.maskNationalId(user.nationalId)
        binding.rowAddress.tvValue.text = user.address
        binding.rowEkyc.tvValue.apply {
            setTextAppearance(R.style.TextAppearance_Flux_Caption)
            setBackgroundResource(R.drawable.bg_pill_capsule)
            val pad = resources.getDimensionPixelSize(R.dimen.space_8)
            setPadding(pad, pad / 2, pad, pad / 2)
            showKycPill(user.kycStatus)
        }
        binding.rowBiometric.switchToggle.setOnCheckedChangeListener(null)
        binding.rowBiometric.switchToggle.isChecked = state.biometricEnabled && biometricAvailable
        binding.rowBiometric.switchToggle.setOnCheckedChangeListener { toggle, checked ->
            if (!checked) return@setOnCheckedChangeListener viewModel.setBiometric(false)
            // Bật phải xác thực sinh trắc học thành công trước
            toggle.isChecked = false
            BiometricHelper.prompt(requireActivity(), onSuccess = { viewModel.setBiometric(true) }, onError = {})
        }
    }

    private fun renderAvatar(base64: String?) {
        val bitmap = base64?.let { runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull() }
            ?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        binding.ivAvatar.isVisible = bitmap != null
        binding.tvInitials.isVisible = bitmap == null
        bitmap?.let(binding.ivAvatar::setImageBitmap)
    }

    private fun openFaceCheck() {
        TransactionAuthSheet.show(
            childFragmentManager,
            AuthSummary(getString(R.string.profile_face_check), emptyList(), FaceVerificationPolicy.HIGH_VALUE_THRESHOLD),
            FACE_CHECK_KEY
        )
    }

    private fun openEkyc() {
        val uid = viewModel.state.value?.user?.uid ?: return
        screenHost.openScreen(EkycCaptureFragment.newInstance(uid, submittedBy = uid))
    }

    private fun ViewProfileRowBinding.label(@StringRes text: Int, @DrawableRes icon: Int? = null) {
        tvLabel.setText(text)
        icon?.let {
            ivIcon.setImageResource(it)
            ivIcon.isVisible = true
        }
    }

    private fun ViewProfileRowBinding.action(@StringRes text: Int, @DrawableRes icon: Int, emerald: Boolean = false, onClick: () -> Unit) {
        label(text, icon)
        ivChevron.isVisible = true
        if (emerald) {
            val color = requireContext().getColor(R.color.flux_emerald_dark)
            tvLabel.setTextColor(color)
            ivIcon.setColorFilter(color)
            ivChevron.setColorFilter(color)
        }
        root.setOnClickListener { onClick() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_OFFICER = "officer"
        private const val FACE_CHECK_KEY = "face_check"

        fun newInstance(officer: Boolean = false) = ProfileFragment().apply { arguments = bundleOf(ARG_OFFICER to officer) }
    }
}
