
package com.eightbitcloud.internode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eightbitcloud.internode.data.Account;
import com.eightbitcloud.internode.data.Service;
import com.eightbitcloud.internode.data.ServiceIdentifier;
import com.eightbitcloud.internode.provider.ProviderFetcher;
import com.eightbitcloud.internode.provider.ServiceUpdateDetails;

/**
 * This service is the component that is reponsible for making network connections.  It is done here so that
 * a change in orientation won't stop downloads.
 * 
 * This used to be a proper service, but it isn't needed.  Instead, it now just is shared between activities...
 * @author bruce
 *
 */
public class DataFetcher  {

//    private NotificationManager mNM;
    // This is the object that receives interactions from clients. See
    // RemoteService for a more complete example.
//    private final IBinder mBinder = new LocalBinder();
    List<AccountUpdateListener> listeners = new ArrayList<AccountUpdateListener>();
    List<Account> accounts = new ArrayList<Account>();
    Handler handler = new Handler();
    SharedPreferences prefs;
    private List<Service> allServices;
    
//    BackgroundSaver backgroundSaver;

    List<AccountUpdater> runningTasks = Collections.synchronizedList(new ArrayList<AccountUpdater>());
    
    
    boolean ignoringPreferenceUpdates = false;
    
//    
//    /**
//     * Class for clients to access. Because we know this service always runs in
//     * the same process as its clients, we don't need to deal with IPC.
//     */
//    public class LocalBinder extends Binder {
//        DataFetchService getService() {
//            return DataFetchService.this;
//        }
//    }
    private Context context;
    
    public DataFetcher(Context ctx) {
        this.context = ctx;
        onCreate();
    }

//    @Override
    public void onCreate() {
//        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        prefs = context.getSharedPreferences(PreferencesSerialiser.PREFS_FILE, Application.MODE_PRIVATE);
//        prefs.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
//            public void onSharedPreferenceChanged(SharedPreferences arg0, String arg1) {
//                if (!ignoringPreferenceUpdates) {
//                    Log.i(NodeUsage.TAG, "Preferences Changed!!!!!.... Updating accounts");
//                    refreshFromPreferences();
//                }
//                // TODO any need to take action? Possibly cancel tasks on accounts that no longer exist
//            }
//        });
        refreshFromPreferences();
//        backgroundSaver = new BackgroundSaver();
//        backgroundSaver.start();
        
    }

//    @Override
//    public void onStart(Intent intent, int startId) {
//        // We want this service to continue running until it is explicitly
//        // stopped, so return sticky.
//        // return START_STICKY;
//        
//    }
    
    public void refreshFromPreferences() {
        allServices = null;
        Log.i(NodeUsage.TAG, "Data Fetcher Reloading");
        PreferencesSerialiser.deserialise(prefs, accounts);
        updateAccounts();
        
    }
    

    
    
    public List<Service> getAllServices() {
        if (allServices == null) {
            allServices = new ArrayList<Service>();
            for (Account account: accounts) {
                allServices.addAll(account.getAllServices());
            }
        }
        return allServices;
    }

    
    
    public void cancelRunningFetches() {
        
        Log.i(NodeUsage.TAG, "Canceling Running fetches.  There are " + runningTasks.size() + " active tasks");
        synchronized (runningTasks) {
            for (AccountUpdater a: runningTasks) {
                a.cancel(true);
            }
            runningTasks.clear();
        }
    }
    
    public void notifyTaskFinished(AccountUpdater t) {
        runningTasks.remove(t);
    }
    
    
    public void shutdown() {
        Log.i(NodeUsage.TAG, "Shutting down Background Fetch Service");
        cancelRunningFetches();

        saveState();

//        backgroundSaver.shutdown();
//        try {
//            // Wait for it to complete, otherwise we may do two saves on top of each other...
//            backgroundSaver.join();
//        } catch (InterruptedException e) {
//        }
//      sp.edit().putString("graphType", graphType.toString()).commit();


        
        // Cancel the persistent notification.
//        mNM.cancel(R.string.local_service_started);

        // Tell the user we stopped.
//        Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
    }
    
    public synchronized void saveState() {
        ignoringPreferenceUpdates = true;
        try {
            Log.i(NodeUsage.TAG, "Saving preferences");
            PreferencesSerialiser.serialise(accounts, prefs);
        } finally {
            ignoringPreferenceUpdates = false;
        }
    }

    
//
//    @Override
//    public IBinder onBind(Intent intent) {
//        return mBinder;
//    }

    /**
     * Show a notification while this service is running.
     */
//    private void showNotification() {
//        // In this sample, we'll use the same text for the ticker and the
//        // expanded notification
//        CharSequence text = getText(R.string.local_service_started);
//
//        // Set the icon, scrolling text and timestamp
//        Notification notification = new Notification(R.drawable.nodedroid, text, System.currentTimeMillis());
//
//        // The PendingIntent to launch our activity if the user selects this
//        // notification
//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, NodeUsage.class), 0);
//
//        // Set the info for the views that show in the notification panel.
//        notification.setLatestEventInfo(this, getText(R.string.local_service_label), text, contentIntent);
//
//        // Send the notification.
//        // We use a layout id because it is a unique number. We use it later to
//        // cancel.
//        mNM.notify(R.string.local_service_started, notification);
//    }

    public void registerCallback(AccountUpdateListener accountUpdateListener) {
        listeners.add(accountUpdateListener);
    }

    public void deregisterCallback(AccountUpdateListener accountUpdateListener) {
        listeners.remove(accountUpdateListener);
    }
    
    public void updateAccounts() {
        
        Log.i(NodeUsage.TAG, "Updating Services");
        cancelRunningFetches();
        for (Account account: accounts) {
            AccountUpdater u = new AccountUpdater(account);
            runningTasks.add(u);
            u.execute(account);
        }
    }
    
