package com.eightbitcloud.internode.data;

import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.eightbitcloud.internode.GraphColors;
import com.eightbitcloud.internode.provider.InternodeFetcher;
import com.eightbitcloud.internode.provider.OptusFetcher;
import com.eightbitcloud.internode.provider.ProviderFetcher;
import com.eightbitcloud.internode.provider.VodafoneMBBFetcher;

public class Provider {
    private String name;
    private String url;
    private String textColour;
    private String backgroundResource;
    private String logoURL;
    
    private GraphColors graphColors;
    private boolean beta;
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

    
    public GraphColors getGraphColors() {
        return graphColors;
    }
    public void setGraphColors(GraphColors graphColors) {
        this.graphColors = graphColors;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public String getTextColour() {
        return textColour;
    }
    public void setTextColour(String primaryColour) {
        this.textColour = primaryColour;
    }
    public String getBackgroundResource() {
        return backgroundResource;
    }
    public void setBackgroundResource(String secondaryColour) {
        this.backgroundResource = secondaryColour;
    }
    public String getLogoURL() {
        return logoURL;
    }
    public void setLogoURL(String logoURL) {
        this.logoURL = logoURL;
    }
    
    public ProviderFetcher createFetcher(Context ctx) {
        if (name.equalsIgnoreCase("internode")) {
            KeyStore ts = (KeyStore) ProviderStore.getInstance().getProvider("internode").getProperty("keyStore");
            return new InternodeFetcher(this, ctx, ts);
        } else if (name.equalsIgnoreCase("Vodafone MBB")) {
            return new VodafoneMBBFetcher(this, ctx);
        } else if (name.equalsIgnoreCase("optus mobile")) {
            return new OptusFetcher(this, ctx);
        } else
            return null;
    }
    

    @Override
    public String toString() {
        return name;
        
    }
    public boolean isBeta() {
        return beta;
    }
    
    public void setBeta(boolean beta) {
        this.beta = beta;
    }
}
