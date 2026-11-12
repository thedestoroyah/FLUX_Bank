package com.flux.bank.ui.customer.assistant

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.flux.bank.R
import com.flux.bank.databinding.FragmentAssistantBinding
import com.flux.bank.domain.assistant.AssistantAction
import com.flux.bank.ui.common.screenHost
import com.flux.bank.ui.customer.UtilityScreens
import com.flux.bank.ui.customer.history.HistoryFragment
import com.flux.bank.ui.customer.transfer.TransferHubFragment

/** Màn Trợ lý FLUX (mockup F7 khung 1–3): chat với Gemini, nút dưới câu trả lời mở màn thật của app. */
class AssistantFragment : Fragment() {

    private var _binding: FragmentAssistantBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AssistantViewModel by viewModels()
    private val adapter = ChatAdapter(month = { viewModel.currentMonth.monthValue }, onAction = ::open)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAssistantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        applyInsets()
        binding.header.tvHeaderTitle.setText(R.string.assistant_title)
        binding.header.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.rvChat.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        binding.rvChat.adapter = adapter
        bindSuggestions()
        bindInput()

        viewModel.firstName.observe(viewLifecycleOwner) { binding.tvHello.text = getString(R.string.assistant_hello, it) }
        viewModel.items.observe(viewLifecycleOwner) { items ->
            binding.emptyState.isVisible = items.isEmpty()
            binding.rvChat.isVisible = items.isNotEmpty()
            adapter.submit(items)
            if (items.isNotEmpty()) binding.rvChat.scrollToPosition(items.lastIndex)
        }
        viewModel.busy.observe(viewLifecycleOwner) { renderSendState() }
    }

    // Cửa sổ edge-to-edge: tự cộng thanh hệ thống, và đẩy thanh nhập lên trên bàn phím (lấy cái lớn hơn ở đáy)
    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = maxOf(bars.bottom, ime.bottom))
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun bindSuggestions() {
        listOf(binding.chipTopup, binding.chipElectricity, binding.chipMonth, binding.chipBranch).forEach { chip ->
            chip.setOnClickListener { viewModel.send(chip.text.toString()) }
        }
    }

    private fun bindInput() {
        binding.etMessage.doAfterTextChanged { renderSendState() }
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            (actionId == EditorInfo.IME_ACTION_SEND).also { if (it) submit() }
        }
        binding.btnSend.setOnClickListener { submit() }
        renderSendState()
    }

    private fun submit() {
        val text = binding.etMessage.text?.toString().orEmpty()
        if (text.isBlank() || viewModel.busy.value == true) return
        viewModel.send(text)
        binding.etMessage.text = null
    }

    // Nút gửi: emerald khi có chữ và không chờ trả lời, ngược lại obsidian mờ (quy tắc emerald = điểm tương tác)
    private fun renderSendState() {
        val busy = viewModel.busy.value == true
        val ready = !busy && !binding.etMessage.text.isNullOrBlank()
        binding.btnSend.isEnabled = ready
        binding.btnSend.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), if (ready) R.color.flux_emerald else R.color.flux_primary)
        )
        binding.btnSend.alpha = if (ready) 1f else DISABLED_ALPHA
        binding.groupSuggest.isEnabled = !busy
    }

    private fun open(action: AssistantAction) {
        val screen = AssistantActionUi.utilityFor(action)?.let { UtilityScreens.screenFor(it, viewModel.savingAccountId()) }
            ?: when (action) {
                // Mở màn chọn kiểu chuyển (trong FLUX / tài khoản của tôi / liên ngân hàng) dạng màn phụ; showTab() sẽ xoá back stack và mất chat
                AssistantAction.TRANSFER -> TransferHubFragment()
                else -> HistoryFragment.forMonth(viewModel.currentMonth)
            }
        screenHost.openScreen(screen)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val DISABLED_ALPHA = 0.35f
    }
}
