/*
 *     Copyright (C) 2019 Lawnchair Team.
 *
 *     This file is part of Lawnchair Launcher.
 *
 *     Lawnchair Launcher is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Lawnchair Launcher is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Lawnchair Launcher.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.saggitt.omega.predictions

import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.text.TextUtils
import com.android.launcher3.AppFilter
import com.android.launcher3.Utilities
import com.android.launcher3.Utilities.makeComponentKey
import com.android.launcher3.appprediction.PredictionUiStateManager
import com.android.launcher3.appprediction.PredictionUiStateManager.Client.HOME
import com.android.launcher3.appprediction.PredictionUiStateManager.Client.OVERVIEW
import com.android.launcher3.model.AppLaunchTracker.CONTAINER_ALL_APPS
import com.android.launcher3.util.ComponentKey
import com.saggitt.omega.predictions.AppTargetEventCompat.ACTION_LAUNCH
import com.saggitt.omega.predictions.CustomAppPredictor.PLACE_HOLDERS
import com.saggitt.omega.util.runOnMainThread
import java.util.*
import java.util.concurrent.TimeUnit

open class OmegaAppPredictor(
        private val context: Context, count: Int,
        extras: Bundle?) : AppPredictorCompat(context, HOME, count, extras) {

    private val homeCallback = PredictionUiStateManager.INSTANCE.get(context).appPredictorCallback(HOME)
    private val overviewCallback = PredictionUiStateManager.INSTANCE.get(context).appPredictorCallback(OVERVIEW)
    private val maxPredictions = count

    private val handler = Handler()
    private val packageManager = context.packageManager
    private val appFilter = AppFilter.newInstance(context)

    private val devicePrefs = Utilities.getDevicePrefs(context)
    private val appsList = CountRankedArrayPreference(devicePrefs, "recent_app_launches", 250)
    private val phonesList = CountRankedArrayPreference(devicePrefs, "plugged_app_launches", 20)

    /**
     * Time at which headphones have been plugged in / connected. 0 if disconnected, -1 before initialized
     */
    private var phonesConnectedAt = -1L
        set(value) {
            field = value
            if (value != -1L) {
                phonesLaunches = 0
                updatePredictions()
                if (value != 0L) {
                    // Ensure temporary predictions get removed again after
                    handler.postDelayed(this::updatePredictions, DURATION_RECENTLY)
                }
            }
        }

    /**
     * Whether headphones have just been plugged in / connected (in the last two minutes)
     * TODO: Is two minutes appropriate or do we want to increase this?
     */
    private val phonesJustConnected get() = phonesConnectedAt > 0 && SystemClock.uptimeMillis() in phonesConnectedAt until phonesConnectedAt + DURATION_RECENTLY

    /**
     * Whether or not the current app launch is relevant for headphone suggestions or not
     */
    private val relevantForPhones get() = phonesLaunches < 2 && phonesJustConnected

    /**
     * Number of launches recorded since headphones were connected
     */
    private var phonesLaunches = 0
    private val phonesStateChangeReceiver by lazy {
        object : BroadcastReceiver() {
            private var firstReceive = true

            override fun onReceive(context: Context, intent: Intent) {
                if (!firstReceive) {
                    phonesConnectedAt = when (intent.action) {
                        Intent.ACTION_HEADSET_PLUG -> {
                            when (intent.getIntExtra("state", -1)) {
                                1 -> SystemClock.currentThreadTimeMillis()
                                else -> 0
                            }
                        }
                        BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                            when (intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)) {
                                2 -> SystemClock.currentThreadTimeMillis()
                                else -> 0
                            }
                        }
                        else -> 0
                    }
                }
                firstReceive = false
            }
        }
    }

    init {
        // This object is currently a singleton, so just register and forget
        context.registerReceiver(
                phonesStateChangeReceiver,
                IntentFilter(Intent.ACTION_HEADSET_PLUG).apply {
                    addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
                }, null, handler)
    }

    override fun notifyAppTargetEvent(event: AppTargetEventCompat) {
        if (event.action == ACTION_LAUNCH) {
            val target = event.target
            if (target?.className != null) {
                val component = ComponentName(target.packageName, target.className!!)
                val key = ComponentKey(component, target.user).toString()
                if (appFilter.shouldShowApp(component, target.user)) {
                    clearRemovedComponents()

                    var changed = false
                    if (event.launchLocation == CONTAINER_ALL_APPS) {
                        appsList.add(key)
                        changed = true
                    }
                    if (relevantForPhones) {
                        phonesList.add(key)
                        phonesLaunches++
                        changed = true
                    }

                    if (changed) {
                        updatePredictions()
                    }
                }
            }
        }
    }

    override fun requestPredictionUpdate() {
        updatePredictions()
    }

    override fun destroy() {
    }

    private fun updatePredictions() {
        clearRemovedComponents()

        val user = Process.myUserHandle()
        val appList = if (phonesJustConnected) phonesList.getRanked().take(MAX_HEADPHONE_SUGGESTIONS).toMutableList() else mutableListOf()
        appList.addAll(appsList.getRanked().filterNot { appList.contains(it) }.take(maxPredictions - appList.size))
        val fullList = appList.map { makeComponentKey(context, it) }
                .filterNot { isHiddenApp(context, it) }.toMutableList()
        if (fullList.size < maxPredictions) {
            fullList.addAll(
                    PLACE_HOLDERS.mapNotNull { packageManager.getLaunchIntentForPackage(it)?.component }
                            .map { ComponentKey(it, user) }
            )
        }
        val targets = fullList.take(maxPredictions).mapIndexed { rank, key ->
            val id = AppTargetIdCompat("app:${key.componentName}")
            AppTargetCompat.Builder(id, key.componentName.packageName, key.user)
                    .setClassName(key.componentName.className)
                    .setRank(rank)
                    .build()
        }
        runOnMainThread {
            homeCallback.onTargetsAvailable(targets)
            overviewCallback.onTargetsAvailable(targets)
        }
    }

    private fun clearRemovedComponents() {
        appsList.removeAll {
            if (TextUtils.isEmpty(it)) {
                return@removeAll true
            }
            val key = makeComponentKey(context, it)
            val component = key.componentName
            try {
                packageManager.getActivityInfo(component, 0)
                false
            } catch (ignored: PackageManager.NameNotFoundException) {
                val intent = packageManager.getLaunchIntentForPackage(component.packageName)
                if (intent != null) {
                    val componentInfo = intent.component
                    if (componentInfo != null) {
                        appsList.replace(it, key.toString())
                        return@removeAll false
                    }
                }
                true
            }
        }
        phonesList.removeAll {
            if (TextUtils.isEmpty(it)) {
                return@removeAll true
            }
            val key = makeComponentKey(context, it)
            val component = key.componentName
            try {
                packageManager.getActivityInfo(component, 0)
                false
            } catch (ignored: PackageManager.NameNotFoundException) {
                val intent = packageManager.getLaunchIntentForPackage(component.packageName)
                if (intent != null) {
                    val componentInfo = intent.component
                    if (componentInfo != null) {
                        phonesList.replace(it, key.toString())
                        return@removeAll false
                    }
                }
                true
            }
        }
    }

    /**
     * A ranked list with roll over to get/store currently relevant events and rank them by occurence
     */
    inner class CountRankedArrayPreference(private val prefs: SharedPreferences, private val key: String, private val maxSize: Int = -1, private val delimiter: String = ";") {
        private var list = load()

        fun getRanked(): Set<String> = list.distinct().sortedBy { value -> list.count { it == value } }.reversed().toSet()

        fun add(string: String) {
            list.add(0, string)
            if (maxSize >= 0 && list.size > maxSize) {
                list = list.drop(maxSize).toMutableList()
            }
            save()
        }

        fun clear() {
            list.clear()
            prefs.edit().remove(key).apply()
        }

        fun removeAll(filter: (String) -> Boolean) = list.removeAll(filter)
        fun replace(filter: String, replacement: String) {
            list = list.map { if (it == filter) replacement else it }.toMutableList()
        }

        fun contains(element: String) = list.contains(element)

        private fun load() = (prefs.getString(key, "") ?: "").split(delimiter).toMutableList()
        private fun save() {
            val strValue = list.joinToString(delimiter)
            prefs.edit().putString(key, strValue).apply()
        }
    }

    companion object {

        // TODO: Increase to two?
        const val MAX_HEADPHONE_SUGGESTIONS = 1

        // Our definition of "Recently"
        @JvmStatic
        val DURATION_RECENTLY = TimeUnit.MINUTES.toMillis(2)

        @JvmStatic
        fun isHiddenApp(context: Context, key: ComponentKey): Boolean {
            return getHiddenApps(context).contains(key.toString())
        }

        private // This can't be null anyway
        fun getHiddenApps(context: Context): MutableSet<String> {
            return HashSet(Utilities.getOmegaPrefs(context).hiddenPredictionAppSet)
        }

        private fun setHiddenApps(context: Context, hiddenApps: Set<String>) {
            Utilities.getOmegaPrefs(context).hiddenPredictionAppSet = hiddenApps
        }

        @JvmStatic
        fun setComponentNameState(context: Context, key: ComponentKey, hidden: Boolean) {
            val comp = key.toString()
            val hiddenApps = getHiddenApps(context)
            while (hiddenApps.contains(comp)) {
                hiddenApps.remove(comp)
            }
            if (hidden) {
                hiddenApps.add(comp)
            }
            setHiddenApps(context, hiddenApps)
        }
    }
}
