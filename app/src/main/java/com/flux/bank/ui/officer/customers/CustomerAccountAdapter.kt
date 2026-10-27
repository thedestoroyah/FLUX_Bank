package com.flux.bank.ui.officer.customers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.flux.bank.R
import com.flux.bank.core.format.MoneyFormatter
import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.BankAccount
import com.flux.bank.databinding.ItemCustomerAccountBinding
import com.flux.bank.ui.customer.home.HomeViewModel

/** Danh sách tài khoản của khách trong màn chi tiết khách (nhân viên xem). */
class CustomerAccountAdapter : RecyclerView.Adapter<CustomerAccountAdapter.Holder>() {

    private var items: List<BankAccount> = emptyList()

    class Holder(val binding: ItemCustomerAccountBinding) : RecyclerView.ViewHolder(binding.root)

    fun submit(accounts: List<BankAccount>) {
        items = accounts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemCustomerAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val account = items[position]
        with(holder.binding) {
            tvType.setText(HomeViewModel.labelFor(account.type))
            tvNumber.text = account.accountNumber
            tvBalanceLabel.setText(if (account.type == AccountType.MORTGAGE) R.string.card_outstanding else R.string.card_available_balance)
            tvBalance.text = MoneyFormatter.formatVnd(account.balance)
        }
    }
}
