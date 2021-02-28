package com.google.android.apps.nexuslauncher;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.android.launcher3.Utilities;
import com.android.launcher3.uioverrides.QuickstepLauncher;
import com.google.android.apps.nexuslauncher.smartspace.SmartspaceView;
import com.google.android.libraries.gsa.launcherclient.LauncherClient;
import com.saggitt.omega.settings.SettingsActivity;
import com.saggitt.omega.smartspace.FeedBridge;

// TODO: could this be replaced with plugins?
public class NexusLauncherActivity extends QuickstepLauncher {

    private final NexusLauncher mLauncher;

    public NexusLauncherActivity() {
        mLauncher = new NexusLauncher(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = Utilities.getPrefs(this);
        if (!FeedBridge.Companion.getInstance(this).isInstalled()) {
            prefs.edit().putBoolean(SettingsActivity.ENABLE_MINUS_ONE_PREF, false).apply();
        }
    }

    @Nullable
    public LauncherClient getGoogleNow() {
        return mLauncher.mClient;
    }

    public void playQsbAnimation() {
        mLauncher.mQsbAnimationController.dZ();
    }

    public void openQsb() {
        mLauncher.mQsbAnimationController.openQsb();
    }

    public void registerSmartspaceView(SmartspaceView smartspace) {
        mLauncher.registerSmartspaceView(smartspace);
    }
}
