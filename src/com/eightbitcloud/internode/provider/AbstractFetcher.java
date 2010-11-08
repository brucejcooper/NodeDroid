package com.eightbitcloud.internode.provider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;

import android.util.Log;

import com.eightbitcloud.internode.NodeUsage;
import com.eightbitcloud.internode.data.Provider;

public abstract class AbstractFetcher implements ProviderFetcher {
    private static final int MAX_LOG_LENGTH = 4000;
    protected Provider provider;
    protected boolean logTraffic = false;
    private HttpClient httpClient;
    
    public AbstractFetcher(Provider provider) {
        this.provider = provider;
    }

    protected HttpClient createHttpClient() {
        HttpClient client = new DefaultHttpClient();
        client.getParams().setParameter(HttpProtocolParams.USER_AGENT, "NodeDroid/2.02 (Android Usage Meter <nodedroid@crimsoncactus.net>)");
        return client;
    }
    

    public void setLogTraffic(boolean val) {
        logTraffic = val;
    }

    
    protected HttpResponse executeThenCheckIfInterrupted(HttpRequestBase m, String... logValueToRedact) throws InterruptedException, ClientProtocolException,
            IOException {
        if (httpClient == null) {
            httpClient = createHttpClient();
        }
        if (logTraffic) {
            logTraffic("Performing " + m.getMethod() + " on " + m.getURI());
            for (Header h: m.getAllHeaders()) {
                if (h.getName().equals("Authorization")) {
                    logTraffic("Authorization header not logged for security reasons");
                    
                } else {
                    logTraffic("Request Header " + h);
                }
            }
            
            if (m instanceof HttpPost) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ((HttpPost) m).getEntity().writeTo(baos);
                baos.close();
                
                String stringRepresentation = URLDecoder.decode(new String(baos.toByteArray()));  
                for (String token: logValueToRedact) {
                    stringRepresentation = stringRepresentation.replace(token, "XXX");
                }
                
                logTraffic("Request Entity: " + stringRepresentation);
            }
            
        }
        HttpResponse resp = httpClient.execute(m);
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
        
        if (logTraffic) {
            
            logTraffic( "Response Status " + resp.getStatusLine());
            for (Header h: resp.getAllHeaders()) {
                logTraffic( "Response Header: " + h);
            }
            
            HttpEntity e = resp.getEntity();
            e = new BufferedHttpEntity(e);
            resp.setEntity(e);
            
            logTraffic( "Response Body: " + EntityUtils.toString(e));
        }
        
        return resp;
    }

    
    private void logTraffic(String x) {
        if (x.length() > MAX_LOG_LENGTH) {
            logTraffic(x.substring(0, MAX_LOG_LENGTH));
            logTraffic(x.substring(MAX_LOG_LENGTH));
        } else {
            Log.d(NodeUsage.TAG, "HTTP" + Thread.currentThread().getId() + ": " + x);
        }
    }
    
}

