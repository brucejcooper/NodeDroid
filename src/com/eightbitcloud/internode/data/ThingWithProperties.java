package com.eightbitcloud.internode.data;

import java.util.HashMap;
import java.util.Map;

public class ThingWithProperties {
    private Map<String, Object> properties = new HashMap<String, Object>();

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public void addProperties(Map<String, String> loadProperties) {
        properties.putAll(loadProperties);
        
    }

    public Map<String,?> getProperties() {
        return properties;
    }

}
