package com.eightbitcloud.internode.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class ThingWithProperties implements PreferencesSerialisable{
    private Map<String, String> properties;

    public void setProperty(String key, String value) {
        if (properties == null) {
            properties = new HashMap<String, String>();
        }
        properties.put(key, value);
    }

    public String getProperty(String key) {
        return properties == null ? null : properties.get(key);
    }

    public void addProperties(Map<String, String> loadProperties) {
        if (properties == null) {
            properties = new HashMap<String, String>();
        }
        properties.putAll(loadProperties);
        
    }

    @SuppressWarnings("unchecked")
    public Map<String,?> getProperties() {
        return properties == null ? Collections.EMPTY_MAP : properties;
    }

    
    public void writeTo(JSONObject obj) throws JSONException {
        if (properties != null) {
            for (Map.Entry<String,String> e: properties.entrySet()) {
                obj.put(e.getKey(), e.getValue());
            }
        }
    }

    public void readFrom(JSONObject obj) throws JSONException {
        @SuppressWarnings("unchecked")
        Iterator<String> keys = obj.keys();
        
        while (keys.hasNext()) {
            String key = keys.next();
            setProperty(key, obj.getString(key));
        }
        
    }

}
