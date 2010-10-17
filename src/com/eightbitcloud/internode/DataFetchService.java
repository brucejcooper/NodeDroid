package com.eightbitcloud.internode;

import java.util.ArrayList;
import java.util.List;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import com.eightbitcloud.internode.data.Account;
import com.eightbitcloud.internode.data.Service;
import com.eightbitcloud.internode.provider.ProviderFetcher;

/**
 * This service is the component that is reponsible for making network connections.  It is done here so that
 * a change in orientation won't stop downloads.
 * 
 * It also acts as a data store for the Accounts, sharable between components.
 * @author bruce
 *
 */
public class DataFetchService extends android.app.Service {

//    private NotificationManager mNM;
    // This is the object that receives interactions from clients. See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
    List<AccountUpdateListener> listeners = new ArrayList<AccountUpdateListener>();
    List<Account> accounts = new ArrayList<Account>();
    Handler handler = new Handler();
    SharedPreferences prefs;
    private List<Thread> runningThreads = new ArrayList<Thread>();
    
    /**
     * Class for clients to access. Because we know this service always runs in
     * the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        DataFetchService getService() {
            return DataFetchService.this;
        }
    }

    @Override
    public void onCreate() {
//        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        prefs = getApplication().getSharedPreferences(PreferencesSerialiser.PREFS_FILE, Application.MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences arg0, String arg1) {
                PreferencesSerialiser.deserialise(prefs, accounts);
                // TODO any need to take action? Possibly cancel tasks on accounts that no longer exist
            }
        });
        PreferencesSerialiser.deserialise(prefs, accounts);
//        showNotification();
    }

//    @Override
//    public void onStart(Intent intent, int startId) {
//        // We want this service to continue running until it is explicitly
//        // stopped, so return sticky.
//        // return START_STICKY;
//        
//    }

    
    private void startFetch(Runnable r) {
        Thread t = new Thread(r);
        runningThreads.add(t);
        t.start();
    }
    
    public void cancelRunningFetches() {
        for (Thread t: runningThreads) {
            t.interrupt();
        }
    }
    

    @Override
    public void onDestroy() {
//      sp.edit().putString("graphType", graphType.toString()).commit();
        PreferencesSerialiser.serialise(accounts, prefs);
        
        // Cancel the persistent notification.
//        mNM.cancel(R.string.local_service_started);

        // Tell the user we stopped.
//        Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

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
        cancelRunningFetches();
        for (Account account: accounts) {
            startFetch(new ServiceFetcher(account));
        }
    }
    
    private abstract class UpdateTask<T> implements Runnable {
        protected T target;
        
        public UpdateTask(T target) {
            this.target = target;
        }
        
//        public T getTarget() {
//            return target;
//        }
        
        public void run() {
            try {
                handler.post(new Runnable() {
                    public void run() {
                        before();
                    }
                });
                execute();
                handler.post(new Runnable() {
                    public void run() {
                        after();
                    }
                });
            } catch (final Exception ex) {
                error(ex);
            }
        }
        
        public abstract void before();
        public abstract void execute() throws Exception;
        public abstract void after();
        public abstract void error(Exception ex);
    }

    private class ServiceFetcher extends UpdateTask<Account> {
        private ProviderFetcher fetcher;

        public ServiceFetcher(Account account) {
            super(account);
        }

        @Override
        public void before() {
            for (AccountUpdateListener l : listeners) {
                l.accountLoadStarted(target);
            }
        }

        @Override
        public void execute() throws Exception {
            fetcher = target.getProvider().createFetcher();
            fetcher.fetchServices(target);
        }

        @Override
        public void after() {
            for (AccountUpdateListener l : listeners) {
                l.accountLoadCompletedSuccessfully(target);
            }
            
            for (Service service : target.getAllServices()) {
                startFetch(new ServiceUpdaterTask(service, fetcher));
            }
        }

        @Override
        public void error(Exception ex) {
            for (AccountUpdateListener l : listeners) {
                l.errorUpdatingAccounts(target, ex);
            }
        }
    }

    private class ServiceUpdaterTask extends UpdateTask<Service> {
        private ProviderFetcher fetcher;

        public ServiceUpdaterTask(Service service, ProviderFetcher fetcher) {
            super(service);
            this.fetcher = fetcher;
        }

        @Override
        public void before() {
            for (AccountUpdateListener l : listeners) {
                l.serviceLoadStarted(target);
            }
        }

        @Override
        public void execute() throws Exception {
            fetcher.updateService(target);
        }

        @Override
        public void after() {
            for (AccountUpdateListener l : listeners) {
                l.serviceUpdatedCompletedSuccessfully(target);
            }
        }

        @Override
        public void error(Exception ex) {
            for (AccountUpdateListener l : listeners) {
                l.errorUpdatingService(target, ex);
            }
        }
    }
}
