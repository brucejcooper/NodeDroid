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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

            @Override
            public boolean onPreferenceClick(Preference preference) {
                boolean extraLogging = PreferenceManager.getDefaultSharedPreferences(PreferencesActivity.this).getBoolean("performExtraLogging", false);
                if (extraLogging) {
                    sendLogs();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(PreferencesActivity.this);
                    builder.setMessage("Logs will be much more useful if you do a refresh from the main screen with Detailed Logging turned on first.  Do you still want to send only the brief logs?")
                           .setCancelable(true)
                           .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                               @Override
                            public void onClick(DialogInterface confirmDialog, int id) {
                                   confirmDialog.cancel();
                                   sendLogs();
                               }
                           }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                               @Override
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
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) { 
            Toast.makeText(this, "External media must be mounted to gather and send logs", Toast.LENGTH_LONG).show();
            return;
        }
        
        Toast.makeText(this, "Preparing logs. Email app will appear soon", Toast.LENGTH_SHORT).show();
        
        try {
            final Process p = Runtime.getRuntime().exec(new String[] {"/system/bin/logcat", "-d",  "-v", "threadtime", "NodeDroid:V", "*:S"});
            final Handler mHandler = new Handler();
    
            new Thread(new Runnable() {
                Exception ex = null;
                
                private void dump(String name, InputStream in, ZipOutputStream out) throws IOException {
                    out.putNextEntry(new ZipEntry(name));
                    byte[] buf = new byte[4096];
                    int amt;
                    while ((amt = in.read(buf)) != -1) {
                        out.write(buf, 0, amt);
                    }
                    in.close();
                    out.closeEntry();
                }
                
                
                @Override
                public void run() {
                    
                    try {
                        
                        final File dumpFile = NodeUsage.getDumpFile();
                        dumpFile.getParentFile().mkdirs();
                        Log.d(NodeUsage.TAG, "Dump file is " + dumpFile);
                        
                        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dumpFile));
                        zos.setMethod(ZipOutputStream.DEFLATED);
                        
                        // First, write the log.
                        dump("log.txt", p.getInputStream(), zos);
                        
                        // Now 
                        File[] dumps = getFilesDir().listFiles(new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String filename) {
                                return filename.startsWith("FetcherLog-");
                            }
                        });
                        
                        for (File f: dumps) {
                            dump(f.getName(), new FileInputStream(f), zos);
                            f.delete();
                        }
                        zos.finish();
                        zos.close();
                    
                        mHandler.post(new Runnable() {
        
                            @Override
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
                                    sendIntent.putExtra(Intent.EXTRA_TEXT, "");
                                    sendIntent.setType("application/zip");
                                    sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(dumpFile));
                            
                                    startActivity(Intent.createChooser(sendIntent, "Email:"));
                                }
                            }
                            
                        });
                    } catch (final IOException ex) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(PreferencesActivity.this, "Errror Collecting logs: " + ex, Toast.LENGTH_SHORT).show();
                            }
                        });
                        ex.printStackTrace();
                    }
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
