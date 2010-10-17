package com.eightbitcloud.internode.data;

import java.security.KeyStore;
import java.util.Map;

import android.graphics.Color;

import com.eightbitcloud.internode.GraphColors;
import com.eightbitcloud.internode.provider.ProviderFetcher;
import com.eightbitcloud.internode.provider.internode.InternodeFetcher;
import com.eightbitcloud.internode.provider.optus.OptusFetcher;

public class Provider extends ThingWithProperties {
    private String name;
    private String url;
    private String textColour;
    private String backgroundResource;
    private String logoURL;
    
    private GraphColors graphColors;
    
    
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
    
    public ProviderFetcher createFetcher() {
        if (name.equalsIgnoreCase("internode")) {
            KeyStore ts = (KeyStore) ProviderStore.getInstance().getProvider("internode").getProperty("keyStore");
            return new InternodeFetcher(this,ts);
        } else if (name.equalsIgnoreCase("optus")) {
            return new OptusFetcher(this);
        } else
            return null;
    }
    
    @Override
    public String toString() {
        return name;
        
    }
}
