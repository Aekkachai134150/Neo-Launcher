/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.tapl;

import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiObject2;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Common overview panel for both Launcher and fallback recents
 */
public class BaseOverview extends LauncherInstrumentation.VisibleContainer {
    private static final int FLINGS_FOR_DISMISS_LIMIT = 40;

    BaseOverview(LauncherInstrumentation launcher) {
        super(launcher);
        verifyActiveContainer();
        verifyActionsViewVisibility();
    }

    @Override
    protected LauncherInstrumentation.ContainerType getContainerType() {
        return LauncherInstrumentation.ContainerType.FALLBACK_OVERVIEW;
    }

    /**
     * Flings forward (left) and waits the fling's end.
     */
    public void flingForward() {
        try (LauncherInstrumentation.Closable e = mLauncher.eventsCheck()) {
            flingForwardImpl();
        }
    }

    private void flingForwardImpl() {
        try (LauncherInstrumentation.Closable c =
                     mLauncher.addContextLayer("want to fling forward in overview")) {
            LauncherInstrumentation.log("Overview.flingForward before fling");
            final UiObject2 overview = verifyActiveContainer();
            final int leftMargin =
                    mLauncher.getTargetInsets().left + mLauncher.getEdgeSensitivityWidth();
            mLauncher.scroll(overview, Direction.LEFT, new Rect(leftMargin + 1, 0, 0, 0), 20,
                    false);
            try (LauncherInstrumentation.Closable c2 =
                         mLauncher.addContextLayer("flung forwards")) {
                verifyActiveContainer();
                verifyActionsViewVisibility();
            }
        }
    }

    /**
     * Dismissed all tasks by scrolling to Clear-all button and pressing it.
     */
    public void dismissAllTasks() {
        try (LauncherInstrumentation.Closable e = mLauncher.eventsCheck();
             LauncherInstrumentation.Closable c = mLauncher.addContextLayer(
                     "dismissing all tasks")) {
            final BySelector clearAllSelector = mLauncher.getOverviewObjectSelector("clear_all");
            for (int i = 0;
                 i < FLINGS_FOR_DISMISS_LIMIT
                         && !verifyActiveContainer().hasObject(clearAllSelector);
                 ++i) {
                flingForwardImpl();
            }

            mLauncher.clickLauncherObject(
                    mLauncher.waitForObjectInContainer(verifyActiveContainer(), clearAllSelector));

            mLauncher.waitUntilLauncherObjectGone(clearAllSelector);
        }
    }

    /**
     * Flings backward (right) and waits the fling's end.
     */
    public void flingBackward() {
        try (LauncherInstrumentation.Closable e = mLauncher.eventsCheck();
             LauncherInstrumentation.Closable c =
                     mLauncher.addContextLayer("want to fling backward in overview")) {
            LauncherInstrumentation.log("Overview.flingBackward before fling");
            final UiObject2 overview = verifyActiveContainer();
            final int rightMargin =
                    mLauncher.getTargetInsets().right + mLauncher.getEdgeSensitivityWidth();
            mLauncher.scroll(
                    overview, Direction.RIGHT, new Rect(0, 0, rightMargin + 1, 0), 20, false);
            try (LauncherInstrumentation.Closable c2 =
                         mLauncher.addContextLayer("flung backwards")) {
                verifyActiveContainer();
                verifyActionsViewVisibility();
            }
        }
    }

    /**
     * Scrolls the current task via flinging forward until it is off screen.
     * <p>
     * If only one task is present, it is only partially scrolled off screen and will still be
     * the current task.
     */
    public void scrollCurrentTaskOffScreen() {
        try (LauncherInstrumentation.Closable e = mLauncher.eventsCheck();
             LauncherInstrumentation.Closable c = mLauncher.addContextLayer(
                     "want to scroll current task off screen in overview")) {
            verifyActiveContainer();

            OverviewTask task = getCurrentTask();
            mLauncher.assertNotNull("current task is null", task);
            mLauncher.scrollLeftByDistance(verifyActiveContainer(),
                    task.getVisibleWidth() + mLauncher.getOverviewPageSpacing());

            try (LauncherInstrumentation.Closable c2 =
                         mLauncher.addContextLayer("scrolled task off screen")) {
                verifyActiveContainer();
                verifyActionsViewVisibility();

                if (getTaskCount() > 1) {
                    if (mLauncher.isTablet()) {
                        mLauncher.assertTrue("current task is not grid height",
                                getCurrentTask().getVisibleHeight() == mLauncher
                                        .getGridTaskRectForTablet().height());
                    }
                    mLauncher.assertTrue("Current task not scrolled off screen",
                            !getCurrentTask().equals(task));
                }
            }
        }
    }

