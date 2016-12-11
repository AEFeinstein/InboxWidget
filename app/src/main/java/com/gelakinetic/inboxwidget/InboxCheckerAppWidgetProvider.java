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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import com.google.android.gm.contentprovider.GmailContract;

public class InboxCheckerAppWidgetProvider extends AppWidgetProvider {

    /**
     * Called periodically by InboxCheckerPollingAlarm in order to check if there is any unread
     * mail, and update the widgets if there is.
     *
     * @param context          The Context in which this receiver is running.
     * @param appWidgetManager A AppWidgetManager object you can call
     *                         updateAppWidget(ComponentName, RemoteViews) on.
     * @param appWidgetIds     The appWidgetIds for which an update is needed. Note that this may be
     *                         all of the AppWidget instances for this provider, or just a subset of
     *                         them.
     */
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        /* Perform this loop procedure for each App Widget that belongs to this provider */
        for (int appWidgetId : appWidgetIds) {
            /* Create an Intent to launch Inbox */
            Intent intent = context.getPackageManager()
                    .getLaunchIntentForPackage("com.google.android.apps.inbox");
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            /* Get the layout for the App Widget and attach an on-click listener to the button */
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.inbox_widget);
            views.setOnClickPendingIntent(R.id.imageView, pendingIntent);

            /* Get the account associated with this widget */
            String account = context
                    .getSharedPreferences(InboxCheckerAppWidgetConfigure.SHARED_PREFS,
                            Context.MODE_PRIVATE)
                    .getString(Integer.toString(appWidgetId), null);
            /* Attempt to query for the number of unread messages */
            if (account != null) {
                int unread = getUnreadMailCount(context, account);
                if (unread > 0) {
                    /* Set the badge text if there is unread mail */
                    views.setTextViewText(R.id.unreadCount, Integer.toString(unread));
                    views.setViewVisibility(R.id.unreadCount, View.VISIBLE);
                } else {
                    /* Clear the badge if there is no unread mail */
                    views.setViewVisibility(R.id.unreadCount, View.GONE);
                }
            }

            /* Tell the AppWidgetManager to perform an update on the current app widget */
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    /**
     * Returns the number of unread mails in the inbox for a given account
     *
     * @param context A context to query with
     * @param account The account name to query
     * @return The number of unread messages in the inbox for the given account
     */
    private static int getUnreadMailCount(Context context, String account) {

        int unread = 0;
        Uri labelsUri = GmailContract.Labels.getLabelsUri(account);
        Cursor cursor = context.getContentResolver().query(
                labelsUri, /* The content URI of the words table */
                null,      /* The columns to return for each row */
                null,      /* Selection criteria */
                null,      /* Selection criteria */
                null);     /* The sort order for the returned rows */

        /* If the query was successful */
        if (cursor != null) {
            /* Iterate over the entire cursor */
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                /* If the name matches CANONICAL_NAME_INBOX */
                String name = cursor
                        .getString(cursor.getColumnIndex(GmailContract.Labels.CANONICAL_NAME));
                if (name.equals(GmailContract.Labels.LabelCanonicalNames.CANONICAL_NAME_INBOX)) {
                    /* Get the unread mail count */
                    unread = cursor.getInt(
                            cursor.getColumnIndex(GmailContract.Labels.NUM_UNREAD_CONVERSATIONS));
                    break;
                }
                cursor.moveToNext();
            }
            cursor.close();
        }
        return unread;
    }
}
