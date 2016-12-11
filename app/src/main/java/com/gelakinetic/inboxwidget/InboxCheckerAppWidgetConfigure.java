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

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.google.android.gm.contentprovider.GmailContract;

import java.io.IOException;

public class InboxCheckerAppWidgetConfigure extends Activity {

    private static final int REQUEST_CODE = 15613;
    private static final String ACCOUNT_TYPE_GOOGLE = "com.google";
    private static final String[] FEATURES_MAIL = {"service_mail"};
    static final String SHARED_PREFS = "Preferences";

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    /**
     * This is called when the widget needs configuring. It will check permissions, get accounts,
     * prompt the user to select an account, and save the widget
     */
    @Override
    public void onResume() {
        super.onResume();

        /* Get the app widget ID we're configuring */
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (extras == null || mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            /* Not configuring the widget, just close */
            this.finish();
            return;
        }

        /* Check for required permissions */
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) !=
                PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, GmailContract.PERMISSION) !=
                        PackageManager.PERMISSION_GRANTED) {

            /* If the permissions aren't granted, ask for them */
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.GET_ACCOUNTS,
                            GmailContract.PERMISSION},
                    REQUEST_CODE);
            return;
        }

        /* All permissions granted, go get the list of accounts */
        getAccounts();
    }

    /**
     * This is called after permissions were either granted or denied. If permissions were denied,
     * close the activity. If permissions were granted, get a list of accounts
     *
     * @param requestCode  The request code passed in requestPermissions(String[], int).
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions which is either
     *                     PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        /* Check if the requested permissions were granted */
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                /* Something was denied, pop a toast and finish */
                Toast.makeText(this, R.string.required_permissions, Toast.LENGTH_LONG).show();
                this.finish();
                return;
            }
        }

        /* All permissions granted, continue */
        getAccounts();
    }

    /**
     * Start the process to get all Inbox accounts. Will call onAccountResults() when the accounts
     * are retrieved.
     *
     * @throws SecurityException If permissions weren't granted, this is called
     */
    private void getAccounts() throws SecurityException {
        /* Get all eligible accounts */
        AccountManager.get(this).getAccountsByTypeAndFeatures(ACCOUNT_TYPE_GOOGLE, FEATURES_MAIL,
                new AccountManagerCallback<Account[]>() {
                    @Override
                    public void run(AccountManagerFuture<Account[]> future) {
                        Account[] accounts = null;
                        try {
                            accounts = future.getResult();
                        } catch (OperationCanceledException | IOException |
                                AuthenticatorException oce) {
                            /* Eat it */
                        }
                        onAccountResults(accounts);
                    }
                }, null);
    }

    /**
     * Called once accounts are retrieved
     *
     * @param accounts A list of accounts on this device
     */
    private void onAccountResults(Account[] accounts) {

        /* If there are multiple accounts */
        if (accounts.length > 1) {

            /* Extract all the account names */
            int idx = 0;
            final String accountNames[] = new String[accounts.length];
            for (Account account : accounts) {
                accountNames[idx++] = account.name;
            }

            /* Display a dialog to select an account */
            (new AlertDialog.Builder(this))
                    .setTitle(R.string.accounts)
                    .setItems(accountNames, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            /* Commit the user's choice */
                            getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
                                    .edit()
                                    .putString(String.valueOf(mAppWidgetId), accountNames[i])
                                    .apply();
                            /* Update the widget */
                            finishAndUpdateWidget();
                        }
                    }).show();
        } else if (accounts.length == 1) {
            /* There's only one account, use it without prompting the user */
            getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(String.valueOf(mAppWidgetId), accounts[0].name)
                    .apply();
            finishAndUpdateWidget();
        } else {
            /* There are no accounts, the widget can't do anything */
            Toast.makeText(this, R.string.no_accounts, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * This finishes the Activity and updates the widget. Appropriately named.
     */
    private void finishAndUpdateWidget() {

        /* Tell the widget to update */
        updateWidgets(getApplicationContext());

        /* Start the poll */
        InboxCheckerPollingAlarm.setAlarm(this);

        /* Return */
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    /**
     * Sends a broadcast to call onUpdate() for the Inbox Widgets
     *
     * @param context A context to send the broadcast
     */
    public static void updateWidgets(Context context) {
        context.sendBroadcast(getUpdateWidgetIntent(context));
    }

    /**
     * @param context A context to build the intent with
     * @return An Intent which will call onUpdate for all Inbox Widgets
     */
    private static Intent getUpdateWidgetIntent(Context context) {
        /* Get an intent for the InboxCheckerAppWidgetProvider class */
        Intent intent = new Intent(context.getApplicationContext(),
                InboxCheckerAppWidgetProvider.class);
        /* Tell it to update widgets */
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        /* Get a list of IDs for all the widgets */
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        int[] ids = widgetManager.getAppWidgetIds(new ComponentName(context,
                InboxCheckerAppWidgetProvider.class));

        /* Tell the intent which widgets to update */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            widgetManager.notifyAppWidgetViewDataChanged(ids, android.R.id.list);
        }
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);

        /* Return the intent */
        return intent;
    }
}
