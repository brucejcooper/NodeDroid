package com.eightbitcloud.internode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.eightbitcloud.internode.data.Account;
import com.eightbitcloud.internode.data.PreferencesSerialisable;
import com.eightbitcloud.internode.data.ProviderStore;
import com.eightbitcloud.internode.data.Service;

public class PreferencesSerialiser {
    private static final String PASSWORD = ".password";
    private static final String USERNAME = ".username";
    private static final String PROVIDER = ".provider";

    private static final String PREF = ".pref";
    private static final String PREF_KEY = ".name";
    private static final String PREF_VALUE = ".value";
    
    private static final String PREFIX_SERVICE = ".service";
    private static final String PREFIX_ACCT = "acct";    


    
    
    public static final String PREFS_FILE = "com.eightbitcloud.internode.InternodeAccount";
    

    public static void serialise(List<Account> accounts, SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        
        clearOld(prefs, editor);
        int accountNum = 1;
        
        /*
         * Write out the accounts as simple preferences, but the service is serialised.
         * The service is far more complicated than the account, so its a pain to write
         * custom serialisation.  This makes serialisation attractive, but we need to
         * remember that data types change, which screws up serialisation.  In this case,
         * The accounts are still important to us, because that is our fundamental 
         * configuration, but the service information is only a cache and can be 
         * regenerated.  IF we have a deserialisation problem then we simply throw away
         * the data and regenerate it.  
         * 
         * This is why we have a hybrid approach.
         */
        for (Account account: accounts) {
            String accountPrefix = PREFIX_ACCT+ accountNum;
            editor.putString(accountPrefix + USERNAME, account.getUsername());
            editor.putString(accountPrefix + PASSWORD, account.getPassword());
            editor.putString(accountPrefix + PROVIDER, account.getProvider().getName());
            saveProperties(accountPrefix + PREF, editor, account.getProperties());

            try {
                int serviceCount = 1;
                for (Service service : account.getAllServices()) {
                    String servicePrefix = accountPrefix + PREFIX_SERVICE + serviceCount;
                    
                    JSONObject obj = new JSONObject();
                    service.writeTo(obj);
                    editor.putString (servicePrefix, obj.toString());
                    serviceCount++;
                }
            } catch (JSONException ex) {
                // Thats' okay.  We don't _NEED_ the service information, as it can be regenerated.  Just ignore it.
                Log.e(NodeUsage.TAG, "Error serialising services", ex);
            }
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
            
            int serviceCount = 1;
            while (prefs.contains(accountPrefix + PREFIX_SERVICE + serviceCount)) {
                String servicePrefix = accountPrefix + PREFIX_SERVICE + serviceCount;
                
                try {
                    String txt= prefs.getString(servicePrefix, null);
                    if (txt != null) {
                        JSONObject json = new JSONObject(txt);
                        Service service = new Service();
                        service.readFrom(json);
                        account.addService(service);
                    }
                } catch (NullPointerException ex) {
                    Log.w(NodeUsage.TAG, "Error deserialising service.  It will be regenerated by service fetch", ex);
                } catch (Exception ex) {
                    Log.w(NodeUsage.TAG, "Error deserialising service.  It will be regenerated by service fetch", ex);
                    // Thats okay, we won't loose any information.
                }
                serviceCount++;
            }
            accountNum++;
        }
    }



    

    
    public static JSONObject createJSONRepresentation(Map<String,? extends PreferencesSerialisable> map, JSONObject obj) throws JSONException {
        for (Entry<String, ? extends PreferencesSerialisable> e: map.entrySet()) {
            JSONObject child = new JSONObject();
            e.getValue().writeTo(child);
            obj.put(e.getKey(), child);
        }

        return obj;
    }


    

    public static JSONObject createJSONRepresentation(PreferencesSerialisable map) throws JSONException {
        JSONObject obj = new JSONObject();
        map.writeTo(obj);
        return obj;
    }
    
    public static JSONArray createJSONRepresentation(Collection<? extends PreferencesSerialisable> map) throws JSONException {
        JSONArray array = new JSONArray();
        for (PreferencesSerialisable s: map) {
            JSONObject obj = new JSONObject();
            s.writeTo(obj);
            array.put(obj);
        }
        return array;
    }

    public static JSONArray createJSONRepresentationForStrings(List<? extends Object> map) throws JSONException {
        JSONArray array = new JSONArray();
        for (Object s: map) {
            array.put(s.toString());
        }
        return array;
    }

    
    public static <T extends PreferencesSerialisable> void populateJSONList(List<T> list, Class<T> clz, JSONArray jsonArray) throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                T obj = clz.newInstance();
                JSONObject jsO = jsonArray.getJSONObject(i);
                obj.readFrom(jsO);
                list.add(obj);
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InstantiationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }


    public static List<String> createStringArray(JSONArray jsonArray) throws JSONException {
        ArrayList<String> result = new ArrayList<String>();
        for (int i = 0; i < jsonArray.length(); i++) {
            result.add(jsonArray.getString(i));
        }
        return result;
    }


}
