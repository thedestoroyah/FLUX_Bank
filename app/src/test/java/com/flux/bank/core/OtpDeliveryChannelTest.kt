package com.flux.bank.core

import com.flux.bank.core.security.OtpNotifier
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpDeliveryChannelTest {

    @Test
    fun notificationsDisabledOnOldAndroidFallsBackToBanner() {
        // API 30: không cần quyền runtime nhưng người dùng đã tắt thông báo của app
        assertFalse(OtpNotifier.canNotify(sdk = 30, permissionGranted = false, appNotificationsEnabled = false, channelBlocked = false))
    }

    @Test
    fun blockedChannelFallsBackToBanner() {
        assertFalse(OtpNotifier.canNotify(sdk = 34, permissionGranted = true, appNotificationsEnabled = true, channelBlocked = true))
    }

    @Test
    fun deniedPermissionOnAndroid13FallsBackToBanner() {
        assertFalse(OtpNotifier.canNotify(sdk = 33, permissionGranted = false, appNotificationsEnabled = true, channelBlocked = false))
    }

    @Test
    fun everythingAllowedUsesNotification() {
        assertTrue(OtpNotifier.canNotify(sdk = 34, permissionGranted = true, appNotificationsEnabled = true, channelBlocked = false))
        assertTrue(OtpNotifier.canNotify(sdk = 30, permissionGranted = false, appNotificationsEnabled = true, channelBlocked = false))
    }
}
