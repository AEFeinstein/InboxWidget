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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class InboxCheckerBootReceiver extends BroadcastReceiver {

    /**
     * Called when the device boots, this allows the app to set the polling alarm
     *
     * @param context A Context to get preferences and set the alarm with
     * @param intent  The intent that called this receiver,
     *                should be android.intent.action.BOOT_COMPLETED
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            InboxCheckerPollingAlarm.setAlarm(context);
        }
    }
}