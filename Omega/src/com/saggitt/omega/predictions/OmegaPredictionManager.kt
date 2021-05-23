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

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import com.android.launcher3.Utilities
import com.android.launcher3.appprediction.PredictionUiStateManager
import com.android.launcher3.appprediction.PredictionUiStateManager.Client.HOME
import com.android.launcher3.appprediction.PredictionUiStateManager.Client.OVERVIEW
import com.android.launcher3.util.PackageManagerHelper
import com.saggitt.omega.util.OmegaSingletonHolder
import com.saggitt.omega.util.omegaPrefs
import com.saggitt.omega.util.runOnMainThread

class OmegaPredictionManager(private val context: Context) {

    private val noOpAppPredictor = NoOpAppPredictor(context)
    private val omegaAppPredictor by lazy {
        OmegaAppPredictor(context, 12, null)
    }
    private val predictionsEnabled get() = context.omegaPrefs.showPredictions

    fun createPredictor(
        client: PredictionUiStateManager.Client,
        count: Int,
        extras: Bundle?
    ): AppPredictorCompat {
        return when {
            !predictionsEnabled -> noOpAppPredictor
            usePlatformPredictor() -> PlatformAppPredictor(context, client, count, extras)
            else -> omegaAppPredictor
        }
    }

    private fun usePlatformPredictor(): Boolean {
        if (!Utilities.ATLEAST_Q) {
            return false
        }
        val predictorService = getPredictorService() ?: return false
        Log.i("OmegaPredictionManager", "Platform predictor: ${predictorService.packageName}")
        if (predictorService.packageName in platformPredictorBlacklist) return false
        return PackageManagerHelper.isAppEnabled(
            context.packageManager, predictorService.packageName, 0
        ) && usageStatsGranted()
    }

    private fun getPredictorService(): ComponentName? {
        val res = Resources.getSystem()
        val id = res.getIdentifier("config_defaultAppPredictionService", "string", "android")
        if (id == 0) return null
        val string = res.getString(id)
        if (TextUtils.isEmpty(string)) return null
        return ComponentName.unflattenFromString(string)
    }

    private fun usageStatsGranted(): Boolean {
        return Utilities.isRecentsEnabled() || context.checkSelfPermission(
            android.Manifest.permission.PACKAGE_USAGE_STATS
        ) == PackageManager.PERMISSION_GRANTED
    }

    class NoOpAppPredictor(context: Context) : AppPredictorCompat(context, HOME, 0, null) {

        private val homeCallback =
            PredictionUiStateManager.INSTANCE.get(context).appPredictorCallback(HOME)
        private val overviewCallback =
            PredictionUiStateManager.INSTANCE.get(context).appPredictorCallback(OVERVIEW)

        override fun notifyAppTargetEvent(event: AppTargetEventCompat) {}

        override fun requestPredictionUpdate() {
            runOnMainThread {
                homeCallback.onTargetsAvailable(emptyList())
                overviewCallback.onTargetsAvailable(emptyList())
            }
        }

        override fun destroy() {}
    }

    companion object :
        OmegaSingletonHolder<OmegaPredictionManager>(::OmegaPredictionManager) {
        private val platformPredictorBlacklist = listOf("com.oneplus.asti")
    }
}
