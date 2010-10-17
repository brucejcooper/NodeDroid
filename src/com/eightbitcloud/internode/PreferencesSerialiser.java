package com.eightbitcloud.internode;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.eightbitcloud.internode.data.Account;
import com.eightbitcloud.internode.data.PlanInterval;
import com.eightbitcloud.internode.data.Provider;
import com.eightbitcloud.internode.data.ProviderStore;
import com.eightbitcloud.internode.data.Service;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class PreferencesSerialiser {
    private static final String PASSWORD = ".password";
    private static final String USERNAME = ".username";
    private static final String PROVIDER = ".provider";

    private static final String PREF = ".pref";
    private static final String PREF_KEY = ".name";
    private static final String PREF_VALUE = ".value";
    

    
    
    private static final String PREFIX_SERVICE = ".service";
    private static final String PREFIX_ACCT = "acct";    
    
    private static final String USAGE = ".usage";
    private static final String PLAN_COST = ".planCost";
    private static final String PLAN_INTERVAL = ".planInterval";
    private static final String EXCESS_RESTRICT_ACCESS = ".excessRestrictAccess";
    private static final String EXCESS_SHAPED = ".excessShaped";
    private static final String EXCESS_CHARGED = ".excessCharged";
    private static final String NEXT_ROLLOVER = ".nextRollover";
    private static final String USAGE_RATING = ".usageRating";
    private static final String SPEED = ".speed";
    private static final String CARRIER = ".carrier";
    private static final String PLAN = ".plan";
    private static final String QUOTA = ".quota";
    private static final String HREF = ".href";
    private static final String SERVICE_TYPE = ".serviceType";
    private static final String SERVICE_IDENTIFIER = ".serviceIdentifier";
    private static final String HISTORY = ".history.";

    
    public static final String PREFS_FILE = "com.eightbitcloud.internode.InternodeAccount";
    

    public static void serialise(List<Account> accounts, SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        
        clearOld(prefs, editor);
        int accountNum = 1;
        for (Account account: accounts) {
            String accountPrefix = PREFIX_ACCT+ accountNum;
            editor.putString(accountPrefix + USERNAME, account.getUsername());
            editor.putString(accountPrefix + PASSWORD, account.getPassword());
            editor.putString(accountPrefix + PROVIDER, account.getProvider().getName());
            saveProperties(accountPrefix + PREF, editor, account.getProperties());
            
            
            /*
            
            int serviceCount = 1;
            for (Service service : account.getAllServices()) {
                String servicePrefix = accountPrefix + PREFIX_SERVICE + serviceCount;
                
                editor.putString (servicePrefix + SERVICE_IDENTIFIER,     service.getServiceIdentifier());
                editor.putString (servicePrefix + SERVICE_TYPE,           service.getType() == null ? ServiceType.Personal_ADSL.toString() : service.getType().toString());
                editor.putString (servicePrefix + HREF,                   service.getHref() == null ? null : service.getHref().toString());
                editor.putLong   (servicePrefix + QUOTA,                  service.getQuota());
                editor.putString (servicePrefix + PLAN,                   service.getPlan());
                editor.putString (servicePrefix + CARRIER,                service.getCarrier());
                editor.putString (servicePrefix + SPEED,                  service.getSpeed());
                editor.putString (servicePrefix + USAGE_RATING,           service.getUsageRating());
                editor.putLong   (servicePrefix + NEXT_ROLLOVER,          convertToLong(service.getNextRollover()));
                editor.putBoolean(servicePrefix + EXCESS_CHARGED,         service.isExcessCharged());
                editor.putBoolean(servicePrefix + EXCESS_SHAPED,          service.isExcessShaped());
                editor.putBoolean(servicePrefix + EXCESS_RESTRICT_ACCESS, service.isExcessRestrictAccess());
                editor.putString (servicePrefix + PLAN_INTERVAL,          service.getPlanInterval() == null ? PlanInterval.Monthly.toString() : service.getPlanInterval().toString());
                editor.putLong   (servicePrefix + PLAN_COST,              service.getPlanCost());
                editor.putLong   (servicePrefix + USAGE,                  service.getUsage());
                
                
                for (Map.Entry<Date, Long> e: service.getHistory().entrySet()) {
                    editor.putLong(servicePrefix + HISTORY + DateTools.PREFS_DATE_FORMAT.format(e.getKey()), e.getValue());
                }

                serviceCount++;
            }
            */
            accountNum++;
        }
        editor.commit();
        
    }
    

    private static void clearOld(SharedPreferences prefs, Editor editor) {
        Map<String, ?> existing = prefs.getAll();
        for (String key: existing.keySet()) {
            if (key.startsWith(PREFIX_ACCT))
                editor.remove(key);
        }
    }

    
    private static void saveProperties(String prefix, Editor editor, Map<String, ?> properties) {
        int prefNum = 1;
        for (Map.Entry<String, ?> entry: properties.entrySet()) {
            editor.putString(prefix + prefNum + PREF_KEY, entry.getKey());
            editor.putString(prefix + prefNum + PREF_VALUE, entry.getValue().toString());
            
            prefNum++;
        }   
    }

    
    public static Map<String,String> loadProperties(SharedPreferences prefs, String prefix) {
        Map<String,String> result = new HashMap<String,String>();
        
        int prefNum = 1;
        while (prefs.contains(prefix + prefNum + PREF_KEY)) {
            String key = prefs.getString(prefix + prefNum + PREF_KEY, "");
            String value = prefs.getString(prefix + prefNum + PREF_VALUE, "");
            
            result.put(key, value);
            prefNum++;
        }
        
        return result;
    }
    

    public static void deserialise(SharedPreferences prefs, List<Account> accounts) {
        accounts.clear();
        
        int accountNum = 1;
        while (prefs.contains(PREFIX_ACCT+ accountNum + USERNAME)) {
            String accountPrefix = PREFIX_ACCT+ accountNum;
            
            Account account = new Account(); // accounts.getAccount(prefs.getString(accountPrefix + USERNAME, ""));
            account.setUsername(prefs.getString(accountPrefix + USERNAME, ""));
            account.setPassword(prefs.getString(accountPrefix + PASSWORD, ""));
            account.setProvider(ProviderStore.getInstance().getProvider(prefs.getString(accountPrefix + PROVIDER, "internode")));
            account.addProperties(loadProperties(prefs, accountPrefix + PREF));
            
            accounts.add(account);
            
/*            
            int serviceCount = 1;
            while (prefs.contains(accountPrefix + PREFIX_SERVICE + serviceCount + SERVICE_IDENTIFIER)) {
                String servicePrefix = accountPrefix + PREFIX_SERVICE + serviceCount;
                
                Service service = account.getService(prefs.getString (servicePrefix + SERVICE_IDENTIFIER, ""));
                
                service.setType(ServiceType.valueOf(prefs.getString (servicePrefix + SERVICE_TYPE, ServiceType.Personal_ADSL.toString())));          
                try {
                    service.setHref(new URL(prefs.getString (servicePrefix + HREF, "")));
                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }                
                service.setQuota                (prefs.getLong      (servicePrefix + QUOTA, 0));                
                service.setPlan                 (prefs.getString    (servicePrefix + PLAN, ""));                
                service.setCarrier              (prefs.getString    (servicePrefix + CARRIER, ""));              
                service.setSpeed                (prefs.getString    (servicePrefix + SPEED, ""));                
                service.setUsageRating          (prefs.getString    (servicePrefix + USAGE_RATING,  ""));         
                service.setNextRollover      (new Date(prefs.getLong(servicePrefix + NEXT_ROLLOVER, 0)));          
                service.setExcessCharged        (prefs.getBoolean   (servicePrefix + EXCESS_CHARGED, false));        
                service.setExcessShaped         (prefs.getBoolean   (servicePrefix + EXCESS_SHAPED, false));        
                service.setExcessRestrictAccess (prefs.getBoolean   (servicePrefix + EXCESS_RESTRICT_ACCESS, false));
                service.setPlanInterval(PlanInterval.valueOf(prefs.getString (servicePrefix + PLAN_INTERVAL, PlanInterval.Monthly.toString()))); 
                service.setPlanCost             (prefs.getLong      (servicePrefix + PLAN_COST, 0));  
                service.setUsage                (prefs.getLong      (servicePrefix + USAGE, 0));

                SortedMap<Date,Long> newHistory = new TreeMap<Date,Long>();
                for (String key: prefs.getAll().keySet()) {
                    String check = servicePrefix + HISTORY;
                    if (key.startsWith(check)) {
                        String keyStr= key.substring(check.length());
                        try {
                            Date d = DateTools.PREFS_DATE_FORMAT.parse(keyStr);
                            long val = prefs.getLong(key, 0L); 
                            
                            newHistory.put(d, val);
                        } catch (ParseException ex) {
                            Log.e(NodeUsage.TAG, "Error parsing history value of " + keyStr + " from " + key, ex);
                        }
                    }
                }
                service.setHistory(newHistory);
                
                
                serviceCount++;
            }
*/            
            accountNum++;
        }
    }
    
}
