package com.eightbitcloud.internode;

import java.io.File;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.zip.Inflater;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;
import android.widget.Toast;

import com.eightbitcloud.internode.data.Account;
import com.eightbitcloud.internode.data.AccountProvider;
import com.eightbitcloud.internode.data.ProviderStore;
import com.eightbitcloud.internode.data.Service;

public class NodeUsage extends Activity {
    public static final String TAG = "NodeDroid";
    private static final int FIRST_ACCOUNT_ID = 1;
    private Typeface internodeFont;
    private PagingScrollView scroller;
    private View loadingServicesView;
    private KeyStore trustStore;
    private ListView landscapeList;
    private Cursor servicesCursor;
    private Handler handler = new Handler();
    private Runnable updateLastUpdatedHanlder = new Runnable() {
        @Override
        public void run() {
            updateLastUpdatedLabels();
        }
    };
    


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        internodeFont = Typeface.createFromAsset(getAssets(), "Arial Rounded Bold.ttf");
        
        try {
            // Set up our own KeyStore with the GeoTrust CA in it. This is
            // needed because Android does not have it in its keystore by
            // default. Be aware that this means that this program will now ONLY
            // accept GeoTrust certificates, so if Internode issues another one
            // this program will break. 
            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream instream = getAssets().open("my.bks");
            try {
                trustStore.load(instream, "changeit".toCharArray());
            } finally {
                instream.close();
            }
            ProviderStore.getInstance().getProvider("internode").setProperty("keyStore", trustStore);
        } catch (Exception ex) {
            // this is really bad, but should never happen
        }
        
        servicesCursor = getContentResolver().query(AccountProvider.SERVICES_CONTENT_URI, null, null, null, null);

        setContentView(R.layout.main);
        
        scroller = ((PagingScrollView)findViewById(R.id.scrollView));
        if (scroller != null) { // Portrait
            View loadingView = ((LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.loadingservices, null);
            scroller.setNoViewsView(loadingView);

            
            CursorAdapter adapter = new CursorAdapter(this, servicesCursor) {
                @Override
                public void bindView(View view, Context context, Cursor cursor) {
                    ServiceView sv = (ServiceView) view;
                    Service service = new Service(cursor);
                    
                    sv.setService(service);
                }

                @Override
                public View newView(Context context, Cursor cursor, ViewGroup parent) {
                    View v = new ServiceView(NodeUsage.this, null, internodeFont);

                    return v;
                }
            };
            scroller.setAdapter(adapter);
            
            
        } else { // Landscape
            landscapeList = (ListView) findViewById(R.id.landscroller);
            SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.quotalandline, servicesCursor,
                    new String[] { Service.SERVICE_PROVIDER, Service.ACCOUNT_ID, Service.SERVICE_ID, Service.DATA }, 
                    new int[] {R.id.provider, R.id.accountname, R.id.serviceid, R.id.innerlist}
            );
            adapter.setViewBinder(new ViewBinder() {
                @Override
                public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                    if (view instanceof LinearLayout) {
                        // CHeats way of telling that its the list :)
                        Service srv = new Service(cursor);
                        
                        LinearLayout innerlist = (LinearLayout) view;
                        innerlist.removeAllViews();
                        
                        MetricGroupListAdapter ad = new MetricGroupListAdapter(NodeUsage.this, srv, internodeFont);
                        for (int i = 0; i < ad.getCount(); i++) {
                            View v = ad.getView(i, null, null);
                            innerlist.addView(v);
                        }
                        return true;
                    }
                    return false;
                }
            });
            landscapeList.setAdapter(adapter);
        }

        // Show a special activity, if no accounts exist.
        Cursor accountCursor = getContentResolver().query(AccountProvider.ACCOUNTS_CONTENT_URI, new String[] {Account.ID}, null, null, null);
        try {
            if (accountCursor.getCount() == 0) {
                startActivityForResult(new Intent(this, NoAccountsActivity.class), FIRST_ACCOUNT_ID);
            } else {
                updateUsage();
            }
        } finally {
            accountCursor.close();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        updateLastUpdatedLabels();
        handler.postDelayed(updateLastUpdatedHanlder , 30*1000);
    }
    
    @Override
    protected void onPause() {
        handler.removeCallbacks(updateLastUpdatedHanlder);
        super.onPause();
    }
    
    
    protected void updateLastUpdatedLabels() {
        Log.d(TAG, "Updating last updated labels.  Scroller is " + scroller);
        if (scroller != null) {
            for (View v: scroller.getPages()) {
                if (v instanceof ServiceView) {
                    ((ServiceView)v).refreshLastUpdated();
                }
            }
        }
        handler.postDelayed(updateLastUpdatedHanlder, 30*1000);
        
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // If they canceled the "New Account", then just give up...
        if (requestCode == FIRST_ACCOUNT_ID  && resultCode == Activity.RESULT_CANCELED) {
            finish();
        }
    }
    

    public View getLoadingServicesView() {
        if (loadingServicesView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) NodeUsage.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            loadingServicesView = layoutInflater.inflate(R.layout.loadingservices, null);
        }
        return loadingServicesView;
    }

    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.accountsMenuItem:
                Intent intent = new Intent(this, AccountListActivity.class).setAction(Intent.ACTION_VIEW);
                startActivityForResult(intent, 0);
                return true;
            case R.id.refreshMenuItem:
                Toast.makeText(this, "Refreshing Usage", Toast.LENGTH_SHORT).show();
                updateUsage();
                return true;
            case R.id.settingsMenuItem:
                intent = new Intent(this, PreferencesActivity.class).setAction(Intent.ACTION_VIEW);
                startActivityForResult(intent, 0);
                return true;
        }
        return false;
    }


    private void updateUsage() {
        startService(new Intent(this, UsageUpdateService.class));
    }
    
    public static File getDumpFile() {
        return new File(Environment.getExternalStorageDirectory(), ".nodedroid/NodeDroidDump.zip");
    }

    @Override
    protected void onDestroy() {
        servicesCursor.close();
        
        if (isFinishing()) {
            File dumpFile = getDumpFile();
            if (dumpFile.exists()) {
                dumpFile.delete();
            }
        }
        
        super.onDestroy();
    }


}