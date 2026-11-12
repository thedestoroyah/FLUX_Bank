package com.flux.bank.ui.customer.utility

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.flux.bank.FluxApp
import com.flux.bank.R
import com.flux.bank.core.format.MoneyFormatter
import com.flux.bank.data.model.BankAccount
import com.flux.bank.data.model.ServicePayment
import com.flux.bank.databinding.ItemFilterChipBinding
import com.flux.bank.databinding.ViewPaymentKycBinding
import com.flux.bank.databinding.ViewPaymentSourceBinding
import com.flux.bank.domain.TransferError
import com.flux.bank.ui.auth.sheet.AuthSummary
import com.flux.bank.ui.auth.sheet.TransactionAuthSheet
import com.flux.bank.ui.common.screenHost
import com.flux.bank.ui.customer.Utility
import com.flux.bank.ui.customer.account.ReceiptFragment
import com.flux.bank.ui.customer.home.HomeViewModel
import com.flux.bank.ui.ekyc.EkycCaptureFragment
import com.google.android.material.chip.Chip

/**
 * Phần giao diện chung của các màn tiện ích F5: thẻ eKYC, chip nguồn tiền, lỗi, sheet xác thực và mở biên lai.
 * Tạo trong `onViewCreated` (giữ binding của view hiện tại), gọi [render] mỗi khi [ServicePaymentViewModel.checkout] đổi.
 */
class PaymentUi(
    private val fragment: Fragment,
    private val vm: ServicePaymentViewModel,
    private val utility: Utility,
    private val kyc: ViewPaymentKycBinding,
    private val source: ViewPaymentSourceBinding,
    // Màn có thanh đáy cố định (danh mục) hiện lỗi ở đó để không bị khuất khi đang cuộn
    private val errorView: TextView? = null
) {
    // Chip được tạo lại khi danh sách tài khoản đổi
    private var chipIds: List<String> = emptyList()

    /** Nối sự kiện: chọn nguồn, nút eKYC, sheet xác thực, biên lai khi ghi xong. */
    fun attach() {
        val owner = fragment.viewLifecycleOwner
        val fm = fragment.childFragmentManager
        source.groupSources.setOnCheckedStateChangeListener { group, ids ->
            (group.findViewById<View>(ids.firstOrNull() ?: return@setOnCheckedStateChangeListener)?.tag as? String)
                ?.let(vm::selectSource)
        }
        kyc.btnKyc.setOnClickListener {
            val uid = FluxApp.instance.authRepository.currentUid() ?: return@setOnClickListener
            fragment.screenHost.openScreen(EkycCaptureFragment.newInstance(uid, submittedBy = uid))
        }
        fm.setFragmentResultListener(AUTH_KEY, owner) { _, result ->
            vm.onAuthResult(result.getBoolean(TransactionAuthSheet.KEY_VERIFIED))
        }
        vm.askAuth.observe(owner) { e -> e.consume()?.let(::showAuth) }
        vm.done.observe(owner) { e ->
            e.consume()?.let { tx ->
                val label = sourceLabel()
                fragment.parentFragmentManager.popBackStack()
                fragment.screenHost.openScreen(ReceiptFragment.newInstance(tx, label, viewOnly = false))
            }
        }
    }

    fun render(s: CheckoutState) {
        val context = fragment.requireContext()
        kyc.cardKyc.isVisible = s.kycBlocked
        val ids = s.sources.map { it.accountId }
        if (ids != chipIds) {
            source.groupSources.removeAllViews()
            s.sources.forEach { account ->
                val chip = ItemFilterChipBinding.inflate(fragment.layoutInflater, source.groupSources, false).root
                chip.id = View.generateViewId()
                chip.tag = account.accountId
                source.groupSources.addView(chip)
            }
            chipIds = ids
        }
        for (i in 0 until source.groupSources.childCount) {
            val chip = source.groupSources.getChildAt(i) as Chip
            val account = s.sources.firstOrNull { it.accountId == chip.tag } ?: continue
            chip.text = context.getString(
                R.string.move_account_chip, context.getString(HomeViewModel.labelFor(account.type)), MoneyFormatter.formatVnd(account.balance)
            )
            if (account.accountId == s.sourceId && !chip.isChecked) chip.isChecked = true
        }
        val amountText = s.amountError?.let { errorText(it, s.remainingToday) }
        val errorText = s.errorRes?.let(fragment::getString)
        if (errorView != null) {
            val text = amountText ?: errorText
            errorView.isVisible = text != null
            errorView.text = text
        } else {
            source.tvAmountError.isVisible = amountText != null
            source.tvAmountError.text = amountText
            source.tvError.isVisible = errorText != null
            source.tvError.text = errorText
        }
    }

    /** Nút trả tiền chỉ bật khi không đang ghi, đã eKYC, có tài khoản nguồn và số tiền không vượt số dư/hạn mức. */
    fun canPay(s: CheckoutState): Boolean = !s.submitting && !s.kycBlocked && s.source != null && s.amountError == null

    /** Nhãn tài khoản cho biên lai, ví dụ "Thanh toán · 886812034471". */
    fun accountLabel(account: BankAccount?): String = account?.let {
        fragment.getString(HomeViewModel.labelFor(it.type)) + " · " + it.accountNumber
    }.orEmpty()

    private fun sourceLabel() = accountLabel(vm.checkout.value?.source)

    private fun showAuth(payment: ServicePayment) {
        val rows = listOf(fragment.getString(R.string.receipt_service) to fragment.getString(utility.label)) +
            payment.details.map { it.label to it.value } +
            listOf(
                fragment.getString(R.string.pay_from) to sourceLabel(),
                fragment.getString(R.string.move_sheet_amount) to MoneyFormatter.formatVnd(payment.amount)
            )
        TransactionAuthSheet.show(
            fragment.childFragmentManager,
            AuthSummary(fragment.getString(R.string.pay_confirm_title), rows, payment.amount),
            AUTH_KEY
        )
    }

    private fun errorText(error: TransferError, remainingToday: Long): String = when (error) {
        TransferError.BELOW_MIN -> fragment.getString(R.string.err_amount_min)
        TransferError.ABOVE_MAX -> fragment.getString(R.string.err_pay_max)
        TransferError.INSUFFICIENT_FUNDS -> fragment.getString(R.string.err_amount_balance)
        TransferError.DAILY_LIMIT -> fragment.getString(R.string.err_pay_daily, MoneyFormatter.formatVnd(remainingToday))
        else -> fragment.getString(R.string.err_generic)
    }

    private companion object {
        const val AUTH_KEY = "utility_auth"
    }
}
