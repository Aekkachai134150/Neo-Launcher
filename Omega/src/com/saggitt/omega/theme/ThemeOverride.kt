/*
 *  Copyright (c) 2020 Omega Launcher
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.saggitt.omega.theme

import android.app.Activity
import android.content.Context
import com.android.launcher3.R
import java.lang.ref.WeakReference

class ThemeOverride (private val themeSet: ThemeSet, val listener: ThemeOverrideListener?) {
    constructor(themeSet: ThemeSet, activity: Activity) : this(themeSet, ActivityListener(activity))
    constructor(themeSet: ThemeSet, context: Context) : this(themeSet, ContextListener(context))

    val isAlive get() = listener?.isAlive == true

    fun applyTheme(context: Context) {
        listener?.applyTheme(getTheme(context))
    }

    fun applyTheme(themeFlags: Int) {
        listener?.applyTheme(getTheme(themeFlags))
    }

    fun getTheme(context: Context): Int {
        return themeSet.getTheme(context)
    }

    fun getTheme(themeFlags: Int) = themeSet.getTheme(themeFlags)

    fun onThemeChanged(themeFlags: Int) {
        listener?.reloadTheme()
    }

    class Launcher : ThemeSet {

        override val lightTheme = R.style.AppTheme
        override val darkTextTheme = R.style.AppTheme_DarkText
        override val darkTheme = R.style.AppTheme_Dark
        override val darkDarkTextTheme = R.style.AppTheme_Dark_DarkText
        override val blackTheme = R.style.AppTheme_Black
        override val blackDarkTextTheme = R.style.AppTheme_Black_DarkText
    }

    class Settings : ThemeSet {
        override val lightTheme = R.style.SettingsTheme_Light
        override val darkTextTheme = R.style.SettingsTheme_Light
        override val darkTheme = R.style.SettingsTheme_Dark
        override val darkDarkTextTheme = R.style.SettingsTheme_Dark
        override val blackTheme = R.style.SettingsTheme_Black
        override val blackDarkTextTheme = R.style.SettingsTheme_Black
    }

    class SettingsTransparent : ThemeSet {

        override val lightTheme = R.style.SettingsTheme_Light_Transparent
        override val darkTextTheme = R.style.SettingsTheme_DarkText_Transparent
        override val darkTheme = R.style.SettingsTheme_Dark_Transparent
        override val darkDarkTextTheme = R.style.SettingsTheme_Dark_Transparent
        override val blackTheme = R.style.SettingsTheme_Black_Transparent
        override val blackDarkTextTheme = R.style.SettingsTheme_Black_Transparent
    }

    class AlertDialog : ThemeSet {

        override val lightTheme = R.style.SettingsTheme_Light_Dialog
        override val darkTextTheme = R.style.SettingsTheme_Light_Dialog
        override val darkTheme = R.style.SettingsTheme_Dark_Dialog
        override val darkDarkTextTheme = R.style.SettingsTheme_Dark_Dialog
        override val blackTheme = R.style.SettingsTheme_Dark_Dialog
        override val blackDarkTextTheme = R.style.SettingsTheme_Dark_Dialog
    }

    class LauncherDialog : ThemeSet {

        override val lightTheme = android.R.style.Theme_Material_Light
        override val darkTextTheme = android.R.style.Theme_Material_Light
        override val darkTheme = android.R.style.Theme_Material
        override val darkDarkTextTheme = android.R.style.Theme_Material
        override val blackTheme = android.R.style.Theme_Material
        override val blackDarkTextTheme = android.R.style.Theme_Material
    }

    interface ThemeSet {

        val lightTheme: Int
        val darkTextTheme: Int
        val darkTheme: Int
        val darkDarkTextTheme: Int
        val blackTheme: Int
        val blackDarkTextTheme: Int

        fun getTheme(context: Context): Int {
            return getTheme(ThemeManager.getInstance(context).getCurrentFlags())
        }

        fun getTheme(themeFlags: Int): Int {
            val isDark = ThemeManager.isDark(themeFlags)
            val isDarkText = ThemeManager.isDarkText(themeFlags)
            val isBlack = isDark && ThemeManager.isBlack(themeFlags)
            return when {
                isBlack && isDarkText -> blackDarkTextTheme
                isBlack -> blackTheme
                isDark && isDarkText -> darkDarkTextTheme
                isDark -> darkTheme
                isDarkText -> darkTextTheme
                else -> lightTheme
            }
        }
    }

    interface ThemeOverrideListener {
        val isAlive: Boolean
        fun applyTheme(themeRes: Int)
        fun reloadTheme()
    }

    class ActivityListener(activity: Activity) : ThemeOverrideListener {
        private val activityRef = WeakReference(activity)
        override val isAlive = activityRef.get() != null
        override fun applyTheme(themeRes: Int) {
            activityRef.get()?.setTheme(themeRes)
        }

        override fun reloadTheme() {
            activityRef.get()?.recreate()
        }
    }

    class ContextListener(context: Context) : ThemeOverrideListener {
        private val contextRef = WeakReference(context)
        override val isAlive = contextRef.get() != null

        override fun applyTheme(themeRes: Int) {
            contextRef.get()?.setTheme(themeRes)
        }
        override fun reloadTheme() {
            // Unsupported
        }
    }

}