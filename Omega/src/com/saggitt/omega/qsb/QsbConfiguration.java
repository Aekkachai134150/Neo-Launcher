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

import android.annotation.TargetApi;
import android.content.Context;

import java.util.ArrayList;

@TargetApi(26)
public class QsbConfiguration {

    private static QsbConfiguration INSTANCE;
    private final ArrayList<QsbChangeListener> mListeners = new ArrayList<>(2);

    private QsbConfiguration(Context context) {

    }

    private void notifyListeners() {
        for (QsbChangeListener listener : mListeners) {
            listener.onChange();
        }
    }

    public static QsbConfiguration getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new QsbConfiguration(context.getApplicationContext());
        }
        return INSTANCE;
    }

    public final float micStrokeWidth() {
        // pixel_2018_qsb_mic_stroke_width_dp
        return 0f;
    }

    public final String hintTextValue() {
        // pixel_2017_qsb_hint_text_value
        return "";
    }

    public final boolean hintIsForAssistant() {
        // pixel_2018_qsb_hint_is_for_assistant
        return false;
    }

    public final void addListener(QsbChangeListener qsbChangeListener) {
        this.mListeners.add(qsbChangeListener);
    }

    public final void removeListener(QsbChangeListener qsbChangeListener) {
        this.mListeners.remove(qsbChangeListener);
    }
}
