package com.flux.bank.ui.officer.customers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flux.bank.data.model.User
import com.flux.bank.databinding.ItemCustomerBinding
import com.flux.bank.ui.common.showKycPill
import com.flux.bank.ui.customer.home.HomeViewModel

/** Dòng khách hàng: chữ viết tắt, tên, SĐT (hoặc dòng phụ tuỳ chỉnh), pill eKYC. */
class CustomerAdapter(
    private val subtitle: (User) -> String = { it.phoneNumber },
    private val onClick: (User) -> Unit
) : ListAdapter<User, CustomerAdapter.Holder>(DIFF) {

    class Holder(val binding: ItemCustomerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val user = getItem(position)
        with(holder.binding) {
            tvInitials.text = HomeViewModel.initials(user.fullName)
            tvName.text = user.fullName
            tvSub.text = subtitle(user)
            tvPill.showKycPill(user.kycStatus)
            divider.isVisible = position < itemCount - 1
            root.setOnClickListener { onClick(user) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(a: User, b: User) = a.uid == b.uid
            override fun areContentsTheSame(a: User, b: User) = a == b
        }
    }
}
