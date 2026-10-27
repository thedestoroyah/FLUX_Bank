package com.flux.bank.ui.customer.qr

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.flux.bank.FluxApp
import com.flux.bank.R
import com.flux.bank.core.format.TextNormalizer
import com.flux.bank.data.model.AccountType
import com.flux.bank.data.model.BankAccount
import com.flux.bank.databinding.FragmentQrBinding
import com.flux.bank.databinding.ItemFilterChipBinding
import com.flux.bank.domain.QrPayload
import com.flux.bank.ui.common.screenHost
import com.flux.bank.ui.customer.home.HomeViewModel
import com.flux.bank.ui.customer.transfer.TransferFragment
import com.flux.bank.ui.customer.transfer.TransferMode
import com.flux.bank.ui.customer.transfer.TransferPrefill
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Nút QR nổi giữa thanh tab (mockup F4 màn 9–10): quét mã FLUX để mở form chuyển đã điền sẵn,
 * hoặc hiện mã của tài khoản mình để người khác quét.
 */
class QrFragment : Fragment() {

    private var _binding: FragmentQrBinding? = null
    private val binding get() = _binding!!
    private var scanner: BarcodeScanner? = null
    private var executor: ExecutorService? = null
    private var provider: ProcessCameraProvider? = null
    private var handled = false
    private var lastWarning = 0L
    private var accounts: List<BankAccount> = emptyList()
    private var holderName = ""

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (_binding == null) return@registerForActivityResult
        if (granted) startCamera() else {
            Snackbar.make(binding.root, R.string.qr_camera_denied, Snackbar.LENGTH_LONG).show()
            binding.toggleMode.check(R.id.btnMine)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentQrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.header.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.toggleMode.addOnButtonCheckedListener { _, id, checked -> if (checked) showMode(scan = id == R.id.btnScan) }
        binding.groupAccounts.setOnCheckedStateChangeListener { group, ids ->
            val chip = group.findViewById<View>(ids.firstOrNull() ?: return@setOnCheckedStateChangeListener)
            accounts.firstOrNull { it.accountId == chip?.tag }?.let(::showMyCode)
        }
        loadAccounts()
        showMode(scan = binding.toggleMode.checkedButtonId == R.id.btnScan)
    }

    private fun showMode(scan: Boolean) {
        binding.header.tvHeaderTitle.setText(if (scan) R.string.tab_qr else R.string.qr_my_code)
        binding.header.tvHeaderTitle.setTextColor(
            ContextCompat.getColor(requireContext(), if (scan) R.color.flux_text_on_dark else R.color.flux_text_primary)
        )
        binding.scanFrame.isVisible = scan
        binding.tvScanHint.isVisible = scan
        binding.myCode.isVisible = !scan
        if (!scan) return stopCamera()
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        if (provider != null) return
        val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
        val executor = Executors.newSingleThreadExecutor()
        this.scanner = scanner
        this.executor = executor
        // Lấy sẵn main executor: analyzer chạy trên luồng camera, có thể sau khi fragment đã rời màn hình
        val main = ContextCompat.getMainExecutor(requireContext())
        val future = ProcessCameraProvider.getInstance(requireContext())
        future.addListener({
            val cameraProvider = future.get()
            val binding = _binding ?: return@addListener
            val preview = Preview.Builder().build().also { it.surfaceProvider = binding.previewView.surfaceProvider }
            val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            analysis.setAnalyzer(executor) { image -> analyze(scanner, image, main) }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            provider = cameraProvider
        }, main)
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analyze(scanner: BarcodeScanner, image: ImageProxy, main: java.util.concurrent.Executor) {
        val media = image.image ?: return image.close()
        scanner.process(InputImage.fromMediaImage(media, image.imageInfo.rotationDegrees))
            .addOnSuccessListener(main) { codes ->
                codes.firstNotNullOfOrNull { it.rawValue }?.let(::onCode)
            }
            .addOnCompleteListener { image.close() }
    }

    /** Mã FLUX: mở form chuyển đã điền số tài khoản (một lần). Mã khác: nhắc tối đa mỗi 2 giây. */
    private fun onCode(text: String) {
        val binding = _binding ?: return
        if (handled) return
        val number = QrPayload.decode(text)
        if (number == null) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastWarning > WARNING_INTERVAL_MS) {
                lastWarning = now
                Snackbar.make(binding.root, R.string.qr_not_flux, Snackbar.LENGTH_SHORT).show()
            }
            return
        }
        handled = true
        stopCamera()
        parentFragmentManager.popBackStack()
        screenHost.openScreen(TransferFragment.newInstance(TransferMode.INTERNAL, TransferPrefill(number)))
    }

    private fun stopCamera() {
        provider?.unbindAll()
        provider = null
        scanner?.close()
        scanner = null
        executor?.shutdown()
        executor = null
    }

    private fun loadAccounts() {
        val uid = FluxApp.instance.authRepository.currentUid() ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            holderName = TextNormalizer.holderName(FluxApp.instance.userRepository.getUser(uid).getOrNull()?.fullName.orEmpty())
            accounts = FluxApp.instance.accountRepository.listByOwner(uid).getOrNull().orEmpty().filter { it.type != AccountType.MORTGAGE }
            val binding = _binding ?: return@launch
            binding.groupAccounts.removeAllViews()
            accounts.forEach { account ->
                val chip = ItemFilterChipBinding.inflate(layoutInflater, binding.groupAccounts, false).root
                chip.id = View.generateViewId()
                chip.tag = account.accountId
                chip.setText(HomeViewModel.labelFor(account.type))
                binding.groupAccounts.addView(chip)
            }
            (binding.groupAccounts.getChildAt(0) as? Chip)?.isChecked = true
        }
    }

    private fun showMyCode(account: BankAccount) {
        binding.ivQr.setImageBitmap(qrBitmap(QrPayload.encode(account.accountNumber)))
        binding.tvName.text = holderName
        binding.btnCopy.text = getString(R.string.move_account_chip, getString(HomeViewModel.labelFor(account.type)), account.accountNumber)
        binding.btnCopy.setOnClickListener {
            requireContext().getSystemService<ClipboardManager>()
                ?.setPrimaryClip(ClipData.newPlainText(account.accountNumber, account.accountNumber))
            Snackbar.make(binding.root, R.string.detail_copied, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun qrBitmap(text: String): Bitmap {
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE)
        val dark = ContextCompat.getColor(requireContext(), R.color.flux_primary)
        val pixels = IntArray(QR_SIZE * QR_SIZE) { i -> if (matrix[i % QR_SIZE, i / QR_SIZE]) dark else Color.WHITE }
        return Bitmap.createBitmap(pixels, QR_SIZE, QR_SIZE, Bitmap.Config.ARGB_8888)
    }

    override fun onDestroyView() {
        stopCamera()
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val QR_SIZE = 512
        const val WARNING_INTERVAL_MS = 2_000L
    }
}
