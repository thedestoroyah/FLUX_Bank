package com.flux.bank.ui.officer.ekyc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.flux.bank.R
import com.flux.bank.databinding.FragmentEkycReviewBinding
import com.flux.bank.ui.common.screenHost
import com.flux.bank.ui.officer.customers.CustomerAdapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Tab eKYC của nhân viên: hàng đợi hồ sơ chờ duyệt (cũ nhất trước). */
class EkycReviewFragment : Fragment() {

    private var _binding: FragmentEkycReviewBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EkycReviewViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEkycReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val format = SimpleDateFormat("HH:mm · dd/MM", Locale.forLanguageTag("vi"))
        val adapter = CustomerAdapter(subtitle = { user ->
            val record = viewModel.queue.value?.firstOrNull { it.first.uid == user.uid }?.second
            val time = record?.let { format.format(Date(it.submittedAt)) }.orEmpty()
            if (record != null && EkycReviewViewModel.isAssisted(record)) "$time · ${getString(R.string.officer_assisted_tag)}" else time
        }) { user ->
            viewModel.select(user.uid)
            screenHost.openScreen(EkycReviewDetailFragment())
        }
        binding.rvQueue.layoutManager = LinearLayoutManager(requireContext())
        binding.rvQueue.adapter = adapter
        viewModel.loading.observe(viewLifecycleOwner) { binding.progress.isVisible = it }
        viewModel.queue.observe(viewLifecycleOwner) { queue ->
            adapter.submitList(queue.map { it.first })
            binding.tvCount.text = getString(R.string.review_count, queue.size)
            binding.cardList.isVisible = queue.isNotEmpty()
            binding.tvEmpty.isVisible = viewModel.loading.value == false && queue.isEmpty()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.reasonText = { getString(it.label) }
        viewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
