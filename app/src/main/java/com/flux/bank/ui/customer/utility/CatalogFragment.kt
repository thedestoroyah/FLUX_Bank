package com.flux.bank.ui.customer.utility

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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.flux.bank.R
import com.flux.bank.core.format.MoneyFormatter
import com.flux.bank.databinding.FragmentCatalogBinding
import com.flux.bank.databinding.ItemFilterChipBinding
import com.flux.bank.domain.utility.CatalogKind
import com.flux.bank.domain.utility.CatalogItem
import com.flux.bank.domain.utility.ServiceCatalog
import com.flux.bank.ui.customer.Utility
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Vé máy bay / vé phim / khách sạn / mua sắm (mockup F5 màn 4–7), một layout cho cả bốn [CatalogKind]. */
class CatalogFragment : Fragment() {

    private var _binding: FragmentCatalogBinding? = null
    private val binding get() = _binding!!
    private val kind by lazy { CatalogKind.valueOf(requireArguments().getString(ARG_KIND)!!) }
    private val viewModel: CatalogViewModel by viewModels {
        viewModelFactory { initializer { CatalogViewModel(CatalogKind.valueOf(requireArguments().getString(ARG_KIND)!!)) } }
    }
    private var ui: PaymentUi? = null
    private var adapter: CatalogItemAdapter? = null
    // Chip suất chiếu tạo lại khi đổi phim
    private var showtimesFor: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.header.tvHeaderTitle.setText(titleRes())
        binding.header.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.tvQtyLabel.setText(qtyLabelRes())

        ServiceCatalog.filters(kind).forEach { filter ->
            val chip = ItemFilterChipBinding.inflate(layoutInflater, binding.groupFilters, false).root
            chip.id = View.generateViewId()
            chip.tag = filter.id
            chip.text = filter.label
            binding.groupFilters.addView(chip)
        }
        binding.groupFilters.onChipSelected(viewModel::selectFilter)
        binding.groupShowtimes.onChipSelected(viewModel::selectOption)

        binding.tilDate.isVisible = kind.needsDate
        binding.tilDate.hint = if (kind.needsDate) getString(if (kind == CatalogKind.FLIGHT) R.string.catalog_date_flight else R.string.catalog_date_hotel) else null
        binding.etDate.setOnClickListener { pickDate() }
        (childFragmentManager.findFragmentByTag(DATE_TAG) as? MaterialDatePicker<*>)?.let(::listenDate)

        val itemsAdapter = CatalogItemAdapter(kind, viewModel::selectItem)
        adapter = itemsAdapter
        binding.rvItems.layoutManager =
            if (kind == CatalogKind.SHOP) GridLayoutManager(requireContext(), SHOP_COLUMNS) else LinearLayoutManager(requireContext())
        binding.rvItems.adapter = itemsAdapter

        binding.btnMinus.setOnClickListener { viewModel.decrement() }
        binding.btnPlus.setOnClickListener { viewModel.increment() }
        binding.btnPay.setOnClickListener { viewModel.confirm() }

