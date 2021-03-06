package com.notfour.ss

import android.app.Activity
import android.app.PendingIntent
import android.app.backup.BackupManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatSpinner
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.Toast
import com.github.shadowsocks.ShadowsocksConnection
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.IShadowsocksServiceCallback
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.bg.Executable
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.preference.DataStore
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.InterstitialAd
import com.notfour.ss.App.Companion.app
import com.notfour.ss.adapter.MySpinnerAdapter
import com.notfour.ss.utils.OkhttpHelper

class MainActivity : AppCompatActivity(), ShadowsocksConnection.Interface {
    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CONNECT = 1
        fun pendingIntent(context: Context) = PendingIntent.getActivity(context, 0,
                Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT), 0)
    }

    lateinit var mAdView: AdView
    lateinit var mBtn: Button
    lateinit var mSpinner: AppCompatSpinner
    lateinit var mAdapter: MySpinnerAdapter
    lateinit var mList: List<Profile>
    private lateinit var mInterstitialAd: InterstitialAd
    // service
    var state = BaseService.IDLE

    override val serviceCallback: IShadowsocksServiceCallback.Stub by lazy {
        object : IShadowsocksServiceCallback.Stub() {
            override fun stateChanged(state: Int, profileName: String?, msg: String?) {
                app.handler.post { changeState(state, msg, true) }
            }

            override fun trafficUpdated(profileId: Long, txRate: Long, rxRate: Long, txTotal: Long, rxTotal: Long) {
            }

            override fun trafficPersisted(profileId: Long) {
            }
        }
    }

    override fun onServiceConnected(service: IShadowsocksService) = changeState(service.state)
    override fun onServiceDisconnected() = changeState(BaseService.IDLE)
    override fun binderDied() {
        super.binderDied()
        app.handler.post {
            connection.disconnect()
            Executable.killAll()
            connection.connect()
        }
    }

    fun changeState(state: Int, msg: String? = null, animate: Boolean = false) {
        when (state) {
            BaseService.CONNECTING -> mBtn.setText(R.string.btn_starting)
            BaseService.CONNECTED -> mBtn.setText(R.string.btn_stop)
            BaseService.STOPPING -> mBtn.setText(R.string.stopping)
            else -> {
                if (msg != null) {
                    Toast.makeText(this, "Error to start VPN service: $msg", Toast.LENGTH_SHORT).show()
                }
                mBtn.setText(R.string.btn_start)
            }
        }
        this.state = state
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mSpinner = findViewById(R.id.main_spinner)
        mBtn = findViewById(R.id.main_btn_start)
        mAdView = findViewById(R.id.adView)
        mAdapter = MySpinnerAdapter(this, R.layout.activity_main_spinner_item)
        mSpinner.adapter = mAdapter
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd.adUnitId = "ca-app-pub-7332030505319718/1974019313"
        mInterstitialAd.loadAd(AdRequest.Builder().build())
        initClick()
        changeState(BaseService.IDLE)   // reset everything to init state
        app.handler.post { connection.connect() }
    }

    fun initClick() {
        val profiles = ProfileManager.getAllProfiles()
        if (profiles != null) {
            mList = profiles
            mAdapter.refreshItems(profiles)
            mSpinner.setSelection(findIndexSpinner(DataStore.originUrl, profiles))
        }
        OkhttpHelper.loadProfiles(object : OkhttpHelper.CallBack<List<Profile>> {
            override fun onSuccess(result: List<Profile>) {
                mList = result
                mAdapter.refreshItems(result)
                ProfileManager.clearProfile()
                ProfileManager.insertProfiles(result)
                if (DataStore.originUrl == null) {
                    DataStore.originUrl = result.first().originUrl
                }
                mSpinner.setSelection(findIndexSpinner(DataStore.originUrl, result))
            }

            override fun onFail() {
                Toast.makeText(this@MainActivity, "服务器拉取失败，请稍后再试!", Toast.LENGTH_SHORT).show()
            }

        })
        mBtn.setOnClickListener {
            when {
                state == BaseService.CONNECTED -> app.stopService()
                BaseService.usingVpnMode -> {
                    val intent = VpnService.prepare(this)
                    if (intent != null) startActivityForResult(intent, REQUEST_CONNECT)
                    else onActivityResult(REQUEST_CONNECT, Activity.RESULT_OK, null)
                }
                else -> {
                    app.startService()
                    if (mInterstitialAd.isLoaded) {
                        mInterstitialAd.show()
                    } else {
                        Log.d(TAG, "The interstitial wasn't loaded yet.")
                    }
                }
            }
        }
        mSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!mList[position].originUrl.equals(DataStore.originUrl)) {
                    app.switchProfile(mList[position].originUrl)
                    if (state == BaseService.CONNECTED) app.reloadService()
                }
            }

        }
    }

    fun findIndexSpinner(originUrl: String, profiles: List<Profile>): Int {
        for (i in profiles.indices) {
            if (originUrl == profiles[i].originUrl) {
                return i
            }
        }
        return 0
    }

    fun showCustomDialog() {
        val customDialog = AlertDialog.Builder(this@MainActivity)
        val dialogView = AdView(this)
        dialogView.adSize = AdSize.SMART_BANNER
        dialogView.adUnitId = "ca-app-pub-7332030505319718/5549877213"
        dialogView.loadAd(AdRequest.Builder().build())
        customDialog.setTitle("广告")
        customDialog.setView(dialogView)
        customDialog.setNegativeButton("关闭", null)
        customDialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        connection.disconnect()
        BackupManager(this).dataChanged()
        app.handler.removeCallbacksAndMessages(null)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) =
            when (item.itemId) {
                R.id.action_settings -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            app.startService()
            if (mInterstitialAd.isLoaded) {
                mInterstitialAd.show()
            } else {
                Log.d(TAG, "The interstitial wasn't loaded yet.")
            }
        } else {
            Toast.makeText(this, "Failed to start VpnService: $data", Toast.LENGTH_SHORT).show()
        }
    }
}
