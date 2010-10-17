package com.eightbitcloud.internode;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.admob.android.ads.AdManager;
import com.admob.android.ads.AdView;
import com.admob.android.ads.SimpleAdListener;
import com.eightbitcloud.internode.data.Account;
import com.eightbitcloud.internode.data.ProviderStore;
import com.eightbitcloud.internode.data.Service;

public class NodeUsage extends Activity implements AccountUpdateListener {
    public static final String TAG = "NodeDroid";
    
    Handler handler;
    private int activeNetConnections;
    
    Typeface internodeFont;
    
    Map<String, ServiceView> serviceViews = new HashMap<String, ServiceView>();

    private PagingScrollView scroller;

    private View loadingServicesView;
    private KeyStore trustStore;
    
    // LocalService test stuff
    private DataFetchService dataFetcher;
    private boolean mIsBound;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG, "Bound to service. Have we checked accounts yet? " + checkedAccounts);
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            dataFetcher = ((DataFetchService.LocalBinder)service).getService();
            // We want to monitor the service for as long as we are
            // connected to it.
            
            dataFetcher.registerCallback(NodeUsage.this);
            
            checkAccounts(); 
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            dataFetcher = null;
        }
    };

    private boolean checkedAccounts;

    


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        internodeFont = Typeface.createFromAsset(getAssets(), "Arial Rounded Bold.ttf");
        handler = new Handler();
        

        
        try {
            
            // Set up our own KeyStore with the GeoTrust CA in it. This is
            // needed because Android does not have it in its keystore by
            // default. Be aware that this means that this program will now ONLY
            // accept GeoTrust certificates, so if Internode issues another one
            // this program will break. TODO Create an all-inclusive trust
            // store.
            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream instream = getAssets().open("my.bks");
            try {
                trustStore.load(instream, "changeit".toCharArray());
            } finally {
                instream.close();
            }
            ProviderStore.getInstance().getProvider("internode").setProperty("keyStore", trustStore);
            

        } catch (Exception ex) {
            // TODO this is really bad.
        }

        
        AdManager.setTestDevices( new String[] {                 
                AdManager.TEST_EMULATOR             // Android emulator
        } );  

        
        AdView ad = (AdView) findViewById(R.id.ad);
        ad.setAdListener(new SimpleAdListener() {

            /* (non-Javadoc)
             * @see com.admob.android.ads.AdView.SimpleAdListener#onFailedToReceiveAd(com.admob.android.ads.AdView)
             */
            @Override
            public void onFailedToReceiveAd(AdView adView)
            {
                super.onFailedToReceiveAd(adView);
                Log.i(TAG, "Failed to receive Ad");
            }

            /* (non-Javadoc)
             * @see com.admob.android.ads.AdView.SimpleAdListener#onFailedToReceiveRefreshedAd(com.admob.android.ads.AdView)
             */
            @Override
            public void onFailedToReceiveRefreshedAd(AdView adView)
            {
                super.onFailedToReceiveRefreshedAd(adView);
                Log.i(TAG, "Failed to receive refreshed Ad");
            }

            /* (non-Javadoc)
             * @see com.admob.android.ads.AdView.SimpleAdListener#onReceiveAd(com.admob.android.ads.AdView)
             */
            @Override
            public void onReceiveAd(AdView adView)
            {
                super.onReceiveAd(adView);
                Log.i(TAG, "Received Ad");
            }

            /* (non-Javadoc)
             * @see com.admob.android.ads.AdView.SimpleAdListener#onReceiveRefreshedAd(com.admob.android.ads.AdView)
             */
            @Override
            public void onReceiveRefreshedAd(AdView adView)
            {
                super.onReceiveRefreshedAd(adView);
                Log.i(TAG, "Received Refreshed Ad");
            }
        });
        
