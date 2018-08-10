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

package com.github.shadowsocks.database

import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.util.Log
import com.notfour.ss.App.Companion.app
import java.io.IOException
import java.sql.SQLException

/**
 * SQLExceptions are not caught (and therefore will cause crash) for insert/update transactions
 * to ensure we are in a consistent state.
 */
object ProfileManager {
    private const val TAG = "ProfileManager"

    @Throws(SQLException::class)
    fun createProfile(p: Profile? = null): Profile {
        val profile = p ?: Profile()
        profile.id = 0
        val oldProfile = app.currentProfile
        if (oldProfile != null) {
            // Copy Feature Settings from old profile
            profile.route = oldProfile.route
            profile.ipv6 = oldProfile.ipv6
            profile.proxyApps = oldProfile.proxyApps
            profile.bypass = oldProfile.bypass
            profile.individual = oldProfile.individual
            profile.udpdns = oldProfile.udpdns
        }
        profile.userOrder = PrivateDatabase.profileDao.nextOrder() ?: 0
        profile.id = PrivateDatabase.profileDao.create(profile)
        return profile
    }

    fun clearProfile() = PrivateDatabase.profileDao.clear()

    fun insertProfiles(profiles: List<Profile>) = PrivateDatabase.profileDao.insertProfiles(profiles)

    /**
     * Note: It's caller's responsibility to update DirectBoot profile if necessary.
     */
    @Throws(SQLException::class)
    fun updateProfile(profile: Profile) = check(PrivateDatabase.profileDao.update(profile) == 1)

    @Throws(IOException::class)
    fun getProfile(originUrl: String): Profile? = try {
        PrivateDatabase.profileDao[originUrl]
    } catch (ex: SQLException) {
        if (ex.cause is SQLiteCantOpenDatabaseException) throw IOException(ex)
        Log.e(TAG, "getProfile", ex)
        null
    }


    @Throws(IOException::class)
    fun isNotEmpty(): Boolean = try {
        PrivateDatabase.profileDao.isNotEmpty()
    } catch (ex: SQLException) {
        if (ex.cause is SQLiteCantOpenDatabaseException) throw IOException(ex)
        Log.e(TAG, "isNotEmpty", ex)
        false
    }

    @Throws(IOException::class)
    fun getAllProfiles(): List<Profile>? = try {
        PrivateDatabase.profileDao.list()
    } catch (ex: SQLException) {
        if (ex.cause is SQLiteCantOpenDatabaseException) throw IOException(ex)
        Log.e(TAG, "getAllProfiles", ex)
        null
    }
}
