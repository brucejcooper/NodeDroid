/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.eightbitcloud.internode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class PreferencesActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        PreferenceGroup ps = (PreferenceGroup) getPreferenceScreen().getPreference(0);
        Preference send = ps.getPreference(1);
        send.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                boolean extraLogging = PreferenceManager.getDefaultSharedPreferences(PreferencesActivity.this).getBoolean("performExtraLogging", false);
                if (extraLogging) {
                    sendLogs();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(PreferencesActivity.this);
                    builder.setMessage("Logs will be much more useful if you do a refresh from the main screen with Detailed Logging turned on first.  Do you still want to send only the brief logs?")
                           .setCancelable(true)
                           .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                               public void onClick(DialogInterface confirmDialog, int id) {
                                   confirmDialog.cancel();
                                   sendLogs();
                               }
                           }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                               public void onClick(DialogInterface confirmDialog, int id) {
                                   confirmDialog.cancel();
                              }
                          });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
                return true;
            }
        });

    }

    public void sendLogs() {
        Toast.makeText(this, "Preparing logs. Email app will appear soon", Toast.LENGTH_SHORT).show();
        
        try {
            final File tempFile = File.createTempFile("NodeDroid", ".dump");
            tempFile.deleteOnExit();
            final Process p = Runtime.getRuntime().exec(new String[] {"/system/bin/logcat", "-d", "-f", tempFile.getAbsolutePath(), "-v", "threadtime", "NodeDroid:V", "*:S"});
            final Handler mHandler = new Handler();
    
            
            new Thread(new Runnable() {
                Exception ex = null;
                public void run() {
                    try {
                        p.waitFor();
                    } catch (Exception ex ) {
                        this.ex = ex;
                    }
    
                    mHandler.post(new Runnable() {
    
                        public void run() {
                            if (ex != null) {
                                Toast t = Toast.makeText(PreferencesActivity.this, "Error Preparing logs: " + ex, Toast.LENGTH_LONG);
                                t.show();
                            } else {
                                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                                // Add attributes to the intent
                                sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Log dump from NodeDroid");
                                sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { "android-logs@8bitcloud.com" });
                                sendIntent.putExtra(Intent.EXTRA_TEXT, "Body of email");
                                sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tempFile));
                                sendIntent.setType("vnd.android.cursor.dir/email");
                        
                                startActivity(Intent.createChooser(sendIntent, "Email:"));
                            }
                        }
                        
                    });
                }
            }).start();
            
        } catch (IOException ex) {
            Toast.makeText(PreferencesActivity.this, "Error Preparing logs: " + ex, Toast.LENGTH_LONG).show();
        }

    }
    
    
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(NodeUsage.TAG, "Finished Sending Email. Request Code is " + requestCode +", result is " + resultCode);
    }

    
}
