/*******************************************************************************
 *                                                                             *
 *  Copyright (C) 2017 by Max Lv <max.c.lv@gmail.com>                          *
 *  Copyright (C) 2017 by Mygod Studio <contact-shadowsocks-android@mygod.be>  *
 *                                                                             *
 *  This program is free software: you can redistribute it and/or modify       *
 *  it under the terms of the GNU General Public License as published by       *
 *  the Free Software Foundation, either version 3 of the License, or          *
 *  (at your option) any later version.                                        *
 *                                                                             *
 *  This program is distributed in the hope that it will be useful,            *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 *  GNU General Public License for more details.                               *
 *                                                                             *
 *  You should have received a copy of the GNU General Public License          *
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                             *
 *******************************************************************************/

package com.github.shadowsocks.preference

import android.os.Binder
import android.support.v7.app.AppCompatDelegate
import com.github.shadowsocks.database.PrivateDatabase
import com.github.shadowsocks.database.PublicDatabase
import com.github.shadowsocks.utils.DirectBoot
import com.github.shadowsocks.utils.Key
import com.github.shadowsocks.utils.parsePort
import com.notfour.ss.App.Companion.app

object DataStore {
    val publicStore = RoomPreferenceDataStore(PublicDatabase.kvPairDao)
    // privateStore will only be used as temp storage for ProfileConfigFragment
    val privateStore = RoomPreferenceDataStore(PrivateDatabase.kvPairDao)

    // hopefully hashCode = mHandle doesn't change, currently this is true from KitKat to Nougat
    private val userIndex by lazy { Binder.getCallingUserHandle().hashCode() }

    private fun getLocalPort(key: String, default: Int): Int {
        val value = publicStore.getInt(key)
        return if (value != null) {
            publicStore.putString(key, value.toString())
            value
        } else parsePort(publicStore.getString(key), default + userIndex)
    }

    var profileId: Long
        get() = publicStore.getLong(Key.id) ?: 0
        set(value) {
            publicStore.putLong(Key.id, value)
            if (DataStore.directBootAware) DirectBoot.update()
        }
    var originUrl: String
        get() = publicStore.getString(Key.originUrl) ?: ""
        set(value) {
            publicStore.putString(Key.originUrl, value)
            if (DataStore.directBootAware) DirectBoot.update()
        }
    val canToggleLocked: Boolean get() = publicStore.getBoolean(Key.directBootAware) == true
    val directBootAware: Boolean get() = app.directBootSupported && canToggleLocked
    private var nightModeString: String
        get() = publicStore.getString(Key.nightMode) ?: Key.nightModeSystem
        set(value) = publicStore.putString(Key.nightMode, value)
    @AppCompatDelegate.NightMode
    val nightMode: Int
        get() = when (nightModeString) {
            Key.nightModeAuto -> AppCompatDelegate.MODE_NIGHT_AUTO
            Key.nightModeOff -> AppCompatDelegate.MODE_NIGHT_NO
            Key.nightModeOn -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    var serviceMode: String
        get() = publicStore.getString(Key.serviceMode) ?: Key.modeVpn
        set(value) = publicStore.putString(Key.serviceMode, value)
    var portProxy: Int
        get() = getLocalPort(Key.portProxy, 1080)
        set(value) = publicStore.putString(Key.portProxy, value.toString())
    var portLocalDns: Int
        get() = getLocalPort(Key.portLocalDns, 5450)
        set(value) = publicStore.putString(Key.portLocalDns, value.toString())
    var portTransproxy: Int
        get() = getLocalPort(Key.portTransproxy, 8200)
        set(value) = publicStore.putString(Key.portTransproxy, value.toString())

    fun initGlobal() {
        // temporary workaround for support lib bug
        if (publicStore.getString(Key.nightMode) == null) nightModeString = nightModeString
        if (publicStore.getString(Key.serviceMode) == null) serviceMode = serviceMode
        if (publicStore.getString(Key.portProxy) == null) portProxy = portProxy
        if (publicStore.getString(Key.portLocalDns) == null) portLocalDns = portLocalDns
        if (publicStore.getString(Key.portTransproxy) == null) portTransproxy = portTransproxy
    }

    var proxyApps: Boolean
        get() = privateStore.getBoolean(Key.proxyApps) ?: false
        set(value) = privateStore.putBoolean(Key.proxyApps, value)
    var bypass: Boolean
        get() = privateStore.getBoolean(Key.bypass) ?: false
        set(value) = privateStore.putBoolean(Key.bypass, value)
    var individual: String
        get() = privateStore.getString(Key.individual) ?: ""
        set(value) = privateStore.putString(Key.individual, value)
    var plugin: String
        get() = privateStore.getString(Key.plugin) ?: ""
        set(value) = privateStore.putString(Key.plugin, value)
    var dirty: Boolean
        get() = privateStore.getBoolean(Key.dirty) ?: false
        set(value) = privateStore.putBoolean(Key.dirty, value)
}
