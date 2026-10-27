package com.flux.bank.ui.customer.account

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.flux.bank.FluxApp
import com.flux.bank.R
import com.flux.bank.databinding.FragmentVnpayBinding
import com.flux.bank.payment.VnPay

/**
 * Mở trang thanh toán VNPay trong WebView và chặn URL trả về. Kết quả gửi qua fragment result [REQUEST_KEY]:
 * có [KEY_QUERY] (query đã decode) khi VNPay trả về, rỗng khi người dùng đóng màn này.
 */
class VnPayFragment : Fragment() {

    private var _binding: FragmentVnpayBinding? = null
    private val binding get() = _binding!!
    private var delivered = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVnpayBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled") // trang thanh toán VNPay cần JavaScript
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.header.tvHeaderTitle.setText(R.string.vnpay_title)
        binding.header.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                _binding?.progress?.apply { progress = newProgress; isVisible = newProgress < FULL_PROGRESS }
            }
        }
        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                interceptReturn(request.url.toString())

            // Dự phòng cho chuyển hướng không đi qua shouldOverrideUrlLoading
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                if (interceptReturn(url)) view.stopLoading()
            }
        }
        if (savedInstanceState == null) binding.webView.loadUrl(requireArguments().getString(ARG_URL)!!)
        else binding.webView.restoreState(savedInstanceState)
    }

    /** URL trả về của VNPay: gửi query cho màn nạp tiền rồi đóng. */
    private fun interceptReturn(url: String): Boolean {
        val vnPay = FluxApp.instance.vnPay ?: return false
        if (!vnPay.isReturnUrl(url)) return false
        if (!delivered) {
            delivered = true
            setFragmentResult(REQUEST_KEY, bundleOf(KEY_QUERY to HashMap(VnPay.parseQuery(url))))
            parentFragmentManager.popBackStack()
        }
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        _binding?.webView?.saveState(outState)
    }

    override fun onDestroyView() {
        // Đóng màn (back) trước khi có kết quả = huỷ; xoay màn hình thì không tính
        if (!delivered && !requireActivity().isChangingConfigurations) {
            delivered = true
            setFragmentResult(REQUEST_KEY, Bundle())
        }
        binding.webView.destroy()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_KEY = "vnpay"
        const val KEY_QUERY = "query"
        private const val ARG_URL = "url"
        private const val FULL_PROGRESS = 100

        fun newInstance(url: String) = VnPayFragment().apply { arguments = bundleOf(ARG_URL to url) }
    }
}
