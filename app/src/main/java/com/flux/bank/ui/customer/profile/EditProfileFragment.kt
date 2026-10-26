package com.flux.bank.ui.customer.profile

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.flux.bank.R
import com.flux.bank.databinding.FragmentEditProfileBinding
import com.flux.bank.ui.auth.sheet.AuthSummary
import com.flux.bank.ui.auth.sheet.TransactionAuthSheet
import com.flux.bank.ui.customer.home.HomeViewModel
import com.flux.bank.utils.ImageUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Khách sửa SĐT (qua OTP), địa chỉ, ảnh đại diện; họ tên/CCCD/email chỉ xem. */
class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EditProfileViewModel by viewModels()
    private var avatarBase64: String? = null
    private var filled = false

    private val pickAvatar = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@registerForActivityResult
        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) { ImageUtils.decodeSampled(requireContext(), uri, AVATAR_DIM) } ?: return@launch
            avatarBase64 = withContext(Dispatchers.Default) { ImageUtils.bitmapToBase64(bitmap, quality = 80, maxDim = AVATAR_DIM) }
            showAvatar(bitmap)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.header.tvHeaderTitle.setText(R.string.edit_title)
        binding.header.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.avatarFrame.setOnClickListener {
            pickAvatar.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.btnSave.setOnClickListener {
            viewModel.save(binding.etPhone.text.toString(), binding.etAddress.text.toString(), avatarBase64)
        }
        childFragmentManager.setFragmentResultListener(PHONE_KEY, viewLifecycleOwner) { _, result ->
            viewModel.onAuthResult(result.getBoolean(TransactionAuthSheet.KEY_VERIFIED))
        }
        viewModel.authRequest.observe(viewLifecycleOwner) { event ->
            event.consume()?.let { phone ->
                TransactionAuthSheet.show(
                    childFragmentManager,
                    AuthSummary(getString(R.string.edit_phone_sheet_title), listOf(getString(R.string.edit_phone_sheet_row) to phone), null),
                    PHONE_KEY
                )
            }
        }
        viewModel.state.observe(viewLifecycleOwner, ::render)
        viewModel.load()
    }

    private fun render(state: EditProfileState) {
        val user = state.user
        if (user != null && !filled) {
            filled = true
            binding.etPhone.setText(user.phoneNumber)
            binding.etAddress.setText(user.address)
            binding.etName.setText(user.fullName)
            binding.etNationalId.setText(user.nationalId)
            binding.etEmail.setText(user.email)
            binding.tvInitials.text = HomeViewModel.initials(user.fullName)
            user.avatarBase64?.let { b64 ->
                runCatching { Base64.decode(b64, Base64.NO_WRAP) }.getOrNull()
                    ?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }?.let(::showAvatar)
            }
        }
        binding.tilPhone.error = state.phoneError?.let(::getString)
        binding.btnSave.isEnabled = !state.saving
        state.errorRes?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show() }
        if (state.saved) {
            Snackbar.make(binding.root, R.string.edit_saved, Snackbar.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    private fun showAvatar(bitmap: android.graphics.Bitmap) {
        binding.ivAvatar.setImageBitmap(bitmap)
        binding.tvInitials.isVisible = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val PHONE_KEY = "edit_phone"
        const val AVATAR_DIM = 256
    }
}
