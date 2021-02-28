package com.google.android.apps.nexuslauncher.qsb;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.views.ActivityContext;

public class HotseatQsbWidget extends FrameLayout {

    public static final String KEY_DOCK_COLORED_GOOGLE = "pref_dockColoredGoogle";

    public HotseatQsbWidget(Context context) {
        this(context, null);
    }

    public HotseatQsbWidget(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public HotseatQsbWidget(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public static boolean isGoogleColored(Context context) {
        if (Utilities.getOmegaPrefs(context).getDockColoredGoogle()) {
            return true;
        }
        WallpaperInfo wallpaperInfo = WallpaperManager.getInstance(context).getWallpaperInfo();
        return wallpaperInfo != null && wallpaperInfo.getComponent().flattenToString()
                .equals(context.getString(R.string.default_live_wallpaper));
    }

}
