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

package com.saggitt.omega.smartspace.eventprovider;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.MediaSessionManager.OnActiveSessionsChangedListener;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.Nullable;

import com.android.launcher3.Utilities;
import com.android.launcher3.notification.NotificationListener;
import com.saggitt.omega.smartspace.eventprovider.NotificationsManager.OnChangeListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.saggitt.omega.util.OmegaUtilsKt.makeBasicHandler;

/**
 * Paused mode is not supported on Marshmallow because the MediaSession is missing
 * notifications. Without this information, it is impossible to hide on stop.
 */

public class MediaListener extends MediaController.Callback implements OnChangeListener {
    private static final String TAG = "MediaListener";

    private final Context mContext;
    private final Runnable mOnChange;
    private final NotificationsManager mNotificationsManager;
    private final Handler mHandler = makeBasicHandler(true);
    private MediaNotificationController mTracking;
    private List<MediaNotificationController> mControllers = Collections.emptyList();

    MediaListener(Context context, Runnable onChange) {
        mContext = context;
        mOnChange = onChange;
        mNotificationsManager = NotificationsManager.INSTANCE;
    }

    void onResume() {
        updateTracking();
        mNotificationsManager.addListener(this);
    }

    void onPause() {
        updateTracking();
        mNotificationsManager.removeListener(this);
    }

    MediaNotificationController getTracking() {
        return mTracking;
    }

    private void updateControllers(List<MediaNotificationController> controllers) {
        for (MediaNotificationController mnc : mControllers) {
            mnc.controller.unregisterCallback(this);
        }
        for (MediaNotificationController mnc : controllers) {
            mnc.controller.registerCallback(this);
        }
        mControllers = controllers;
    }

    private void updateTracking() {
        updateControllers(getControllers());

        if (mTracking != null) {
            mTracking.reloadInfo();
        }

        // If the current controller is not playing, stop tracking it.
        if (mTracking != null
                && (!mControllers.contains(mTracking) || !mTracking.isPlaying())) {
            mTracking = null;
        }

        for (MediaNotificationController mnc : mControllers) {
            // Either we are not tracking a controller and this one is valid,
            // or this one is playing while the one we track is not.
            if ((mTracking == null && mnc.isPlaying())
                    || (mTracking != null && mnc.isPlaying() && !mTracking.isPlaying())) {
                mTracking = mnc;
            }
        }

        mHandler.removeCallbacks(mOnChange);
        mHandler.post(mOnChange);
    }

    private void pressButton(int keyCode) {
        if (mTracking != null) {
            mTracking.pressButton(keyCode);
        }
    }

    void toggle(boolean finalClick) {
        if (!finalClick) {
            Log.d(TAG, "Toggle");
            pressButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        }
    }

    void next(boolean finalClick) {
        if (finalClick) {
            Log.d(TAG, "Next");
            pressButton(KeyEvent.KEYCODE_MEDIA_NEXT);
            pressButton(KeyEvent.KEYCODE_MEDIA_PLAY);
        }
    }

    private List<MediaNotificationController> getControllers() {
        List<MediaNotificationController> controllers = new ArrayList<>();
        for (StatusBarNotification notif : mNotificationsManager.getSbNotifications()) {
            Bundle extras = notif.getNotification().extras;
            MediaSession.Token notifToken = extras.getParcelable(Notification.EXTRA_MEDIA_SESSION);
            if (notifToken != null) {
                MediaController controller = new MediaController(mContext, notifToken);
                controllers.add(new MediaNotificationController(controller, notif));
            }
        }
        return controllers;
    }

    /**
     * Events that refresh the current handler.
     */
    public void onPlaybackStateChanged(PlaybackState state) {
        super.onPlaybackStateChanged(state);
        updateTracking();
    }

    public void onMetadataChanged(MediaMetadata metadata) {
        super.onMetadataChanged(metadata);
        updateTracking();
    }

    @Override
    public void onNotificationsChanged() {
        updateTracking();
    }

    public class MediaInfo {

        private CharSequence title;
        private CharSequence artist;
        private CharSequence album;

        public CharSequence getTitle() {
            return title;
        }

        public CharSequence getArtist() {
            return artist;
        }

        public CharSequence getAlbum() {
            return album;
        }
    }

    class MediaNotificationController {

        private MediaController controller;
        private StatusBarNotification sbn;
        private MediaInfo info;

        private MediaNotificationController(MediaController controller, StatusBarNotification sbn) {
            this.controller = controller;
            this.sbn = sbn;
            reloadInfo();
        }

        private boolean hasTitle() {
            return info != null && info.title != null;
        }

        private boolean isPlaying() {
            return hasTitle()
                    && controller.getPlaybackState() != null
                    && controller.getPlaybackState().getState() == PlaybackState.STATE_PLAYING;
        }

        private void pressButton(int keyCode) {
            controller.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
            controller.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
        }

        private void reloadInfo() {
            MediaMetadata metadata = controller.getMetadata();
            if (metadata != null) {
                info = new MediaInfo();
                info.title = metadata.getText(MediaMetadata.METADATA_KEY_TITLE);
                info.artist = metadata.getText(MediaMetadata.METADATA_KEY_ARTIST);
                info.album = metadata.getText(MediaMetadata.METADATA_KEY_ALBUM);
            } else if (sbn != null) {
                info = new MediaInfo();
                info.title = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE);
            }
        }

        public String getPackageName() {
            return controller.getPackageName();
        }

        public StatusBarNotification getSbn() {
            return sbn;
        }

        public MediaInfo getInfo() {
            return info;
        }
    }
}