    /**
     * Gets the current task in the carousel, or fails if the carousel is empty.
     *
     * @return the task in the middle of the visible tasks list.
     */
    @NonNull
    public OverviewTask getCurrentTask() {
        final List<UiObject2> taskViews = getTasks();
        mLauncher.assertNotEquals("Unable to find a task", 0, taskViews.size());

        // taskViews contains up to 3 task views: the 'main' (having the widest visible part) one
        // in the center, and parts of its right and left siblings. Find the main task view by
        // its width.
        final UiObject2 widestTask = Collections.max(taskViews,
                (t1, t2) -> Integer.compare(mLauncher.getVisibleBounds(t1).width(),
                        mLauncher.getVisibleBounds(t2).width()));

        return new OverviewTask(mLauncher, widestTask, this);
    }

    /**
     * Returns an overview task matching TestActivity {@param activityNumber}.
     */
    @NonNull
    public OverviewTask getTestActivityTask(int activityNumber) {
        final List<UiObject2> taskViews = getTasks();
        mLauncher.assertNotEquals("Unable to find a task", 0, taskViews.size());

        final String activityName = "TestActivity" + activityNumber;
        UiObject2 task = null;
        for (UiObject2 taskView : taskViews) {
            // TODO(b/239452415): Use equals instead of descEndsWith
            if (taskView.getParent().hasObject(By.descEndsWith(activityName))) {
                task = taskView;
                break;
            }
        }
        mLauncher.assertNotNull(
                "Unable to find a task with " + activityName + " from the task list", task);

        return new OverviewTask(mLauncher, task, this);
    }

    /**
     * Returns a list of all tasks fully visible in the tablet grid overview.
     */
    @NonNull
    public List<OverviewTask> getCurrentTasksForTablet() {
        final List<UiObject2> taskViews = getTasks();
        mLauncher.assertNotEquals("Unable to find a task", 0, taskViews.size());

        final int gridTaskWidth = mLauncher.getGridTaskRectForTablet().width();

        return taskViews.stream().filter(t -> t.getVisibleBounds().width() == gridTaskWidth).map(
                t -> new OverviewTask(mLauncher, t, this)).collect(Collectors.toList());
    }

    @NonNull
    private List<UiObject2> getTasks() {
        try (LauncherInstrumentation.Closable c = mLauncher.addContextLayer(
                "want to get overview tasks")) {
            verifyActiveContainer();
            return mLauncher.getDevice().findObjects(
                    mLauncher.getOverviewObjectSelector("snapshot"));
        }
    }

    int getTaskCount() {
        return getTasks().size();
    }

    /**
     * Returns whether Overview has tasks.
     */
    public boolean hasTasks() {
        return getTasks().size() > 0;
    }

    /**
     * Gets Overview Actions.
     *
     * @return The Overview Actions
     */
    @NonNull
    public OverviewActions getOverviewActions() {
        try (LauncherInstrumentation.Closable c = mLauncher.addContextLayer(
                "want to get overview actions")) {
            verifyActiveContainer();
            UiObject2 overviewActions = mLauncher.waitForOverviewObject("action_buttons");
            return new OverviewActions(overviewActions, mLauncher);
        }
    }

    /**
     * Returns if clear all button is visible.
     */
    public boolean isClearAllVisible() {
        return mLauncher.hasLauncherObject(mLauncher.getOverviewObjectSelector("clear_all"));
    }

    protected boolean isActionsViewVisible() {
        OverviewTask task = mLauncher.isTablet() ? getFocusedTaskForTablet() : getCurrentTask();
        if (task == null) {
            return false;
        }
        return !task.isTaskSplit();
    }

    private void verifyActionsViewVisibility() {
        if (!hasTasks() || !isActionsViewVisible()) {
            return;
        }
        try (LauncherInstrumentation.Closable c = mLauncher.addContextLayer(
                "want to assert overview actions view visibility")) {
            if (mLauncher.isTablet() && !isOverviewSnappedToFocusedTaskForTablet()) {
                mLauncher.waitUntilOverviewObjectGone("action_buttons");
            } else {
                mLauncher.waitForOverviewObject("action_buttons");
            }
        }
    }

    /**
     * Returns if focused task is currently snapped task in tablet grid overview.
     */
    private boolean isOverviewSnappedToFocusedTaskForTablet() {
        OverviewTask focusedTask = getFocusedTaskForTablet();
        if (focusedTask == null) {
            return false;
        }
        return Math.abs(focusedTask.getExactCenterX() - mLauncher.getExactScreenCenterX()) < 1;
    }

    /**
     * Returns Overview focused task if it exists.
     *
     * @throws IllegalStateException if not run on a tablet device.
     */
    OverviewTask getFocusedTaskForTablet() {
        if (!mLauncher.isTablet()) {
            throw new IllegalStateException("Must be run on tablet device.");
        }
        final List<UiObject2> taskViews = getTasks();
        if (taskViews.size() == 0) {
            return null;
        }
        int focusedTaskHeight = mLauncher.getFocusedTaskHeightForTablet();
        for (UiObject2 task : taskViews) {
            if (task.getVisibleBounds().height() == focusedTaskHeight) {
                return new OverviewTask(mLauncher, task, this);
            }
        }
        return null;
    }
}
