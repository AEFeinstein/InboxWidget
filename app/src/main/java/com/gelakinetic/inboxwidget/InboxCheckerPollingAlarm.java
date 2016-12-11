/**
 * Copyright 2016 Adam Feinstein
 * <p/>
 * This file is part of Inbox Widget.
 * <p/>
 * Inbox Widget is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * Inbox Widget is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with Inbox Widget.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gelakinetic.inboxwidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public class InboxCheckerPollingAlarm extends BroadcastReceiver {

    private static final int POLLING_INTERVAL_MS = 1000 * 60;

    /**
     * Build and return a PendingIntent for the Alarm to call
     *
     * @param context The Context to build the intent with
     * @return A PendingIntent to either set or clear
     */
    private static PendingIntent getPendingIntent(Context context) {
        Intent intent = new Intent(context, InboxCheckerPollingAlarm.class);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    /**
     * Set the polling alarm. This is called when a widget is added, and on boot. The alarm is
     * set to poll every minute, inexactly
     *
     * @param context A Context to set the alarm with
     */
    public static void setAlarm(Context context) {

        /* First, cancel any pending alarms, just in case */
        cancelAlarm(context);

        /* Only set an alarm if there is at least one widget */
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        int[] ids = widgetManager.getAppWidgetIds(new ComponentName(context,
                InboxCheckerAppWidgetProvider.class));
        if (ids.length == 0) {
            return;
        }

        /* Set it to repeat every minute in an inexact fashion. This saves battery because the
         * system can bunch together alarms
         */
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + POLLING_INTERVAL_MS,
                POLLING_INTERVAL_MS,
                getPendingIntent(context));
    }

    /**
     * Cancel the polling Alarm. This is called when the preference is toggled or before resetting
     * the alarm
     *
     * @param context a Context to cancel the alarm with
     */
    private static void cancelAlarm(Context context) {
        /* Cancel the alarm by canceling an equivalent pending intent */
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getPendingIntent(context));
    }

    /**
     * When the Alarm fires, update the widgets
     *
     * @param context A context that fired the alarm
     * @param intent  The intent that fired the alarm
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        InboxCheckerAppWidgetConfigure.updateWidgets(context);
    }
}
