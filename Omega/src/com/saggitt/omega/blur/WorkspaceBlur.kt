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

package com.saggitt.omega.blur

import com.android.launcher3.CellLayout
import com.android.launcher3.R
import com.android.launcher3.Workspace
import com.android.launcher3.logging.StatsLogManager
import com.android.launcher3.util.IntSparseArrayMap
import com.android.launcher3.views.OptionsPopupView
import com.saggitt.omega.util.omegaPrefs

class WorkspaceBlur(private val workspace: Workspace, private val screens: IntSparseArrayMap<CellLayout>) {

    private val prefs = workspace.context.omegaPrefs
    private val blurredScreens = prefs.workspaceBlurScreens.toMutableSet()

    operator fun get(id: Int) = blurredScreens.contains(id)

    operator fun set(id: Int, blurred: Boolean) {
        if (blurred) {
            blurredScreens.add(id)
        } else {
            blurredScreens.remove(id)
        }
        blurredScreens
                .filter { !screens.containsKey(it) }
                .forEach { blurredScreens.remove(it) }
        prefs.workspaceBlurScreens = blurredScreens
        workspace.updateBlurAlpha()
    }

    fun getOptionItem(id: Int): OptionsPopupView.OptionItem {
        return if (this[id]) {
            OptionsPopupView.OptionItem(
                    R.string.title__theme_blur,
                    R.drawable.ic_check_white, StatsLogManager.LauncherRankingEvent.UNKNOWN) {
                set(id, false)
                true
            }
        } else {
            OptionsPopupView.OptionItem(
                    R.string.title__theme_blur,
                    R.drawable.ic_remove_no_shadow, StatsLogManager.LauncherRankingEvent.UNKNOWN) {
                set(id, true)
                true
            }
        }
    }
}