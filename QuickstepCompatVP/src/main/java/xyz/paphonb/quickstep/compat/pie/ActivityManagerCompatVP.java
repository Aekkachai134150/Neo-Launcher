package xyz.paphonb.quickstep.compat.pie;

import static android.app.ActivityManager.SPLIT_SCREEN_CREATE_MODE_TOP_OR_LEFT;
import static android.app.WindowConfiguration.WINDOWING_MODE_PINNED;
import static android.app.WindowConfiguration.ACTIVITY_TYPE_UNDEFINED;
import static android.app.WindowConfiguration.WINDOWING_MODE_UNDEFINED;

import android.app.ActivityManager;
import android.app.ActivityManager.TaskSnapshot;
import android.app.IAssistDataReceiver;
import android.app.ITaskStackListener;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.IRecentsAnimationController;
import android.view.IRecentsAnimationRunner;
import android.view.RemoteAnimationTarget;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import xyz.paphonb.quickstep.compat.ActivityManagerCompat;
import xyz.paphonb.quickstep.compat.RecentsAnimationRunner;

public class ActivityManagerCompatVP extends ActivityManagerCompat {

    @Override
    public List<ActivityManager.RunningTaskInfo> getFilteredTasks(int maxNum,
                                                                  boolean filterOnlyVisibleRecents) throws RemoteException {
        return ActivityManager.getService()
                .getFilteredTasks(maxNum, ACTIVITY_TYPE_UNDEFINED, WINDOWING_MODE_UNDEFINED);
    }

    @Override
    public List<ActivityManager.RecentTaskInfo> getRecentTasks(int maxNum, int flags, int userId)
            throws RemoteException {
        return ActivityManager.getService().getRecentTasks(maxNum, flags, userId).getList();
    }

    @Override
    public ActivityManager.TaskSnapshot getTaskSnapshot(int taskId, boolean reducedResolution)
            throws RemoteException {
        return ActivityManager.getService().getTaskSnapshot(taskId, reducedResolution);
    }

    @Override
    public void startRecentsActivity(Intent intent, IAssistDataReceiver assistDataReceiver,
                                     final RecentsAnimationRunner recentsAnimationRunner) throws RemoteException {
        IRecentsAnimationRunner runner = null;
        if (recentsAnimationRunner != null) {
            runner = new IRecentsAnimationRunner.Stub() {
                @Override
                public void onAnimationStart(IRecentsAnimationController controller,
                                             RemoteAnimationTarget[] apps, Rect homeContentInsets,
                                             Rect minimizedHomeBounds) {
                    recentsAnimationRunner.onAnimationStart(controller, apps,
                            new RemoteAnimationTarget[0], homeContentInsets, minimizedHomeBounds);
                }

                @Override
                public void onAnimationCanceled() {
                    recentsAnimationRunner.onAnimationCanceled(null);
                }
            };
        }
        ActivityManager.getService().startRecentsActivity(intent, assistDataReceiver, runner);
    }

    @Override
    public void cancelRecentsAnimation(boolean restoreHomeStackPosition) throws RemoteException {
        ActivityManager.getService().cancelRecentsAnimation(restoreHomeStackPosition);
    }

    @Override
    public int startActivityFromRecents(int taskId, Bundle bOptions) throws RemoteException {
        return ActivityManager.getService().startActivityFromRecents(taskId, bOptions);
    }

    @Override
    public boolean setTaskWindowingModeSplitScreenPrimary(
            int taskId, boolean toTop) throws RemoteException {
        return ActivityManager.getService().setTaskWindowingModeSplitScreenPrimary(taskId,
                SPLIT_SCREEN_CREATE_MODE_TOP_OR_LEFT, toTop, true, null, true);
    }

    @Override
    public boolean removeTask(int taskId) throws RemoteException {
        return ActivityManager.getService().removeTask(taskId);
    }

    @Override
    public void removeAllVisibleRecentTasks() throws RemoteException {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void cancelTaskWindowTransition(int taskId) throws RemoteException {
        ActivityManager.getService().cancelTaskWindowTransition(taskId);
    }

    @Override
    public int getLockTaskModeState() throws RemoteException {
        return ActivityManager.getService().getLockTaskModeState();
    }

    @Override
    public void registerTaskStackListener(ITaskStackListener listener) throws RemoteException {
        ActivityManager.getService().registerTaskStackListener(listener);
    }

    @Override
    public void unregisterTaskStackListener(ITaskStackListener listener) throws RemoteException {
        ActivityManager.getService().unregisterTaskStackListener(listener);
    }

    @Override
    public boolean supportsMultiWindow(Context context) {
        return ActivityManager.supportsMultiWindow(context);
    }

    @Override
    public int getDisplayId(Context context) {
        return context.getDisplay().getDisplayId();
    }
}
