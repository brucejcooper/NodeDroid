package com.eightbitcloud.internode;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
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
    private DataFetcher dataFetcher;
    
    private ListView landscroller;

    private LayoutInflater inflater;
    
    
//    private boolean mIsBound;

//    private ServiceConnection mConnection = new ServiceConnection() {
//        public void onServiceConnected(ComponentName className, IBinder service) {
//            Log.i(TAG, "Bound to service. Have we checked accounts yet? " + checkedAccounts);
//            // This is called when the connection with the service has been
//            // established, giving us the service object we can use to
//            // interact with the service.  Because we have bound to a explicit
//            // service that we know is running in our own process, we can
//            // cast its IBinder to a concrete class and directly access it.
//            dataFetcher = ((DataFetchService.LocalBinder)service).getService();
//            // We want to monitor the service for as long as we are
//            // connected to it.
//            
//            dataFetcher.registerCallback(NodeUsage.this);
//            
//            checkAccounts(); 
//        }
//
//        public void onServiceDisconnected(ComponentName className) {
//            // This is called when the connection with the service has been
//            // unexpectedly disconnected -- that is, its process crashed.
//            // Because it is running in our same process, we should never
//            // see this happen.
//            dataFetcher = null;
//        }
//    };
//
//    private boolean checkedAccounts;

    


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inflater = LayoutInflater.from(this);

        setContentView(R.layout.main);
        if (isDisplayPortrait()) {
            scroller = ((PagingScrollView)findViewById(R.id.scrollView));
        } else {
            landscroller = (ListView) findViewById(R.id.landscroller);
            
            landscroller.setAdapter(new BaseAdapter() {
                public int getCount() {
                    return dataFetcher == null ? 0 : dataFetcher.getAllServices().size();
                }

                public Service getItem(int position) {
                    return dataFetcher.getAllServices().get(position);
                }

                public long getItemId(int position) {
                    return position;
                }

                public View getView(int position, View convertView, ViewGroup parent) {
                    Service item = getItem(position);
                    LandViewHolder holder;
                    
                    if (convertView == null) {
                        convertView = inflater.inflate(R.layout.quotalandline, null);
                        
                        LinearLayout list = (LinearLayout) convertView.findViewById(R.id.innerlist);
                        holder = new LandViewHolder(
                                (TextView) convertView.findViewById(R.id.provider),
                                (TextView) convertView.findViewById(R.id.accountname),
                                (TextView) convertView.findViewById(R.id.serviceid),
                                list, 
                                new MetricGroupListAdapter(NodeUsage.this, item, internodeFont)
                        );
                        holder.accountType.setTypeface(internodeFont);
                        convertView.setTag(holder);

                        convertView.setLayoutParams(new ListView.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
                    } else {
                        holder = (LandViewHolder) convertView.getTag();
                    }
                    holder.accountType.setText(item.getAccount().getProvider().getName());
                    holder.name.setText(item.getAccount().getUsername());
                    holder.serviceID.setText(item.getIdentifier());
                    

                    holder.adapter.setService(item);
                    
                    // Cheat by using the list adapter to create views that we add directly to a linear layout
                    holder.innerlist.removeAllViews();
                    for (int i = 0; i < holder.adapter.getCount(); i++) {
                        View v = holder.adapter.getView(i, null, null);
                        holder.innerlist.addView(v);
                    }
                    
                    return convertView;
                }
                
            });

        }

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
        
        
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
     //   bindService(new Intent(NodeUsage.this, DataFetchService.class), mConnection, Context.BIND_AUTO_CREATE);
      //  mIsBound = true;
        dataFetcher = (DataFetcher) getLastNonConfigurationInstance();
        if (dataFetcher == null) {
            dataFetcher = new DataFetcher(this);
        }
        dataFetcher.registerCallback(this);
    }
    

    private void updateLandscapeView() {
        if (landscroller != null) 
            ((BaseAdapter)landscroller.getAdapter()).notifyDataSetChanged();
    }
    
    
    /**
     * This is used to keep the dataFetcher around when rotation is performed, so that we don't
     * loose time restarting things in the middle.  
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        return dataFetcher;
    }

    
    public View getLoadingServicesView() {
        if (loadingServicesView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) NodeUsage.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            loadingServicesView = layoutInflater.inflate(R.layout.loadingservices, null);
        }
        return loadingServicesView;
    }

    public boolean isDisplayPortrait() {
        Configuration config = getResources().getConfiguration();
        int orientation = config.orientation;

        if (orientation == Configuration.ORIENTATION_UNDEFINED) {
            Display getOrient = getWindowManager().getDefaultDisplay();
            // if height and widht of screen are equal then
            // it is square orientation
            if (getOrient.getWidth() == getOrient.getHeight()) {
                orientation = Configuration.ORIENTATION_SQUARE;
            } else { // if widht is less than height than it is portrait
                if (getOrient.getWidth() < getOrient.getHeight()) {
                    orientation = Configuration.ORIENTATION_PORTRAIT;
                } else { // if it is not any of the above it will defineitly
                         // be landscape
                    orientation = Configuration.ORIENTATION_LANDSCAPE;
                }
            }
        }
        return orientation == Configuration.ORIENTATION_PORTRAIT;
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
        
                // If we don't deregister the callbacklistener here, it will leak...
        if (isDisplayPortrait()) {
            updatePortraitAccountsView();
        }
    }
    
    public void updatePortraitAccountsView() {
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
        dataFetcher.refreshFromPreferences();
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // For "Title only": Examples of matching an ID with one assigned in
            //                   the XML
            case R.id.settingsMenuItem:
                Intent intent = new Intent(this, AccountListActivity.class).setAction(Intent.ACTION_VIEW);
                dataFetcher.cancelRunningFetches();
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
    

//    void doUnbindService() {
//        if (mIsBound) {
//            // Detach our existing connection.
//            if (dataFetcher != null) {
//                dataFetcher.deregisterCallback(this);
//            }
//            unbindService(mConnection);
//            mIsBound = false;
//        }
//    }

    @Override
    protected void onDestroy() {
        dataFetcher.deregisterCallback(this);
        super.onDestroy();
//        doUnbindService();
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
        updateLandscapeView();
    }

    public void serviceLoadStarted(Service service) {
        Log.i(TAG, "Service " + service + " beginning update");
        if (isDisplayPortrait()) {
            getViewForService(service).setLoading(true);
        } else {
            updateLandscapeView();
        }
        
    }

    public void serviceUpdatedCompletedSuccessfully(Service service) {
        Log.i(TAG, "Service " + service + " finished updating");
        if (isDisplayPortrait()) {
            ServiceView sv = getViewForService(service); 
            sv.setService(service);
            sv.setLoading(false);
        } else {
            updateLandscapeView();
        }
    }

    public void errorUpdatingService(Service service, Exception ex) {
        Log.e(TAG, "Service " + service + " had error while refreshing", ex);
        if (isDisplayPortrait()) {
            ServiceView sv = getViewForService(service); 
            sv.setService(service);
            sv.setLoading(false);
        } else {
            updateLandscapeView();
        }
        reportFatalException(ex);
    }

    

}



class LandViewHolder {
    TextView accountType;
    TextView name;
    TextView serviceID;
    LinearLayout innerlist;
    MetricGroupListAdapter adapter;
    
    public LandViewHolder(TextView accountType, TextView name, TextView serviceID, LinearLayout listView, MetricGroupListAdapter metricGroupListAdapter) {
        this.accountType = accountType;
        this.name = name;
        this.serviceID = serviceID;
        this.innerlist = listView;
        this.adapter = metricGroupListAdapter;
    }
}

