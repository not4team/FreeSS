package com.notfour.ss

import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.utils.Action
import com.github.shadowsocks.utils.DeviceContext
import com.github.shadowsocks.utils.DirectBoot
import com.google.android.gms.ads.MobileAds

/**
 * Created with author.
 * Description:
 * Date: 2018-07-02
 * Time: 下午1:48
 */
class App : Application() {
    companion object {
        lateinit var app: App
        private const val TAG = "VPNApplication"
    }

    val handler by lazy { Handler(Looper.getMainLooper()) }
    val info: PackageInfo by lazy { getPackageInfo(packageName) }
    val deviceContext: Context by lazy { if (Build.VERSION.SDK_INT < 24) this else DeviceContext(this) }
    val directBootSupported by lazy {
        Build.VERSION.SDK_INT >= 24 && getSystemService(DevicePolicyManager::class.java)
                .storageEncryptionStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER
    }

    fun getPackageInfo(packageName: String) = packageManager.getPackageInfo(packageName,
            if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES
            else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES)!!

    fun startService() {
        val intent = Intent(this, BaseService.serviceClass.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
    }

    fun reloadService() = sendBroadcast(Intent(Action.RELOAD))
    fun stopService() = sendBroadcast(Intent(Action.CLOSE))

    val currentProfile: Profile?
        get() = if (DataStore.directBootAware) DirectBoot.getDeviceProfile() else ProfileManager.getProfile(DataStore.profileId)

    override fun onCreate() {
        super.onCreate()
        app = this
        MobileAds.initialize(this, "ca-app-pub-7332030505319718~5493412561")
    }
}