package com.flux.bank.ui.auth.sheet

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.flux.bank.FluxApp
import com.flux.bank.R
import com.flux.bank.databinding.ItemSummaryRowBinding
import com.flux.bank.databinding.SheetTransactionAuthBinding
import com.flux.bank.ui.common.SimulatedSmsBanner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Nội dung tóm tắt hiển thị trên sheet. [amount] null = thao tác không có số tiền (chỉ cần OTP). */
data class AuthSummary(val title: String, val rows: List<Pair<String, String>>, val amount: Long?)

/**
 * Bottom sheet xác thực dùng chung cho mọi giao dịch: OTP (bàn phím riêng) và, với giao dịch giá trị cao,
 * thêm bước khuôn mặt. Kết quả trả qua Fragment Result API: key = requestKey, bundle [KEY_VERIFIED] (Boolean).
 */
class TransactionAuthSheet : BottomSheetDialogFragment() {

    private var _binding: SheetTransactionAuthBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransactionAuthViewModel by viewModels()
    private var resultSent = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetTransactionAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        // Mở rộng hết cỡ ngay: sheet có bàn phím riêng nên luôn đủ chỗ
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val args = requireArguments()
        val amount = args.getLong(ARG_AMOUNT, NO_AMOUNT).takeIf { it != NO_AMOUNT }
        binding.tvTitle.text = args.getString(ARG_TITLE)
        renderSummary(args.getStringArrayList(ARG_LABELS).orEmpty(), args.getStringArrayList(ARG_VALUES).orEmpty())

        binding.keypad.onDigit = viewModel::onDigit
        binding.keypad.onBackspace = viewModel::onBackspace
        binding.btnConfirm.setOnClickListener { viewModel.confirm() }
        binding.btnResend.setOnClickListener { viewModel.resend() }

        val notifier = FluxApp.instance.otpNotifier
        notifier.autofill.observe(viewLifecycleOwner) { event -> event.consume()?.let(viewModel::fill) }
        notifier.inAppSms.observe(viewLifecycleOwner) { event ->
            event.consume()?.let { code ->
                SimulatedSmsBanner.show(requireActivity(), notifier.smsText(code)) { notifier.requestAutofill(code) }
            }
        }
        viewModel.state.observe(viewLifecycleOwner, ::render)
        viewModel.start(amount)

        viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                viewModel.tick()
                delay(TICK_MS)
            }
        }
    }

    private fun render(state: AuthSheetState) {
        binding.groupOtp.isVisible = state is AuthSheetState.EnteringOtp
        binding.face.root.isVisible = state is AuthSheetState.FaceStep
        when (state) {
            is AuthSheetState.EnteringOtp -> renderOtp(state)
            is AuthSheetState.FaceStep -> renderFace(state)
            AuthSheetState.Verified -> finishWith(true)
            AuthSheetState.Locked -> failWith(R.string.otp_locked)
            is AuthSheetState.Failed -> failWith(state.messageRes)
        }
    }

    private fun failWith(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_LONG).show()
        finishWith(false)
    }

    private fun renderFace(state: AuthSheetState.FaceStep) {
        val face = binding.face
        binding.tvTitle.setText(R.string.face_title)
        binding.summaryRows.isVisible = false
        face.btnFaceCancel.setOnClickListener { finishWith(false) }
        face.tvFaceMessage.setTextColor(requireContext().getColor(if (state.messageRes != null) R.color.flux_error_dark else R.color.flux_text_secondary))
        face.tvFaceMessage.text = state.messageRes?.let(::getString) ?: getString(R.string.face_hold)
        face.matchOverlay.isVisible = state.phase == FacePhase.MATCHED
        face.ivMatched.isVisible = state.phase == FacePhase.MATCHED
        face.ring.isIndeterminate = state.phase == FacePhase.CHECKING || state.phase == FacePhase.MATCHING
        when (state.phase) {
            FacePhase.CHECKING -> face.tvInstruction.text = ""
            FacePhase.BLOCKED -> {
                face.tvInstruction.text = ""
                stopCamera()
            }
            FacePhase.BLINK -> {
                face.tvInstruction.setText(R.string.face_blink)
                face.ring.setProgressCompat(PROGRESS_BLINK, true)
                if (state.attemptsLeft < MAX_FACE_ATTEMPTS) {
                    face.tvFaceMessage.text = getString(R.string.face_mismatch) + " " +
                        getString(R.string.face_attempts_left, state.attemptsLeft)
                }
                ensureCamera()?.retry()
            }
            FacePhase.MATCHING -> face.tvInstruction.setText(R.string.face_matching)
            FacePhase.MATCHED -> {
                face.tvInstruction.setText(R.string.face_matched)
                face.tvFaceMessage.setText(R.string.face_matched_caption)
                face.ring.setProgressCompat(100, true)
                stopCamera()
                face.root.postDelayed({ viewModel.onMatchShown() }, MATCH_SHOW_MS)
            }
        }
    }

    private var camera: FaceStepController? = null

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) ensureCamera() else failWith(R.string.face_camera_denied)
    }

    /** Khởi động camera một lần; xin quyền nếu chưa có. */
    private fun ensureCamera(): FaceStepController? {
        camera?.let { return it }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermission.launch(Manifest.permission.CAMERA)
            return null
        }
        // TextureView (COMPATIBLE) mới cắt được theo khung tròn
        binding.face.preview.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        binding.face.preview.clipToOutline = true
        return FaceStepController(
            requireContext(), viewLifecycleOwner, binding.face.preview,
            onBlink = viewModel::onBlink,
            onEmbedding = viewModel::onFaceEmbedding
        ).also {
            camera = it
            it.start()
        }
    }

    private fun stopCamera() {
        camera?.stop()
        camera = null
    }

    private fun renderOtp(state: AuthSheetState.EnteringOtp) {
        binding.otpCells.render(state.digits)
        binding.btnConfirm.isEnabled = state.digits.length == TransactionAuthViewModel.CODE_LENGTH
        binding.tvError.isVisible = state.errorRes != null
        state.errorRes?.let { binding.tvError.text = getString(it, state.attemptsLeft) }
        val waiting = state.remainingSec > 0
        binding.btnResend.isEnabled = !waiting
        binding.btnResend.text = if (waiting) {
            getString(R.string.auth_sheet_resend_in, "%02d:%02d".format(state.remainingSec / 60, state.remainingSec % 60))
        } else {
            getString(R.string.auth_sheet_resend)
        }
    }

    private fun renderSummary(labels: List<String>, values: List<String>) {
        binding.summaryRows.removeAllViews()
        labels.zip(values).forEach { (label, value) ->
            val row = ItemSummaryRowBinding.inflate(layoutInflater, binding.summaryRows, true)
            row.tvLabel.text = label
            row.tvValue.text = value
        }
    }

    private fun finishWith(verified: Boolean) {
        if (resultSent) return
        resultSent = true
        if (verified) binding.root.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
        setFragmentResult(requireArguments().getString(ARG_REQUEST_KEY)!!, bundleOf(KEY_VERIFIED to verified))
        dismissAllowingStateLoss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        // Người dùng vuốt/bấm ra ngoài để đóng = huỷ
        if (!resultSent && isAdded) {
            resultSent = true
            setFragmentResult(requireArguments().getString(ARG_REQUEST_KEY)!!, bundleOf(KEY_VERIFIED to false))
        }
        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        stopCamera()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val KEY_VERIFIED = "verified"
        private const val TAG = "TransactionAuthSheet"
        private const val ARG_TITLE = "title"
        private const val ARG_LABELS = "labels"
        private const val ARG_VALUES = "values"
        private const val ARG_AMOUNT = "amount"
        private const val ARG_REQUEST_KEY = "request_key"
        private const val NO_AMOUNT = Long.MIN_VALUE
        private const val TICK_MS = 1_000L
        private const val PROGRESS_BLINK = 40
        private const val MATCH_SHOW_MS = 900L

        /** Mở sheet; lắng nghe kết quả bằng `fm.setFragmentResultListener(requestKey, ...)`. */
        fun show(fm: FragmentManager, summary: AuthSummary, requestKey: String) {
            TransactionAuthSheet().apply {
                arguments = bundleOf(
                    ARG_TITLE to summary.title,
                    ARG_LABELS to ArrayList(summary.rows.map { it.first }),
                    ARG_VALUES to ArrayList(summary.rows.map { it.second }),
                    ARG_AMOUNT to (summary.amount ?: NO_AMOUNT),
                    ARG_REQUEST_KEY to requestKey
                )
            }.show(fm, TAG)
        }
    }
}
