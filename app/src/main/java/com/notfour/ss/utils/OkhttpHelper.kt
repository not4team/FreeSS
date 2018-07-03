package com.notfour.ss.utils

import android.os.Handler
import android.os.Looper
import com.github.shadowsocks.database.Profile
import okhttp3.*
import org.json.JSONObject
import java.io.IOException


/**
 * Created with author.
 * Description:
 * Date: 2018-07-02
 * Time: 下午4:00
 */
object OkhttpHelper {
    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())

    interface CallBack<T> {
        fun onSuccess(result: T)
        fun onFail()
    }

    fun loadProfiles(callBack: CallBack<List<Profile>>) {
        val request = Request.Builder()
                .url("http://198.181.33.161:8055/getAllprofile")
                .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                handler.post { callBack.onFail() }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val json = response.body()!!.string()
                    val obj = JSONObject(json)
                    val profiles = obj.getJSONArray("profiles")
                    val list = mutableListOf<Profile>()
                    for (i in 0 until profiles.length()) {
                        val profileObject = profiles.getJSONObject(i)
                        val profile = Profile()
                        profile.originUrl = profileObject.getString("OriginUrl")
                        profile.name = profileObject.getString("Name")
                        profile.host = profileObject.getString("Host")
                        profile.remotePort = profileObject.getInt("RemotePort")
                        profile.password = profileObject.getString("Password")
                        profile.vpnType = profileObject.getInt("VpnType")
                        profile.brookType = profileObject.getString("BrookType")
                        list.add(profile)
                    }
                    handler.post { callBack.onSuccess(list) }
                } else {
                    handler.post { callBack.onFail() }
                }
            }

        })
    }
}