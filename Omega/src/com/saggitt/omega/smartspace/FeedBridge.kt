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

package com.saggitt.omega.smartspace

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo.FLAG_SYSTEM
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.util.Log
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.saggitt.omega.util.*
import okhttp3.internal.toHexString

class FeedBridge(private val context: Context) {

    private val shouldUseFeed = context.applicationInfo.flags and FLAG_SYSTEM == 0
    private val prefs = context.omegaPrefs
    private val bridgePackages by lazy {
        listOf(
                PixelBridgeInfo("com.google.android.apps.nexuslauncher", R.integer.bridge_signature_hash),
                BridgeInfo("app.lawnchair.lawnfeed", R.integer.lawnfeed_signature_hash)
        )
    }

    fun resolveBridge(): BridgeInfo? {
        val customBridge = customBridgeOrNull()
        return when {
            customBridge != null -> customBridge
            !shouldUseFeed -> null
            else -> bridgePackages.firstOrNull { it.isAvailable() }
        }
    }

    private fun customBridgeOrNull() = if (prefs.feedProvider.isNotBlank()) {
        val bridge = CustomBridgeInfo(prefs.feedProvider)
        if (bridge.isAvailable()) {
            bridge
        } else null
    } else null

    private fun customBridgeAvailable() = customBridgeOrNull()?.isAvailable() == true

    fun isInstalled(): Boolean {
        return customBridgeAvailable() || !shouldUseFeed || bridgePackages.any { it.isAvailable() }
    }

    fun resolveSmartspace(): String {
        return bridgePackages.firstOrNull { it.supportsSmartspace }?.packageName
                ?: Config.GOOGLE_QSB
    }

    open inner class BridgeInfo(val packageName: String, signatureHashRes: Int) {

        protected open val signatureHash = if (signatureHashRes > 0) context.resources.getInteger(signatureHashRes) else 0

        open val supportsSmartspace = true

        fun isAvailable(): Boolean {
            val info = context.packageManager.resolveService(
                    Intent(overlayAction)
                            .setPackage(packageName)
                            .setData(
                                    Uri.parse(
                                            StringBuilder(packageName.length + 18)
                                                    .append("app://")
                                                    .append(packageName)
                                                    .append(":")
                                                    .append(Process.myUid())
                                                    .toString()
                                    )
                                            .buildUpon()
                                            .appendQueryParameter("v", 7.toString())
                                            .appendQueryParameter("cv", 9.toString())
                                            .build()
                            ), 0
            )
            return info != null && isSigned()
        }

        open fun isSigned(): Boolean {
            //if (BuildConfig.FLAVOR_build.equals("dev"))
            //    return true // Skip signature checks for dev builds
            if (Utilities.ATLEAST_P) {
                val info = context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = info.signingInfo
                if (signingInfo.hasMultipleSigners()) return false
                return signingInfo.signingCertificateHistory.any { it.hashCode() == signatureHash }
            } else {
                val info = context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                info.signatures.forEach {
                    if (it.hashCode() != signatureHash) return false
                }
                return info.signatures.isNotEmpty()
            }
        }
    }

    private inner class CustomBridgeInfo(packageName: String) : BridgeInfo(packageName, 0) {
        override val signatureHash = whitelist[packageName]?.toInt() ?: -1
        private val disableWhitelist = prefs.ignoreFeedWhitelist
        override fun isSigned(): Boolean {
            if (signatureHash == -1 && Utilities.ATLEAST_P) {
                val info = context.packageManager
                        .getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = info.signingInfo
                if (signingInfo.hasMultipleSigners()) return false
                signingInfo.signingCertificateHistory.forEach {
                    val hash = it.hashCode().toHexString()
                    Log.d("FeedBridge", "Feed provider $packageName(0x$hash) isn't whitelisted")
                }
            }
            return disableWhitelist || signatureHash != -1 && super.isSigned()
        }
    }

    private inner class PixelBridgeInfo(packageName: String, signatureHashRes: Int) : BridgeInfo(packageName, signatureHashRes) {

        override val supportsSmartspace get() = isAvailable()
    }

    companion object : SingletonHolder<FeedBridge, Context>(ensureOnMainThread(
            useApplicationContext(::FeedBridge))) {

        private const val TAG = "FeedBridge"
        private const val overlayAction = "com.android.launcher3.WINDOW_OVERLAY"

        private val whitelist = mapOf<String, Long>(
                "ua.itaysonlab.homefeeder" to 0x887456ed, // HomeFeeder, t.me/homefeeder
                "launcher.libre.dev" to 0x2e9dbab5 // Librechair, t.me/librechair
        )

        fun getAvailableProviders(context: Context) = context.packageManager
                .queryIntentServices(Intent(overlayAction).setData(Uri.parse("app://" + context.packageName)), PackageManager.GET_META_DATA)
                .map { it.serviceInfo.applicationInfo }
                .distinct()
                .filter { getInstance(context).CustomBridgeInfo(it.packageName).isSigned() }

        @JvmStatic
        fun useBridge(context: Context) = getInstance(context).let { it.shouldUseFeed || it.customBridgeAvailable() }
    }
}
