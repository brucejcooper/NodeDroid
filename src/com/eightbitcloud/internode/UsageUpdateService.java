package com.eightbitcloud.internode;

import java.util.Date;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.eightbitcloud.internode.data.Account;
import com.eightbitcloud.internode.data.AccountProvider;
import com.eightbitcloud.internode.data.Service;
import com.eightbitcloud.internode.data.ServiceIdentifier;
import com.eightbitcloud.internode.data.UpdateStatus;
import com.eightbitcloud.internode.provider.ProviderFetcher;
import com.eightbitcloud.internode.provider.ServiceUpdateDetails;

public class UsageUpdateService extends android.app.Service {
    private static String TAG = "UsageUpdateService";
    private AccountUpdater ex;
    private Handler handler = new Handler();
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        
        startUpdate();
        return START_REDELIVER_INTENT;
    }
    
    void startUpdate() {
        if (ex != null && ex.isAlive()) {
            ex.shutdown();
        }
        ex = new AccountUpdater();
        ex.start();
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
    }
    
    @Override
    public void onDestroy() {
        Log.i(TAG, "Shutting down update service");
        ex.shutdown();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // We don't support binding directly.
        return null;
    }
    
    public boolean setServiceStatus(Uri uri, UpdateStatus status) {
        ContentResolver resolver = getContentResolver();
        ContentValues statusUpdate = new ContentValues();
        statusUpdate.put(Service.STATUS, status.ordinal());
        return resolver.update(uri, statusUpdate, null, null) > 0;
    }
    
    private void setStatusForAllServices(UpdateStatus status) {
        ContentValues cv = new ContentValues();
        cv.put(Service.STATUS, status.ordinal());
        getContentResolver().update(AccountProvider.SERVICES_CONTENT_URI, cv , null, null);
    }

    private void setStatusForAllServicesInAccount(Account account, UpdateStatus status) {
        Log.d(TAG, "Marking all services in account " + account.getUri() + " as " + status);
        ContentValues cv = new ContentValues();
        cv.put(Service.STATUS, status.ordinal());
        ContentResolver resolver = getContentResolver();
        resolver.update(AccountProvider.SERVICES_CONTENT_URI, cv, Service.ACCOUNT_ID + " = ?", new String[] { Integer.toString(account.getId())});
        
    }
    
    private void notifyError(final Account account, final Exception e) {
        Log.e(TAG, "Error updating Accounts: ", e);
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(UsageUpdateService.this, "Error updating account " + account + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    
    
    class AccountUpdater extends Thread {
        private ContentResolver resolver = getContentResolver();
        private boolean running = true;

        public AccountUpdater() {
        }
        
        public void shutdown() {
            Log.i(TAG, "AccountUpdater thread cancelled");
            running = false;
            interrupt();
            // Wait for it to shutdown, but not too long!
            try {
                join(1000);
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while waiting for updater to shut down.");
            }
        }

        private Uri createServiceUri(Account account, ServiceIdentifier identifier) {
            return AccountProvider.SERVICES_CONTENT_URI.buildUpon().appendPath(Integer.toString(account.getId())).appendPath(identifier.getAccountNumber()).build(); 
        }
        
        private void updateAccount(Account account) {
            ProviderFetcher fetcher = account.getProvider().createFetcher(UsageUpdateService.this);
            try {
                boolean extraLogging = PreferenceManager.getDefaultSharedPreferences(UsageUpdateService.this).getBoolean("performExtraLogging", false);
                fetcher.setLogTraffic(extraLogging);

                try {
                    Log.d(TAG, "Logging In");
                    List<ServiceUpdateDetails> results = fetcher.fetchAccountUpdates(account);

                    // Set all services to "Updating", creating them if
                    // necessary
                    for (ServiceUpdateDetails sud : results) {
                        if (!running)
                            return;
                        Uri uri = createServiceUri(account, sud.getIdentifier());

                        if (!setServiceStatus(uri, UpdateStatus.PENDING_UPDATE)) {
                            Log.i(TAG, "Creating new service " + sud.getIdentifier());
                            // It wasn't there... Must be new!
                            Service service = new Service();
                            service.setAccountID(account.getId());
                            service.setIdentifier(sud.getIdentifier());
                            service.setLastUpdate(new Date());
                            service.setServiceType(sud.getServiceType());
                            service.setUpdateStatus(UpdateStatus.PENDING_UPDATE);
                            resolver.insert(AccountProvider.SERVICES_CONTENT_URI, service.getValues());
                        }
                    }

                    for (ServiceUpdateDetails sud : results) {
                        if (!running)
                            return;
                        Uri uri = createServiceUri(account, sud.getIdentifier());
                        Log.i(TAG, "Updating service " + uri);
                        setServiceStatus(uri, UpdateStatus.UPDATING);

                        try {
                            Service serviceData = fetcher.fetchServiceDetails(account, sud);
                            serviceData.setUpdateStatus(UpdateStatus.IDLE);
                            serviceData.setLastUpdate(new Date());
                            ContentValues vals = serviceData.getValues();
                            vals.remove(Service.ACCOUNT_ID); // Remove these ones as we don't want to change them, especially as updateDate and status will not be set
                            vals.remove(Service.SERVICE_PROVIDER);
                            vals.remove(Service.SERVICE_ID);
                            Log.i(TAG, "Finished updating service " + sud.getIdentifier() + ", marking it as IDLE");
                            resolver.update(uri, vals, null, null);
                        } catch (InterruptedException e) {
                            throw e; // Re-throw it.  The outer catch will catch it and handle the interruption clause.
                        } catch (Exception e) {
                            setServiceStatus(uri, UpdateStatus.FAILED);
                            Toast.makeText(UsageUpdateService.this, "Error updating service " + sud.getIdentifier().getAccountNumber() + " in account " + account + ": " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Error updating Service: ", e);
                        }
                    }

                } catch (InterruptedException e) {
                    Log.w(TAG, "Interrupted fetching account " + account);
                } catch (Exception e) {
                    setStatusForAllServicesInAccount(account, UpdateStatus.FAILED);
                    notifyError(account,  e);
                }
            } finally {
                fetcher.cleanup();
            }
        }
        
        
        
        @Override
        public void run() {
            Cursor accountsCursor = getContentResolver().query(AccountProvider.ACCOUNTS_CONTENT_URI, null, null, null, null);
            try {
                // Mark all services as STALE.  This looks like "Pending" to the user, but can be used
                // to delete ones that don't get updated at the end.
                setStatusForAllServices(UpdateStatus.STALE);
                
                if (running & accountsCursor.moveToFirst()) {
                    do {
                        Account account = new Account(accountsCursor);
                        updateAccount(account);
                    } while (running & accountsCursor.moveToNext());
                }
            } finally {
                accountsCursor.close();
                if (running) {
                    // remove stale services that were not updated above
                    resolver.delete(AccountProvider.SERVICES_CONTENT_URI, Service.STATUS + " = ?", new String[] { Integer.toString(UpdateStatus.STALE.ordinal()) });
                    
                    // Update any widgets.
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            NodeDroidWidgetProvider.updateWidgetsFor(UsageUpdateService.this);
                        }
                    });
                    
                    // If we got this far, it has all worked, and we can shut ourselves down.
                    Log.i(TAG, "Shutting down service, as it completed successfully");
                    stopSelf();
                } else {
                    // If we were cancelled, return all services to IDLE.
                    // TODO should it leave FAILED as is?
                    setStatusForAllServices(UpdateStatus.IDLE);
                }
            }
        }
    }
}
