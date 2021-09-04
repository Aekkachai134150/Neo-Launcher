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

package com.saggitt.omega.iconpack


import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import com.android.launcher3.*
import com.android.launcher3.icons.ShortcutCachingLogic
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.pm.UserCache
import com.android.launcher3.shortcuts.DeepShortcutManager
import com.android.launcher3.util.ComponentKey
import com.google.android.apps.nexuslauncher.DynamicIconProvider
import com.google.android.apps.nexuslauncher.clock.DynamicClock
import com.saggitt.omega.adaptive.AdaptiveIconGenerator
import com.saggitt.omega.icons.CustomIconProvider
import com.saggitt.omega.util.ApkAssets
import com.saggitt.omega.util.getLauncherActivityInfo
import com.saggitt.omega.util.omegaPrefs
import com.saggitt.omega.util.overrideSdk
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

class DefaultPack(context: Context) : IconPack(context, "") {

    private val prefs = context.omegaPrefs
    val dynamicClockDrawer by lazy { DynamicClock(context) }
    private val appMap = HashMap<ComponentKey, Entry>().apply {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        UserCache.INSTANCE.get(context).userProfiles.forEach { user ->
            launcherApps.getActivityList(null, user).forEach {
                put(ComponentKey(it.componentName, user), Entry(it))
            }
        }
    }
    override val entries get() = appMap.values.toList()

    init {
        executeLoadPack()
    }

    override val packInfo = IconPackList.DefaultPackInfo(context)

    override fun onDateChanged() {
        val model = LauncherAppState.getInstance(context).model
        UserCache.INSTANCE.get(context).userProfiles.forEach { user ->
            model.onAppIconChanged(GOOGLE_CALENDAR, user)
            val shortcuts = DeepShortcutManager.getInstance(context)
                .queryForPinnedShortcuts(DynamicIconProvider.GOOGLE_CALENDAR, user)
            if (!shortcuts.isEmpty()) {
                model.updatePinnedShortcuts(DynamicIconProvider.GOOGLE_CALENDAR, shortcuts, user)
            }
        }
    }

