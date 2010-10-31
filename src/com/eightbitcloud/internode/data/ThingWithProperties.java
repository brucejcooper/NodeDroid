package com.eightbitcloud.internode.data;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ThingWithProperties implements Serializable{
    /**
     * 
     */
    private static final long serialVersionUID = 7786505960268077355L;
    private Map<String, Object> properties;

    public void setProperty(String key, Object value) {
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }
        properties.put(key, value);
    }

    public Object getProperty(String key) {
        return properties == null ? null : properties.get(key);
    }

    public void addProperties(Map<String, String> loadProperties) {
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }
        properties.putAll(loadProperties);
        
    }

    @SuppressWarnings("unchecked")
    public Map<String,?> getProperties() {
        return properties == null ? Collections.EMPTY_MAP : properties;
    }

}