        ui = PaymentUi(this, viewModel, Utility.fromService(kind.service)!!, binding.kyc, binding.source, binding.tvPayError).also { it.attach() }
        viewModel.catalog.observe(viewLifecycleOwner) { render() }
        viewModel.checkout.observe(viewLifecycleOwner) { render() }
        if (savedInstanceState == null) viewModel.load()
    }

    private fun render() {
        val s = viewModel.catalog.value ?: return
        val c = viewModel.checkout.value ?: return
        ui?.render(c)
        checkChip(binding.groupFilters, s.filterId)
        binding.etDate.setText(s.date?.format(DATE).orEmpty())
        adapter?.submit(s.items, s.itemId)
        renderShowtimes(s.item, s.option)
        binding.tvQty.text = s.quantity.toString()
        binding.btnMinus.isEnabled = s.quantity > kind.quantityRange.first
        binding.btnPlus.isEnabled = s.quantity < kind.quantityRange.last
        binding.tvCheckout.isVisible = kind == CatalogKind.HOTEL && s.date != null
        s.date?.let { binding.tvCheckout.text = getString(R.string.catalog_checkout, it.plusDays(s.quantity.toLong()).format(DATE)) }
        binding.tvTotalLabel.text = getString(totalLabelRes(), s.quantity)
        binding.tvTotal.text = MoneyFormatter.formatVnd(s.total)
        val ready = s.item != null && (s.item?.options.isNullOrEmpty() || s.option != null)
        binding.btnPay.isEnabled = ui?.canPay(c) == true && ready
    }

    /** Suất chiếu của phim đang chọn; suất đã qua bị tắt. */
    private fun renderShowtimes(item: CatalogItem?, selected: String?) {
        val options = item?.options.orEmpty()
        binding.sectionShowtimes.isVisible = options.isNotEmpty()
        if (item?.id != showtimesFor) {
            showtimesFor = item?.id
            binding.groupShowtimes.removeAllViews()
            val now = System.currentTimeMillis()
            options.forEach { time ->
                val chip = ItemFilterChipBinding.inflate(layoutInflater, binding.groupShowtimes, false).root
                chip.id = View.generateViewId()
                chip.tag = time
                chip.text = time
                chip.isEnabled = !ServiceCatalog.isShowtimePast(time, now)
                binding.groupShowtimes.addView(chip)
            }
        }
        if (selected == null) binding.groupShowtimes.clearCheck() else checkChip(binding.groupShowtimes, selected)
    }

    private fun pickDate() {
        if (childFragmentManager.findFragmentByTag(DATE_TAG) != null) return
        val range = ServiceCatalog.dateRange(kind, System.currentTimeMillis()) ?: return
        val start = utcMillis(range.start)
        val end = utcMillis(range.endInclusive)
        val constraints = CalendarConstraints.Builder()
            .setStart(start).setEnd(end)
            .setValidator(CompositeDateValidator.allOf(listOf(DateValidatorPointForward.from(start), DateValidatorPointBackward.before(end + DAY_MILLIS))))
            .build()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setSelection(viewModel.catalog.value?.date?.let(::utcMillis) ?: start)
            .setCalendarConstraints(constraints)
            .build()
        listenDate(picker)
        picker.show(childFragmentManager, DATE_TAG)
    }

    private fun listenDate(picker: MaterialDatePicker<*>) {
        @Suppress("UNCHECKED_CAST")
        (picker as MaterialDatePicker<Long>).addOnPositiveButtonClickListener { millis ->
            viewModel.setDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
        }
    }

    private fun checkChip(group: ChipGroup, tag: String) {
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as Chip
            if (chip.tag == tag && !chip.isChecked) chip.isChecked = true
        }
    }

    private fun ChipGroup.onChipSelected(select: (String) -> Unit) = setOnCheckedStateChangeListener { group, ids ->
        (group.findViewById<View>(ids.firstOrNull() ?: return@setOnCheckedStateChangeListener)?.tag as? String)?.let(select)
    }

    private fun titleRes() = when (kind) {
        CatalogKind.FLIGHT -> R.string.catalog_title_flight
        CatalogKind.MOVIE -> R.string.catalog_title_movie
        CatalogKind.HOTEL -> R.string.catalog_title_hotel
        CatalogKind.SHOP -> R.string.catalog_title_shop
    }

    private fun qtyLabelRes() = when (kind) {
        CatalogKind.FLIGHT -> R.string.catalog_qty_flight
        CatalogKind.MOVIE -> R.string.catalog_qty_movie
        CatalogKind.HOTEL -> R.string.catalog_qty_hotel
        CatalogKind.SHOP -> R.string.catalog_qty_shop
    }

    private fun totalLabelRes() = when (kind) {
        CatalogKind.FLIGHT -> R.string.catalog_total_flight
        CatalogKind.MOVIE -> R.string.catalog_total_movie
        CatalogKind.HOTEL -> R.string.catalog_total_hotel
        CatalogKind.SHOP -> R.string.catalog_total_shop
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ui = null
        adapter = null
        showtimesFor = null
        _binding = null
    }

    companion object {
        private const val ARG_KIND = "kind"
        private const val DATE_TAG = "catalog_date"
        private const val SHOP_COLUMNS = 2
        private const val DAY_MILLIS = 86_400_000L
        private val DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        // MaterialDatePicker làm việc với nửa đêm UTC của ngày được chọn
        private fun utcMillis(date: LocalDate) = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        fun newInstance(kind: CatalogKind) = CatalogFragment().apply { arguments = bundleOf(ARG_KIND to kind.name) }
    }
}
