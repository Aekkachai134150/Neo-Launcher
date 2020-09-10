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

package com.saggitt.omega.qsb;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.views.ActivityContext;

public class HotseatQsbWidget extends FrameLayout {
    private boolean mIsGoogleColored;

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

    public static int getBottomMargin(ActivityContext launcher) {
        Context context = (Context) launcher;
        Resources resources = context.getResources();
        int minBottom = launcher.getDeviceProfile().getInsets().bottom + resources
                .getDimensionPixelSize(R.dimen.hotseat_qsb_bottom_margin);

        DeviceProfile profile = launcher.getDeviceProfile();
        Rect rect = profile.getInsets();
        Rect hotseatLayoutPadding = profile.getHotseatLayoutPadding();

        int hotseatTop = profile.hotseatBarSizePx + rect.bottom;
        int hotseatIconsTop = hotseatTop - hotseatLayoutPadding.top;

        float f = ((hotseatIconsTop - hotseatLayoutPadding.bottom) + (profile.iconSizePx * 0.92f)) / 2.0f;
        float f2 = ((float) rect.bottom) * 0.67f;
        int bottomMargin = Math.round(f2 + (
                ((((((float) hotseatTop) - f2) - f) - resources
                        .getDimension(R.dimen.qsb_widget_height))
                        - ((float) profile.verticalDragHandleSizePx)) / 2.0f));

        return Math.max(minBottom, bottomMargin);
    }
}

