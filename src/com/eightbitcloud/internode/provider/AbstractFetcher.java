package com.eightbitcloud.internode.provider;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.util.Log;

import com.eightbitcloud.internode.NodeUsage;
import com.eightbitcloud.internode.data.Provider;

public abstract class AbstractFetcher implements ProviderFetcher {
    protected Provider provider;
    protected boolean logTraffic = false;
    private DefaultHttpClient httpClient;
    private Context context;
    
    
    private ThreadLocal<PrintWriter> logWriter = new ThreadLocal<PrintWriter>() {
        @Override
        protected synchronized PrintWriter initialValue() {
            DateFormat f = new SimpleDateFormat("yyyyMMddHHmmss");
            try {
                return  new PrintWriter(new OutputStreamWriter(context.openFileOutput(
                        "FetcherLog-" + provider.getName().replace(' ', '_') + '-' + f.format(new Date()) + '-' + Thread.currentThread().getId() + ".txt", Context.MODE_APPEND | Context.MODE_WORLD_READABLE)));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }
    };

    
    public AbstractFetcher(Provider provider, Context ctx) {
        this.provider = provider;
        this.context = ctx;
    }
    
    protected HttpParams createHttpParams() {
        HttpParams params = new BasicHttpParams();
        params.setParameter(HttpProtocolParams.USER_AGENT, "NodeDroid/2.02 (Android Usage Meter <nodedroid@crimsoncactus.net>)");
        return params;
    }
    
    protected SchemeRegistry createSchemeRegistry() {
        try {
            SchemeRegistry sr = new SchemeRegistry();
            sr.register(new Scheme("http", new PlainSocketFactory(), 80));
    
            SSLSocketFactory sf = SSLSocketFactory.getSocketFactory();
            Scheme httpsScheme = new Scheme("https", sf, 443);
            sr.register(httpsScheme);
            
            return sr;
            
        } catch (Exception ex) {
            // Unlikely to happen
            return null;
        }

    }

    protected DefaultHttpClient createHttpClient() {
//        try {
            HttpParams params = createHttpParams();
            ClientConnectionManager cm = new SingleClientConnManager(params, createSchemeRegistry());
            params.setParameter(HttpProtocolParams.USER_AGENT, "NodeDroid/2.02 (Android Usage Meter <nodedroid@crimsoncactus.net>)");
            return  new DefaultHttpClient(cm, params);
//        } catch (Exception e) {
//            // This is pretty unlikely
//            e.printStackTrace();
//            return null;
//        }
    }
    

    @Override
    public void setLogTraffic(boolean val) {
        logTraffic = val;
    }

    
    public String getCookie(String name) {
        if (httpClient == null) {
            return null;
        }
        
        for (Cookie c: httpClient.getCookieStore().getCookies()) {
            Log.d(NodeUsage.TAG, "Cookie is " + c);
            if (c.getName().equals(name)) {
                return c.getValue();
            }
                
        }
        return null;
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
        if (Thread.interrupted()) {
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

    
    private void logTraffic(String x) throws IOException {
        
        logWriter.get().println(x);
//        if (x.length() > MAX_LOG_LENGTH) {
//            logTraffic(x.substring(0, MAX_LOG_LENGTH));
//            logTraffic(x.substring(MAX_LOG_LENGTH));
//        } else {
//            Log.d(NodeUsage.TAG, "HTTP" + Thread.currentThread().getId() + ": " + x);
//        }
    }
 
    
    @Override
    public void cleanup() {
        if (logTraffic) {
            logWriter.get().close();
        }
    }

    
}

