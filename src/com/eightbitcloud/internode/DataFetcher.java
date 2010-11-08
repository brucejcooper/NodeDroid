
package com.eightbitcloud.internode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eightbitcloud.internode.data.Account;
import com.eightbitcloud.internode.data.Service;
import com.eightbitcloud.internode.data.ServiceIdentifier;
import com.eightbitcloud.internode.provider.AccountUpdateException;
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

    ThreadPoolExecutor threadRunner = new ThreadPoolExecutor(0, 5, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    
    
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

    
    private void startFetch(Runnable r) {
        threadRunner.execute(new FutureTask<Void>(r, null));
    }
    
    public void cancelRunningFetches() {
        for (Runnable r: threadRunner.getQueue()) {
            @SuppressWarnings("unchecked")
            FutureTask<Void> ft = (FutureTask<Void>) r;
            ft.cancel(true);
        }
        threadRunner.purge();
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
            startFetch(new ServiceFetcher(account));
        }
    }
    
    private abstract class UpdateTask<T,Y> implements Runnable {
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
                final Y val = execute();
//                backgroundSaver.scheduleSave();
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            after(val);
                        } catch (final Exception ex) {
                            handler.post(new Runnable() {
                                public void run() {
                                    error(ex);
                                }
                            });
                        }

                    }
                });
            } catch (final InterruptedException ex) {
                // Thats okay.  Its fine even!!!
                // TODO does this need to notify that it was cancelled, therefore turning off downloading notifications and the like?
            } catch (final Exception ex) {
                handler.post(new Runnable() {
                    public void run() {
                        error(ex);
                    }
                });
            }
        }
        
        public abstract void before();
        public abstract Y execute() throws Exception;
        public abstract void after(Y val) throws AccountUpdateException;
        public abstract void error(Exception ex);
    }

    private class ServiceFetcher extends UpdateTask<Account, List<ServiceUpdateDetails>> {
        private ProviderFetcher fetcher;

        public ServiceFetcher(Account account) {
            super(account);
        }

        @Override
        public void before() {
            for (AccountUpdateListener l : listeners) {
                l.startingServiceFetchForAccount(target);
            }
        }

        @Override
        public List<ServiceUpdateDetails> execute() throws Exception {
            fetcher = target.getProvider().createFetcher();
            boolean extraLogging = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("performExtraLogging", false);
            fetcher.setLogTraffic(extraLogging);
            return fetcher.fetchAccountUpdates(target);
        }

        @Override
        public void after(List<ServiceUpdateDetails> details) throws AccountUpdateException {
            
            Set<Service> oldServices = new HashSet<Service>(target.getAllServices());
            for (ServiceUpdateDetails u: details) {
                ServiceIdentifier accountNumber = u.getIdentifier();
            
                Service service = target.getService(accountNumber);
                if (service == null) {
                    service = new Service();
                    service.setIdentifier(accountNumber);
                    target.addService(service);
                }
                service.addProperties(u.getProperties()); //TODO Should we clear existing ones first
                
                if (u.getPlan() != null) {
                    service.setPlan(u.getPlan());
                }
                
                oldServices.remove(service);
            }
            for (Service service: oldServices) {
                target.removeService(service);
            }
            
            allServices = null;
            for (AccountUpdateListener l : listeners) {
                l.fetchedServiceNamesForAccount(target);
            }
            
            for (Service service : target.getAllServices()) {
                startFetch(new ServiceUpdaterTask(service, fetcher));
            }
        }

        @Override
        public void error(Exception ex) {
            allServices = null;
            for (AccountUpdateListener l : listeners) {
                l.errorUpdatingServices(target, ex);
            }
        }
    }

    private class ServiceUpdaterTask extends UpdateTask<Service, Service> {
        private ProviderFetcher fetcher;

        public ServiceUpdaterTask(Service service, ProviderFetcher fetcher) {
            super(service);
            this.fetcher = fetcher;
        }

        @Override
        public void before() {
            for (AccountUpdateListener l : listeners) {
                l.serviceUpdateStarted(target);
            }
        }

        @Override
        public Service execute() throws Exception {
            Service clone = target.createUpdateClone();
            fetcher.fetchServiceDetails(clone);
            return clone;
        }

        @Override
        public void after(Service serviceWithUpdates) {
            // Apply the service changes
            target.updateFrom(serviceWithUpdates);
            allServices = null;
            for (AccountUpdateListener l : listeners) {
                l.serviceUpdated(target);
            }
        }

        @Override
        public void error(Exception ex) {
            allServices = null;
            for (AccountUpdateListener l : listeners) {
                l.errorUpdatingService(target, ex);
            }
        }
    }
    
    
//    public class BackgroundSaver extends Thread {
//        boolean running = true;
//        long scheduledSaveTime = -1;
//        
//        public synchronized void scheduleSave() {
////            Log.i(NodeUsage.TAG, "Scheduling Saving preferences");
//            // Give it 5 seconds... It seems like a long time, but remember
//            // that saving will happen anyway.
//            this.scheduledSaveTime = System.currentTimeMillis() + 5000;
//            notify();
//        }
//        
//        public synchronized void shutdown() {
//            this.running = false;
//            notify();
//        }
//
//        @Override
//        public synchronized void run() {
//            while (running) {
//                long now = System.currentTimeMillis();
//                
//                if (scheduledSaveTime >= 0 && scheduledSaveTime <= now) {
//                    saveState();
//                    scheduledSaveTime = -1;
//                }
//    
//                try {
//                    if (scheduledSaveTime >= 0) {
//                        backgroundSaver.wait(scheduledSaveTime - now);
//                    } else {
//                        backgroundSaver.wait(); // wait until we're interrupted
//                    }
//                } catch (InterruptedException ex) {
//                }
//            }
//            // One final save
//            saveState();
//            Log.i(NodeUsage.TAG, "Background state saver finished");
//        }
//    }
}