    private class AccountUpdater extends AsyncTask<Account, Notification, Void> {
        private Account account;
        private ProviderFetcher fetcher;

        public AccountUpdater(Account acct) {
            this.account = acct;
        }

        @Override
        protected Void doInBackground(Account... params) {
            fetcher = account.getProvider().createFetcher(context);
            try {
                boolean extraLogging = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("performExtraLogging", false);
                fetcher.setLogTraffic(extraLogging);
                
                try {
                    publishProgress(new Notification(Event.START_ACCOUNT_UPDATE, account));
                    List<ServiceUpdateDetails> results;
                    results = fetcher.fetchAccountUpdates(account);
                    publishProgress(new Notification(Event.COMPLETE_ACCOUNT_UPDATE, results));

                
                    // Now we update the usage for each service.
                    for (Service service : account.getAllServices()) {
                        if (!isCancelled()) {
                            try {
                                publishProgress(new Notification(Event.START_SERVICE_USAGE_UPDATE, service));
                                Service clone = service.createUpdateClone();
                                fetcher.fetchServiceDetails(clone);
                                publishProgress(new Notification(Event.COMPLETE_SERVICE_USAGE_UPDATE, new Service[] {service, clone}));
                            } catch (InterruptedException e) {
                                Log.e(NodeUsage.TAG, "Interrupted while updating Service: ", e);
                                publishProgress(new Notification(Event.ERROR_IN_SERVICE_USAGE_UPDATE, service, e));
                            } catch (Exception e) {
                                Log.e(NodeUsage.TAG, "Error updating Service: ", e);
                                publishProgress(new Notification(Event.ERROR_IN_SERVICE_USAGE_UPDATE, service, e));
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Log.w(NodeUsage.TAG, "Interrupted fetching account " + account);
                    publishProgress(new Notification(Event.ERROR_IN_ACCOUNT_UPDATE, account, e));
                } catch (Exception e) {
                    Log.e(NodeUsage.TAG, "Error updating Accounts: ", e);
                    publishProgress(new Notification(Event.ERROR_IN_ACCOUNT_UPDATE, account, e));
                }
                
                
            } finally {
                fetcher.cleanup();
            }
            return null;
        }
        
        @SuppressWarnings("unchecked")
        @Override
        protected void onProgressUpdate(Notification...details) {
            for (Notification n: details) {
                switch (n.evt) {
                case START_ACCOUNT_UPDATE:
                    Account account = (Account) n.obj;
                    for (AccountUpdateListener l : listeners) {
                        l.startingServiceFetchForAccount(account);
                    }
                    break;
                case COMPLETE_ACCOUNT_UPDATE:
                    notifyNewServiceList((List<ServiceUpdateDetails>) n.obj);
                    break;
                case ERROR_IN_ACCOUNT_UPDATE:
                    for (AccountUpdateListener l : listeners) {
                        l.errorUpdatingServices((Account) n.obj, n.ex);
                    }
                    break;
                    
                case START_SERVICE_USAGE_UPDATE:
                    for (AccountUpdateListener l: listeners) {
                        l.serviceUpdated((Service) n.obj);
                    }
                    break;
                case COMPLETE_SERVICE_USAGE_UPDATE:
                    Service[] x = (Service[]) n.obj;
                    updateService(x[0], x[1]);
                    break;
                case ERROR_IN_SERVICE_USAGE_UPDATE:
                    for (AccountUpdateListener l : listeners) {
                        l.errorUpdatingService((Service) n.obj, n.ex);
                    }
                    break;
                }
            }
        }
        
        
        
        private void notifyNewServiceList(List<ServiceUpdateDetails> details) {
            Set<Service> oldServices = new HashSet<Service>(account.getAllServices());
            for (ServiceUpdateDetails u: details) {
                ServiceIdentifier accountNumber = u.getIdentifier();
            
                Service service = account.getService(accountNumber);
                if (service == null) {
                    service = new Service();
                    service.setIdentifier(accountNumber);
                    account.addService(service);
                }
                service.addProperties(u.getProperties()); //TODO Should we clear existing ones first
                
                if (u.getPlan() != null) {
                    service.setPlan(u.getPlan());
                }
                
                oldServices.remove(service);
            }
            for (Service service: oldServices) {
                account.removeService(service);
            }
            
            allServices = null;
            for (AccountUpdateListener l : listeners) {
                l.fetchedServiceNamesForAccount(account);
            }
            
            // Notify all the services that we are staring the update.
            for (Service service : account.getAllServices()) {
                
                for (AccountUpdateListener l : listeners) {
                    l.serviceUpdateStarted(service);
                }
            }

        }

        protected void updateService(Service service, Service serviceWithUpdates) {
            service.updateFrom(serviceWithUpdates);
            service.setLastUpdate(new Date());

            // Update From cloned service.
            for (AccountUpdateListener l : listeners) {
                l.serviceUpdated(service);
            }
        }
    }
    
    public static enum Event { START_ACCOUNT_UPDATE, COMPLETE_ACCOUNT_UPDATE, ERROR_IN_ACCOUNT_UPDATE, START_SERVICE_USAGE_UPDATE, COMPLETE_SERVICE_USAGE_UPDATE, ERROR_IN_SERVICE_USAGE_UPDATE };
    public static class Notification {
        Event evt;
        Object obj;
        Exception ex;
        
        public Notification(Event evt, Object obj) {
            this(evt, obj, null);
        }
        
        public Notification(Event evt, Object obj, Exception ex) {
            this.evt = evt;
            this.obj = obj;
            this.ex = ex;
        }
    }
    
    
}
