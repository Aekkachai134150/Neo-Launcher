/*
 *     Copyright (C) 2019 paphonb@xda
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

package com.saggitt.omega.adaptive

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Path
import android.graphics.Region
import android.graphics.drawable.AdaptiveIconDrawable
import android.text.TextUtils
import androidx.annotation.Keep
import androidx.core.graphics.PathParser
import com.android.launcher3.AdaptiveIconDrawableExt
import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.Utilities
import com.android.launcher3.icons.GraphicsUtils
import com.saggitt.omega.util.OmegaSingletonHolder
import com.saggitt.omega.util.omegaPrefs

class IconShapeManager(private val context: Context) {

    private val systemIconShape = getSystemShape()
    var iconShape by context.omegaPrefs.StringBasedPref(
            "pref_iconShape", systemIconShape, AdaptiveIconDrawableExt::onShapeChanged,
            {
                IconShape.fromString(it) ?: systemIconShape
            }, IconShape::toString) { /* no dispose */ }

    init {
        migratePref()
    }

    @SuppressLint("RestrictedApi")
    private fun migratePref() {
        // Migrate from old path-based override
        val override = getLegacyValue()
        if (!TextUtils.isEmpty(override)) {
            try {
                iconShape = findNearestShape(PathParser.createPathFromPathData(override))
                Utilities.getPrefs(context).edit().remove(KEY_LEGACY_PREFERENCE).apply()
            } catch (e: RuntimeException) {
                // Just ignore the error
            }
        }
    }

    fun getLegacyValue(): String {
        val devValue = Utilities.getDevicePrefs(context).getString(KEY_LEGACY_PREFERENCE, "")
        if (!TextUtils.isEmpty(devValue)) {
            // Migrate to general preferences to back up shape overrides
            Utilities.getPrefs(context).edit().putString(KEY_LEGACY_PREFERENCE, devValue).apply()
            Utilities.getDevicePrefs(context).edit().remove(KEY_LEGACY_PREFERENCE).apply()
        }

        return Utilities.getPrefs(context).getString(KEY_LEGACY_PREFERENCE, "")!!
    }

    private fun getSystemShape(): IconShape {
        if (!Utilities.ATLEAST_OREO) return IconShape.Circle

        val iconMask = AdaptiveIconDrawable(null, null).iconMask
        val systemShape = findNearestShape(iconMask)
        return object : IconShape(systemShape) {

            override fun getMaskPath(): Path {
                return Path(iconMask)
            }

            override fun toString() = ""

            override fun getHashString(): String {
                return InvariantDeviceProfile.getIconShapePath(context)
            }
        }
    }

    private fun findNearestShape(comparePath: Path): IconShape {
        val size = 200
        val clip = Region(0, 0, size, size)
        val iconR = Region().apply {
            setPath(comparePath, clip)
        }
        val shapePath = Path()
        val shapeR = Region()
        return listOf(
                IconShape.Circle,
                IconShape.Square,
                IconShape.RoundedSquare,
                IconShape.Squircle,
                IconShape.Sammy,
                IconShape.Teardrop,
                IconShape.Cylinder).minBy {
            shapePath.reset()
            it.addShape(shapePath, 0f, 0f, size / 2f)
            shapeR.setPath(shapePath, clip)
            shapeR.op(iconR, Region.Op.XOR)

            GraphicsUtils.getArea(shapeR)
        }!!
    }

    companion object : OmegaSingletonHolder<IconShapeManager>(::IconShapeManager) {

        private const val KEY_LEGACY_PREFERENCE = "pref_override_icon_shape"

        @JvmStatic
        fun getWindowTransitionRadius(context: Context): Float {
            return getInstance(context).iconShape.windowTransitionRadius
        }

        @Keep
        @JvmStatic
        @Suppress("unused")
                /** Used in AdaptiveIconCompat to get the mask path **/
        fun getAdaptiveIconMaskPath() = dangerousGetInstance()!!.iconShape.getMaskPath()
    }
}
