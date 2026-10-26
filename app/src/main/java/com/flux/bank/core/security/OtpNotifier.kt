package com.flux.bank.core.security

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.flux.bank.R
import com.flux.bank.ui.common.Event

/**
 * Gửi mã OTP bằng "SMS giả lập": một local notification trên chính máy (gói Firebase Spark không gửi SMS thật).
 *
 * - Có quyền thông báo: đăng notification; bấm vào sẽ mở [OtpAutofillActivity] để tự điền mã.
 * - Không có quyền (Android 13+ bị từ chối): phát mã qua [inAppSms] để Activity hiện banner trong app.
 * Mã được tự điền thông qua [autofill].
 */
class OtpNotifier(private val context: Context) {

    private val _inAppSms = MutableLiveData<Event<String>>()
    val inAppSms: LiveData<Event<String>> = _inAppSms

    private val _autofill = MutableLiveData<Event<String>>()
    val autofill: LiveData<Event<String>> = _autofill

    fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.otp_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Gửi [code] tới người dùng. */
    fun deliver(code: String) {
        if (!canPostNotifications()) {
            _inAppSms.postValue(Event(code))
            return
        }
        val tap = Intent(context, OtpAutofillActivity::class.java)
            .putExtra(EXTRA_OTP_CODE, code)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pending = PendingIntent.getActivity(
            context, 0, tap, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield_check)
            .setContentTitle(context.getString(R.string.otp_sms_sender))
            .setContentText(smsText(code))
            .setStyle(NotificationCompat.BigTextStyle().bigText(smsText(code)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        // Quyền đã được kiểm tra ở canPostNotifications()
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /** Gọi khi người dùng bấm notification hoặc banner: đẩy mã vào sheet đang mở. */
    fun requestAutofill(code: String) {
        _autofill.postValue(Event(code))
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    fun smsText(code: String): String =
        context.getString(R.string.otp_sms_body, code.take(3) + " " + code.drop(3))

    private fun canPostNotifications(): Boolean {
        val manager = NotificationManagerCompat.from(context)
        val channel = manager.getNotificationChannelCompat(CHANNEL_ID)
        return canNotify(
            sdk = Build.VERSION.SDK_INT,
            permissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED,
            appNotificationsEnabled = manager.areNotificationsEnabled(),
            channelBlocked = channel?.importance == NotificationManagerCompat.IMPORTANCE_NONE
        )
    }

    companion object {
        /**
         * Có đăng được notification hay không. Người dùng có thể tắt thông báo của app hoặc của kênh OTP
         * mà không thu hồi quyền — khi đó notify() bị bỏ qua âm thầm nên phải dùng banner trong app.
         */
        fun canNotify(sdk: Int, permissionGranted: Boolean, appNotificationsEnabled: Boolean, channelBlocked: Boolean): Boolean =
            (sdk < Build.VERSION_CODES.TIRAMISU || permissionGranted) && appNotificationsEnabled && !channelBlocked

        const val CHANNEL_ID = "otp_sms"
        const val EXTRA_OTP_CODE = "extra_otp_code"
        private const val NOTIFICATION_ID = 6868
    }
}