//        flipper = (ViewFlipper) findViewById(R.id.serviceFlipper);
        scroller = ((PagingScrollView)findViewById(R.id.scrollView));
        
        
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(NodeUsage.this, DataFetchService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;

        
    }
    
    public View getLoadingServicesView() {
        if (loadingServicesView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) NodeUsage.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            loadingServicesView = layoutInflater.inflate(R.layout.loadingservices, null);
        }
        return loadingServicesView;
    }

    
    
    public void addServiceView(ServiceView v) {
        scroller.addPage(v);
        if (scroller.hasPage(getLoadingServicesView())) {
            scroller.removePage(getLoadingServicesView());
        }
    }
    
    public ServiceView getViewForService(Service service) {
        ServiceView view = serviceViews.get(service.getIdentifier());
        if (view == null) {
            view = new ServiceView(this, null, internodeFont);
            view.setService(service);
            addServiceView(view);
            serviceViews.put(service.getIdentifier(), view);
        }
        return view;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        
        Log.d(TAG, "Starting. Do we have a dataFetcher? " + (dataFetcher != null));
        checkAccounts(); 
    }
    
    
    public void checkAccounts() {
        if (!checkedAccounts && dataFetcher != null) {
           
            Log.i(TAG, "Checking accounts.  Data Fetcher reports there are " + dataFetcher.accounts.size() + " accounts");
            
            scroller.removeAllPages();
            serviceViews.clear();
            if (dataFetcher.accounts.isEmpty()) {
                LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View noaccountsView = layoutInflater.inflate(R.layout.noaccounts, null);
                Button addButton = (Button) noaccountsView.findViewById(R.id.addaccountbutton);
                addButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        addFirstAccount(v);
                    }
                });
                
                scroller.addPage(noaccountsView);
            } else {
                int countServices = 0;
                for (Account acct: dataFetcher.accounts) {
                    for (Service service: acct.getAllServices()) {
                        getViewForService(service);
                        countServices++;
                    }
                }
                if (countServices == 0) {
                    scroller.addPage(getLoadingServicesView());
                }
            }
    
            dataFetcher.updateAccounts();
            checkedAccounts = true;
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "Pausing");
    }
    
    /**
     * Called when the user clicks on the "Add Account" button on the "you have no accounts" view
     * @param sourcE
     */
    public void addFirstAccount(View sourcE) {
        Intent intent = new Intent(NodeUsage.this, AccountListActivity.class).setAction(Intent.ACTION_INSERT);
        startActivityForResult(intent, 0);
    }
    
    
    
    public synchronized void incrementActiveNetConnections(int amt) {
        boolean runningBefore = activeNetConnections > 0;
        
        activeNetConnections += amt;
        boolean runningAfterwards = activeNetConnections > 0;
//        Log.d(TAG, "Active net connections is now " + activeNetConnections + ", before = " + runningBefore + ", after = " + runningAfterwards);


        if (runningBefore ^ runningAfterwards) {
//            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress);
            if (runningBefore) {
                // We're turing it off
//                Log.d(TAG, "Making Progress bar invisible");
//                progressBar.setVisibility(View.INVISIBLE);
            } else {
                // We're turning it on.
//                Log.d(TAG, "Making Progress bar visible");
//                progressBar.setVisibility(View.VISIBLE);
            }
        }
    }
    
    public void prevPage(View source) {
//        flipper.showPrevious();
    }
    public void nextPage(View source) {
//        flipper.showNext();
    }

    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "Finished fiddling with accounts. Request Code is " + requestCode +", result is " + resultCode);
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // For "Title only": Examples of matching an ID with one assigned in
            //                   the XML
            case R.id.settingsMenuItem:
                Intent intent = new Intent(this, AccountListActivity.class).setAction(Intent.ACTION_VIEW);
                startActivityForResult(intent, 0);
                return true;
            case R.id.refreshMenuItem:
                // TODO cancel existing refresh first
                Toast.makeText(this, "Refreshing Usage", Toast.LENGTH_SHORT).show();
                dataFetcher.updateAccounts();
                return true;
        }
        return false;
    }

    

    public void reportFatalException(Exception ex) {
        Toast t = Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_LONG);
        t.show();
    }
    

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            dataFetcher.deregisterCallback(this);
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    
    /* Messages from the data fetcher service */ 
    
    public void accountLoadStarted(Account account) {
        Log.i(TAG, "Account " + account + " loading services");
        // TODO Auto-generated method stub
        
    }

    public void accountLoadCompletedSuccessfully(Account account) {
        Log.i(TAG, "Account " + account + " completed loading services");
        
    }

    public void errorUpdatingAccounts(Account account, Exception ex) {
        Log.e(TAG, "Account " + account + " got error while updating services", ex);
        reportFatalException(ex);
    }

    public void serviceLoadStarted(Service service) {
        Log.i(TAG, "Service " + service + " beginning update");
        getViewForService(service).setLoading(true);
        
    }

    public void serviceUpdatedCompletedSuccessfully(Service service) {
        Log.i(TAG, "Service " + service + " finished updating");
        ServiceView sv = getViewForService(service); 
        sv.setService(service);
        sv.setLoading(false);
    }

    public void errorUpdatingService(Service service, Exception ex) {
        Log.e(TAG, "Service " + service + " had error while refreshing", ex);
        ServiceView sv = getViewForService(service); 
        sv.setService(service);
        sv.setLoading(false);
        reportFatalException(ex);
    }

    

}
