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

import com.eightbitcloud.internode.data.Account;
import com.eightbitcloud.internode.data.PreferencesSerialisable;

public class PreferencesSerialiser {
    private static final String PASSWORD = ".password";
    private static final String USERNAME = ".username";
    private static final String PROVIDER = ".provider";

//    private static final String PREF = ".pref";
    private static final String PREF_KEY = ".name";
    private static final String PREF_VALUE = ".value";
    
    private static final String PREFIX_ACCT = "acct";    


    
    
    public static final String PREFS_FILE = "com.eightbitcloud.internode.InternodeAccount";
    


    
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
            
            String providerName = prefs.getString(accountPrefix + PROVIDER, "internode");
            if (providerName.equals("Optus")) {   // Legacy name for Optus Mobile
                providerName = "Optus Mobile";
            }
            account.setProviderName(providerName);
//            account.addProperties(loadProperties(prefs, accountPrefix + PREF));
            
            accounts.add(account);
            
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
