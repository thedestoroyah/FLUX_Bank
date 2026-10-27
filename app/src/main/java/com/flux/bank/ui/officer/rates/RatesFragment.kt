package com.flux.bank.ui.officer.rates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.flux.bank.R
import com.flux.bank.core.format.MoneyFormatter
import com.flux.bank.databinding.FragmentRatesBinding
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Tab "Lãi suất" của nhân viên (mockup màn 6): hai thẻ, chạm để sửa. */
class RatesFragment : Fragment() {

    private var _binding: FragmentRatesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RatesViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRatesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.cardSaving.setOnClickListener { EditRateSheet.show(childFragmentManager, RateKind.SAVING) }
        binding.cardMortgage.setOnClickListener { EditRateSheet.show(childFragmentManager, RateKind.MORTGAGE) }
        viewModel.state.observe(viewLifecycleOwner) { s ->
            binding.tvSavingRate.text = getString(R.string.rate_per_year, MoneyFormatter.formatRate(s.rates.savingRate))
            binding.tvMortgageRate.text = getString(R.string.rate_per_year, MoneyFormatter.formatRate(s.rates.mortgageRate))
            val updated = if (s.rates.updatedAt == 0L) getString(R.string.rates_never_updated)
            else getString(R.string.rates_updated_by, s.rates.updatedByName, TIME_FORMAT.format(Date(s.rates.updatedAt)))
            binding.tvSavingNote.text = getString(R.string.rates_saving_note) + " · " + updated
            binding.tvMortgageNote.text = getString(R.string.rates_mortgage_note)
            binding.tvError.isVisible = s.errorRes != null
            s.errorRes?.let(binding.tvError::setText)
        }
        viewModel.saved.observe(viewLifecycleOwner) { e ->
            e.consume()?.let { Snackbar.make(binding.root, R.string.rates_saved, Snackbar.LENGTH_SHORT).show() }
        }
        if (savedInstanceState == null) viewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        val TIME_FORMAT = SimpleDateFormat("dd/MM HH:mm", Locale("vi"))
    }
}
