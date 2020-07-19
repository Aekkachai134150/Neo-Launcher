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

package com.saggitt.omega.search.providers;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherState;
import com.android.launcher3.R;
import com.saggitt.omega.search.SearchProvider;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class AppSearchSearchProvider extends SearchProvider {

    public AppSearchSearchProvider(Context context) {
        super(context);
        mContext = context;

        providerName = mContext.getString(R.string.search_provider_appsearch);
        supportsAssistant = false;
        supportsFeed = false;
        supportsVoiceSearch = false;
    }

    @Override
    public Drawable getIcon() {
        return mContext.getDrawable(R.drawable.ic_search).mutate();
    }

    @Override
    public Drawable getVoiceIcon() {
        return null;
    }

    @Override
    public Drawable getAssistantIcon() {
        return null;
    }

    @Override
    public void startSearch(Function1<Intent, Unit> callback) {
        Launcher launcher = LauncherAppState.getInstanceNoCreate().getLauncher();
        launcher.getStateManager().goToState(LauncherState.ALL_APPS, true, (Runnable) (new Runnable() {
            public final void run() {
                launcher.getAppsView().getSearchUiManager().startSearch();
            }
        }));
    }
}