    override fun loadPack() {}
    override fun getEntryForComponent(key: ComponentKey) = appMap[key]

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getIcon(entry: IconPackManager.CustomIconEntry, iconDpi: Int): Drawable? {
        return getIcon(Utilities.makeComponentKey(context, entry.icon), iconDpi)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getIcon(key: ComponentKey, iconDpi: Int): Drawable? {
        ensureInitialLoadComplete()

        val info = key.getLauncherActivityInfo(context) ?: return null
        val component = key.componentName
        var originalIcon = info.getIcon(iconDpi).apply { mutate() }
        getLegacyIcon(component, iconDpi, prefs.forceShapeless)?.let {
            originalIcon = it.apply { mutate() }
        }
        var roundIcon: Drawable? = null
        if (!prefs.forceShapeless) {
            getRoundIcon(component, iconDpi)?.let {
                roundIcon = it.apply { mutate() }
            }
        }
        val gen = AdaptiveIconGenerator(context, originalIcon, roundIcon)
        return gen.getResult()
    }

    override fun getIcon(
        launcherActivityInfo: LauncherActivityInfo,
        iconDpi: Int, flattenDrawable: Boolean,
        customIconEntry: IconPackManager.CustomIconEntry?,
        iconProvider: CustomIconProvider?
    ): Drawable {
        ensureInitialLoadComplete()

        val key: ComponentKey
        val info: LauncherActivityInfo
        if (customIconEntry != null && !TextUtils.isEmpty(customIconEntry.icon)) {
            key = Utilities.makeComponentKey(context, customIconEntry.icon)
            info = key.getLauncherActivityInfo(context) ?: launcherActivityInfo
        } else {
            key = ComponentKey(launcherActivityInfo.componentName, launcherActivityInfo.user)
            info = launcherActivityInfo
        }
        val component = key.componentName
        val packageName = component.packageName
        var originalIcon = info.getIcon(iconDpi).apply { mutate() }
        getLegacyIcon(component, iconDpi, prefs.forceShapeless)?.let {
            originalIcon = it.apply { mutate() }
        }
        if (iconProvider == null
            || (DynamicIconProvider.GOOGLE_CALENDAR != packageName
                    && DynamicClock.DESK_CLOCK != component)
        ) {
            var roundIcon: Drawable? = null
            if (!prefs.forceShapeless) {
                getRoundIcon(component, iconDpi)?.let {
                    roundIcon = it.apply { mutate() }
                }
            }
            val gen = AdaptiveIconGenerator(context, originalIcon, roundIcon)
            return gen.getResult()
        }
        return iconProvider.getDynamicIcon(info, iconDpi, flattenDrawable)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getIcon(shortcutInfo: ShortcutInfo, iconDpi: Int): Drawable {
        ensureInitialLoadComplete()

        val drawable = ShortcutCachingLogic.getIcon(context, shortcutInfo, iconDpi)
        val gen = AdaptiveIconGenerator(context, drawable, null)
        return gen.getResult()
    }

    override fun newIcon(
        icon: Bitmap, itemInfo: ItemInfo,
        customIconEntry: IconPackManager.CustomIconEntry?,
        drawableFactory: CustomDrawableFactory
    ): FastBitmapDrawable {
        ensureInitialLoadComplete()

        if (itemInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
            val component = if (customIconEntry?.icon != null) {
                try {
                    Utilities.makeComponentKey(
                        context,
                        customIconEntry.packPackageName
                    ).componentName
                } catch (e: NullPointerException) {
                    Log.e("DefaultPack", e.message, e)
                    itemInfo.targetComponent
                }
            } else {
                itemInfo.targetComponent
            }
            if (DynamicClock.DESK_CLOCK == component) {
                return dynamicClockDrawer.drawIcon(icon)
            }
        }

        return FastBitmapDrawable(icon)
    }

    override fun supportsMasking(): Boolean = false

    private fun getRoundIcon(component: ComponentName, iconDpi: Int): Drawable? {
        var appIcon: String? = null
        val elementTags = HashMap<String, String>()

        try {
            val resourcesForApplication =
                context.packageManager.getResourcesForApplication(component.packageName)
            val info = context.packageManager.getApplicationInfo(
                component.packageName,
                PackageManager.GET_SHARED_LIBRARY_FILES or PackageManager.GET_META_DATA
            )

            val parseXml = try {
                // For apps which are installed as Split APKs the asset instance we can get via PM won't hold the right Manifest for us.
                ApkAssets(info.publicSourceDir).openXml(MANIFEST_XML)
            } catch (ex: Exception) {
                ex.message
                val assets = resourcesForApplication.assets
                assets.openXmlResourceParser(MANIFEST_XML)
            }

            while (parseXml.next() != XmlPullParser.END_DOCUMENT) {
                if (parseXml.eventType == XmlPullParser.START_TAG) {
                    val name = parseXml.name
                    for (i in 0 until parseXml.attributeCount) {
                        elementTags[parseXml.getAttributeName(i)] = parseXml.getAttributeValue(i)
                    }
                    if (elementTags.containsKey("roundIcon")) {
                        if (name == "application") {
                            appIcon = elementTags["roundIcon"]
                        } else if ((name == "activity" || name == "activity-alias") &&
                            elementTags.containsKey("name") &&
                            elementTags["name"] == component.className
                        ) {
                            appIcon = elementTags["roundIcon"]
                            break
                        }
                    }
                    elementTags.clear()
                }
            }
            parseXml.close()

            if (appIcon != null) {
                val resId = Utilities.parseResourceIdentifier(
                    resourcesForApplication,
                    appIcon,
                    component.packageName
                )
                return resourcesForApplication.getDrawableForDensity(resId, iconDpi, null)
            }
        } catch (ex: PackageManager.NameNotFoundException) {
            ex.printStackTrace()
        } catch (ex: Resources.NotFoundException) {
            ex.printStackTrace()
        } catch (ex: IOException) {
            ex.printStackTrace()
        } catch (ex: XmlPullParserException) {
            ex.printStackTrace()
        }

        return null
    }

    private fun getLegacyIcon(
        component: ComponentName,
        iconDpi: Int,
        loadShapeless: Boolean
    ): Drawable? {
        var appIcon: String? = null
        val elementTags = HashMap<String, String>()

        try {
            val resourcesForApplication =
                context.packageManager.getResourcesForApplication(component.packageName)
            val info = context.packageManager.getApplicationInfo(
                component.packageName,
                PackageManager.GET_SHARED_LIBRARY_FILES or PackageManager.GET_META_DATA
            )

            val parseXml = try {
                // For apps which are installed as Split APKs the asset instance we can get via PM won't hold the right Manifest for us.
                ApkAssets(info.publicSourceDir).openXml(MANIFEST_XML)
            } catch (ex: Exception) {
                ex.message
                val assets = resourcesForApplication.assets
                assets.openXmlResourceParser(MANIFEST_XML)
            }

            while (parseXml.next() != XmlPullParser.END_DOCUMENT) {
                if (parseXml.eventType == XmlPullParser.START_TAG) {
                    val name = parseXml.name
                    for (i in 0 until parseXml.attributeCount) {
                        elementTags[parseXml.getAttributeName(i)] = parseXml.getAttributeValue(i)
                    }
                    if (elementTags.containsKey("icon")) {
                        if (name == "application") {
                            appIcon = elementTags["icon"]
                        } else if ((name == "activity" || name == "activity-alias") &&
                            elementTags.containsKey("name") &&
                            elementTags["name"] == component.className
                        ) {
                            appIcon = elementTags["icon"]
                            break
                        }
                    }
                    elementTags.clear()
                }
            }
            parseXml.close()

            if (appIcon != null) {
                val resId = Utilities.parseResourceIdentifier(
                    resourcesForApplication,
                    appIcon,
                    component.packageName
                )
                if (loadShapeless) {
                    return resourcesForApplication.overrideSdk(Build.VERSION_CODES.M) {
                        ResourcesCompat.getDrawable(
                            this,
                            resId,
                            null
                        )
                    }
                }
                return resourcesForApplication.getDrawableForDensity(resId, iconDpi, null)
            }
        } catch (ex: PackageManager.NameNotFoundException) {
            ex.printStackTrace()
        } catch (ex: Resources.NotFoundException) {
            ex.printStackTrace()
        } catch (ex: IOException) {
            ex.printStackTrace()
        } catch (ex: XmlPullParserException) {
            ex.printStackTrace()
        }

        return null
    }

    class Entry(private val app: LauncherActivityInfo) : IconPack.Entry() {

        override val displayName by lazy { app.label.toString() }
        override val identifierName = ComponentKey(app.componentName, app.user).toString()
        override val isAvailable = true

        @RequiresApi(Build.VERSION_CODES.O)
        override fun drawableForDensity(density: Int): Drawable {
            return AdaptiveIconCompat.wrap(app.getIcon(density)!!)
        }

        override fun toCustomEntry() = IconPackManager.CustomIconEntry(
            "",
            ComponentKey(app.componentName, app.user).toString()
        )
    }

    companion object {
        const val MANIFEST_XML = "AndroidManifest.xml"
        const val GOOGLE_CALENDAR = "com.google.android.calendar"
    }
}
