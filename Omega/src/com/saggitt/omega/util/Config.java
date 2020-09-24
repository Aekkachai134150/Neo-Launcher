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

package com.saggitt.omega.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.TypedValue;

import com.android.launcher3.R;

public class Config {
    //APP DRAWER SORT MODE
    public static final int SORT_AZ = 0;
    public static final int SORT_ZA = 1;
    public static final int SORT_LAST_INSTALLED = 2;
    public static final int SORT_MOST_USED = 3;
    public static final int SORT_BY_COLOR = 4;

    //PERMISION FLAGS
    public static final int REQUEST_PERMISSION_STORAGE_ACCESS = 666;
    public static final int REQUEST_PERMISSION_LOCATION_ACCESS = 667;
<<<<<<< HEAD
=======
    public static final int CODE_EDIT_ICON = 100;
>>>>>>> ba3d8f4607d1f35bce071eabb638c4e819bb5fbc

    public final static String GOOGLE_QSB = "com.google.android.googlequicksearchbox";

    public Context mContext;

    public Config(Context context) {
        mContext = context;
    }

    public boolean defaultEnableBlur() {
        return mContext.getResources().getBoolean(R.bool.config_default_enable_blur);
    }

    public String getDefaultSearchProvider() {
        return mContext.getResources().getString(R.string.config_default_search_provider);
    }

    public String[] getDefaultIconPacks() {

        return mContext.getResources().getStringArray(R.array.config_default_icon_packs);
    }

    public float getDefaultBlurStrength() {
        TypedValue typedValue = new TypedValue();
        mContext.getResources().getValue(R.dimen.config_default_blur_strength, typedValue, true);
        return typedValue.getFloat();
    }

    public static boolean hasPackageInstalled(Context context, String pkgName) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(pkgName, 0);
            return ai.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
