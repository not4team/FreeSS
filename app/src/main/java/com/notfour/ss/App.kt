package com.notfour.ss

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.UserManager
import android.support.annotation.RequiresApi
import android.util.Log
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.utils.*
import com.google.android.gms.ads.MobileAds
import java.io.File
import java.io.IOException

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

    var currentProfile: Profile? = null
        get() = if (DataStore.directBootAware) DirectBoot.getDeviceProfile() else ProfileManager.getProfile(DataStore.originUrl)

    fun switchProfile(originUrl: String): Profile {
        val result = ProfileManager.getProfile(originUrl) ?: ProfileManager.createProfile()
        DataStore.originUrl = result.originUrl
        return result
    }

    override fun onCreate() {
        super.onCreate()
        app = this
        MobileAds.initialize(this, "ca-app-pub-7332030505319718~5493412561")
        // handle data restored/crash
        if (Build.VERSION.SDK_INT >= 24 && DataStore.directBootAware &&
                (getSystemService(Context.USER_SERVICE) as UserManager).isUserUnlocked) DirectBoot.flushTrafficStats()
        TcpFastOpen.enabledAsync(DataStore.publicStore.getBoolean(Key.tfo, TcpFastOpen.sendEnabled))
        if (DataStore.publicStore.getLong(Key.assetUpdateTime, -1) != info.lastUpdateTime) {
            val assetManager = assets
            for (dir in arrayOf("acl", "overture"))
                try {
                    for (file in assetManager.list(dir)) assetManager.open("$dir/$file").use { input ->
                        File(deviceContext.filesDir, file).outputStream().use { output -> input.copyTo(output) }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, e.message)
                }
            DataStore.publicStore.putLong(Key.assetUpdateTime, info.lastUpdateTime)
        }
        updateNotificationChannels()
    }

    private fun updateNotificationChannels() {
        if (Build.VERSION.SDK_INT >= 26) @RequiresApi(26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannels(listOf(
                    NotificationChannel("service-vpn", getText(R.string.service_vpn),
                            NotificationManager.IMPORTANCE_LOW),
                    NotificationChannel("service-proxy", getText(R.string.service_proxy),
                            NotificationManager.IMPORTANCE_LOW),
                    NotificationChannel("service-transproxy", getText(R.string.service_transproxy),
                            NotificationManager.IMPORTANCE_LOW)))
            nm.deleteNotificationChannel("service-nat") // NAT mode is gone for good
        }
    }
}