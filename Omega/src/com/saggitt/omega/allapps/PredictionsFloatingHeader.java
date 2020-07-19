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

package com.saggitt.omega.allapps;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.launcher3.Insettable;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.AllAppsContainerView;
import com.android.launcher3.allapps.FloatingHeaderView;
import com.android.launcher3.appprediction.ComponentKeyMapper;
import com.android.launcher3.appprediction.PredictionRowView;
import com.android.launcher3.appprediction.PredictionUiStateManager;

import java.util.List;

public class PredictionsFloatingHeader extends FloatingHeaderView implements Insettable {
    private final PredictionUiStateManager mPredictionUiStateManager;
    private Context mContext;
    private boolean mShowAllAppsLabel;
    private PredictionRowView mPredictionRowView;

    public PredictionsFloatingHeader(@NonNull Context context) {
        this(context, null);
    }

    public PredictionsFloatingHeader(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mPredictionUiStateManager = PredictionUiStateManager.INSTANCE.get(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mPredictionRowView = findViewById(R.id.prediction_row);
        updateShowAllAppsLabel();
    }

    @Override
    public void setup(AllAppsContainerView.AdapterHolder[] mAH, boolean tabsHidden) {
        mTabsHidden = tabsHidden;
        updateExpectedHeight();
        super.setup(mAH, tabsHidden);
    }

    public void updateShowAllAppsLabel() {
        setShowAllAppsLabel(Utilities.getOmegaPrefs(mContext).getShowAllAppsLabel());
    }

    public void setShowAllAppsLabel(boolean show) {
        if (mShowAllAppsLabel != show) {
            mShowAllAppsLabel = show;
        }
    }

    public PredictionRowView getPredictionRowView() {
        return mPredictionRowView;
    }

    public boolean hasVisibleContent() {
        return mPredictionUiStateManager.getCurrentState().isEnabled;
    }

    public void setPredictedApps(boolean enabled, List<ComponentKeyMapper> list) {
        mPredictionUiStateManager.getCurrentState().apps = list;
        mPredictionUiStateManager.getCurrentState().isEnabled = enabled;
        mPredictionUiStateManager.onAppsUpdated();
    }
}
