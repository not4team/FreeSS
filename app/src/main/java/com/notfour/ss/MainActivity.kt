package com.notfour.ss

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import com.github.shadowsocks.ShadowsocksConnection
import com.github.shadowsocks.aidl.IShadowsocksServiceCallback
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.database.Profile
import com.google.android.gms.ads.AdView
import com.notfour.ss.App.Companion.app
import com.notfour.ss.adapter.MySpinnerAdapter
import com.notfour.ss.utils.CyptoUtils
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
    lateinit var mSpinner: Spinner
    lateinit var mAdapter: MySpinnerAdapter

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
//        val adRequest = AdRequest.Builder().build()
//        mAdView.loadAd(adRequest)
        initClick()
    }

    fun initClick() {
        OkhttpHelper.loadProfiles(object : OkhttpHelper.CallBack<List<Profile>> {
            override fun onSuccess(result: List<Profile>) {
                mAdapter.refreshItems(result)
                var first = result.first()
                first.password = CyptoUtils.decode(first.password)
                app.currentProfile = first
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
                else -> app.startService()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) =
            when (item.itemId) {
                R.id.action_settings -> true
                else -> super.onOptionsItemSelected(item)
            }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) app.startService() else {
            Toast.makeText(this, "Failed to start VpnService: $data", Toast.LENGTH_SHORT).show()
        }
    }
}
