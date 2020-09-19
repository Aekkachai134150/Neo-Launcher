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

package com.saggitt.omega.icons;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.launcher3.Utilities;

import java.lang.reflect.Field;
import java.util.Arrays;

import static com.android.launcher3.Utilities.getDevicePrefs;
import static com.android.launcher3.Utilities.getPrefs;

/**
 * Utility class to override shape of {@link android.graphics.drawable.AdaptiveIconDrawable}.
 */
@TargetApi(Build.VERSION_CODES.O)
public class IconShapeOverride {

    public static final String KEY_PREFERENCE = "pref_override_icon_shape";
    private static final String TAG = "IconShapeOverride";

    public static boolean isSupported() {
        if (!Utilities.ATLEAST_OREO) {
            return false;
        }

        try {
            if (getSystemResField().get(null) != Resources.getSystem()) {
                // Our assumption that mSystem is the system resource is not true.
                return false;
            }
        } catch (Exception e) {
            // Ignore, not supported
            return false;
        }

        return getConfigResId() != 0;
    }

    public static void apply(Context context) {
        if (!Utilities.ATLEAST_OREO) {
            return;
        }
        String path = getAppliedValue(context);
        if (TextUtils.isEmpty(path)) {
            return;
        }
        if (!isSupported()) {
            return;
        }

        // magic
        try {
            ResourcesOverride override =
                    new ResourcesOverride(Resources.getSystem(), getConfigResId(), path);
            getSystemResField().set(null, override);
            int masks = getOverrideMasksResId();
            if (masks != 0) {
                override.setArrayOverrideId(masks);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to override icon shape", e);
            // revert value.
            getPrefs(context).edit().remove(KEY_PREFERENCE).apply();
        }
    }

    private static Field getSystemResField() throws Exception {
        Field staticField = Resources.class.getDeclaredField("mSystem");
        staticField.setAccessible(true);
        return staticField;
    }

    private static int getConfigResId() {
        return Resources.getSystem().getIdentifier("config_icon_mask", "string", "android");
    }

    private static int getOverrideMasksResId() {
        return Resources.getSystem().getIdentifier("system_icon_masks", "array", "android");
    }

    public static String getAppliedValue(Context context) {
        String devValue = getDevicePrefs(context).getString(KEY_PREFERENCE, "");
        if (!TextUtils.isEmpty(devValue)) {
            // Migrate to general preferences to back up shape overrides
            getPrefs(context).edit().putString(KEY_PREFERENCE, devValue).apply();
            ;
            getDevicePrefs(context).edit().remove(KEY_PREFERENCE).apply();
        }

        return getPrefs(context).getString(KEY_PREFERENCE, "");
    }

    private static class ResourcesOverride extends Resources {

        private final int mOverrideId;
        private final String mOverrideValue;
        private int mArrayOverrideId = 0;

        @SuppressWarnings("deprecation")
        public ResourcesOverride(Resources parent, int overrideId, String overrideValue) {
            super(parent.getAssets(), parent.getDisplayMetrics(), parent.getConfiguration());
            mOverrideId = overrideId;
            mOverrideValue = overrideValue;
        }

        @NonNull
        @Override
        public String getString(int id) throws NotFoundException {
            if (id == mOverrideId) {
                return mOverrideValue;
            }
            return super.getString(id);
        }

        void setArrayOverrideId(int id) {
            mArrayOverrideId = id;
        }

        // I do admit that this is one hell of a hack
        @NonNull
        @Override
        public String[] getStringArray(int id) throws NotFoundException {
            if (id != 0 && id == mArrayOverrideId) {
                int size = super.getStringArray(id).length;
                String[] arr = new String[size];
                Arrays.fill(arr, mOverrideValue);
                return arr;
            }
            return super.getStringArray(id);
        }
    }
}
