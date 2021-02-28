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

package com.saggitt.omega.model;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.util.Log;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherAppWidgetProviderInfo;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.LauncherSettings.Favorites;
import com.android.launcher3.LauncherSettings.Settings;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.model.GridSizeMigrationTask;
import com.android.launcher3.provider.LauncherDbUtils.SQLiteTransaction;
import com.android.launcher3.util.GridOccupancy;
import com.android.launcher3.util.IntArray;
import com.android.launcher3.widget.custom.CustomWidgetManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.saggitt.omega.settings.SettingsActivity.ALLOW_OVERLAP_PREF;
import static com.saggitt.omega.settings.SettingsActivity.SMARTSPACE_PREF;

public class HomeWidgetMigrationTask extends GridSizeMigrationTask {

    private static final String TAG = "HomeWidgetMigrationTask";

    public static final String PREF_MIGRATION_STATUS = "pref_migratedSmartspace";

    private final Context mContext;
    private final int mTrgX, mTrgY;
    private final String mTableName;

    private HomeWidgetMigrationTask(Context context,
                                    SQLiteDatabase db,
                                    HashSet<String> validPackages,
                                    boolean usePreviewTable,
                                    Point sourceSize,
                                    Point targetSize) {
        super(context, db, validPackages, usePreviewTable, sourceSize, targetSize);

        mContext = context;
        mTableName = usePreviewTable ? Favorites.PREVIEW_TABLE_NAME : Favorites.TABLE_NAME;
        mTrgX = targetSize.x;
        mTrgY = targetSize.y;
    }

    @SuppressLint("ApplySharedPref")
    public static void migrateIfNeeded(Context context) {
        SharedPreferences prefs = Utilities.getPrefs(context);

        boolean needsMigration = !prefs.getBoolean(PREF_MIGRATION_STATUS, false)
                && prefs.getBoolean(SMARTSPACE_PREF, false);
        if (!needsMigration) return;
        // Save the pref so we only run migration once
        prefs.edit().putBoolean(PREF_MIGRATION_STATUS, true).commit();

        HashSet<String> validPackages = getValidPackages(context);

        InvariantDeviceProfile idp = LauncherAppState.getIDP(context);
        Point size = new Point(idp.numColumns, idp.numRows);

        long migrationStartTime = System.currentTimeMillis();
        try (SQLiteTransaction transaction = (SQLiteTransaction) Settings.call(
                context.getContentResolver(), Settings.METHOD_NEW_TRANSACTION)
                .getBinder(Settings.EXTRA_VALUE)) {
            if (!new HomeWidgetMigrationTask(context, transaction.getDb(),
                    validPackages, false, size, size).migrateWorkspace()) {
                throw new RuntimeException("Failed to migrate Smartspace");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during grid migration", e);
        } finally {
            Log.v(TAG, "Home widget migration completed in "
                    + (System.currentTimeMillis() - migrationStartTime));
        }
    }

    @Override
    protected boolean migrateWorkspace() throws Exception {
        @SuppressLint("VisibleForTests") IntArray allScreens = getWorkspaceScreenIds(mDb, mTableName);
        if (allScreens.isEmpty()) {
            throw new Exception("Unable to get workspace screens");
        }

        boolean allowOverlap = Utilities.getPrefs(mContext)
                .getBoolean(ALLOW_OVERLAP_PREF, false);
        GridOccupancy occupied = new GridOccupancy(mTrgX, mTrgY);

        if (!allowOverlap) {
            ArrayList<DbEntry> firstScreenItems = new ArrayList<>();
            for (int i = 0; i < allScreens.size(); i++) {
                int screenId = allScreens.get(i);
                ArrayList<DbEntry> items = loadWorkspaceEntries(screenId);
                if (screenId == Workspace.FIRST_SCREEN_ID) {
                    firstScreenItems.addAll(items);
                    break;
                }
            }

            for (DbEntry item : firstScreenItems) {
                occupied.markCells(item, true);
            }
        }

        if (allowOverlap || occupied.isRegionVacant(0, 0, mTrgX, 1)) {
            List<LauncherAppWidgetProviderInfo> customWidgets =
                    CustomWidgetManager.INSTANCE.get(mContext).stream()
                            .collect(Collectors.toList());
            if (!customWidgets.isEmpty()) {
                LauncherAppWidgetProviderInfo provider = customWidgets.get(0);
                int widgetId = CustomWidgetManager.INSTANCE.get(mContext)
                        .getWidgetIdForCustomProvider(provider.provider);
                long itemId = LauncherSettings.Settings.call(mContext.getContentResolver(),
                        Settings.METHOD_NEW_ITEM_ID)
                        .getLong(LauncherSettings.Settings.EXTRA_VALUE);

                ContentValues values = new ContentValues();
                values.put(Favorites._ID, itemId);
                values.put(Favorites.CONTAINER, Favorites.CONTAINER_DESKTOP);
                values.put(Favorites.SCREEN, Workspace.FIRST_SCREEN_ID);
                values.put(Favorites.CELLX, 0);
                values.put(Favorites.CELLY, 0);
                values.put(Favorites.SPANX, mTrgX);
                values.put(Favorites.SPANY, 1);
                values.put(Favorites.ITEM_TYPE, Favorites.ITEM_TYPE_CUSTOM_APPWIDGET);
                values.put(Favorites.APPWIDGET_ID, widgetId);
                values.put(Favorites.APPWIDGET_PROVIDER, provider.provider.flattenToString());
                mDb.insert(Favorites.TABLE_NAME, null, values);
            }
        }

        return true;
    }
}
