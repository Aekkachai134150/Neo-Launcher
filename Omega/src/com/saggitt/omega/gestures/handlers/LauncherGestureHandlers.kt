/*
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

package com.saggitt.omega.gestures.handlers

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.annotation.Keep
import com.android.launcher3.R
import com.saggitt.omega.gestures.GestureController
import com.saggitt.omega.gestures.GestureHandler
import org.json.JSONObject

@Keep
class OpenSettingsGestureHandler(context: Context, config: JSONObject?) :
        GestureHandler(context, config) {

    override val displayName = context.getString(R.string.action_open_settings)!!
    override val iconResource: Intent.ShortcutIconResource by lazy {
        Intent.ShortcutIconResource.fromContext(context, R.drawable.ic_setting)
    }

    override fun onGestureTrigger(controller: GestureController, view: View?) {
        controller.launcher.startActivity(Intent(Intent.ACTION_APPLICATION_PREFERENCES).apply {
            `package` = controller.launcher.packageName
        })
    }
}

@Keep
class OpenDashGestureHandler(context: Context, config: JSONObject?) :
        GestureHandler(context, config) {

    override val displayName = context.getString(R.string.action_open_dash)!!
    override fun onGestureTrigger(controller: GestureController, view: View?) {
        //TODO: INFLAR DASH VIEW
    }
}
