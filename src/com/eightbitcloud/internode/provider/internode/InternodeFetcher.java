package com.eightbitcloud.internode.provider.internode;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.util.Log;

import com.eightbitcloud.internode.NodeUsage;
import com.eightbitcloud.internode.ServerErrorException;
import com.eightbitcloud.internode.ServiceType;
import com.eightbitcloud.internode.UsageGraphType;
import com.eightbitcloud.internode.data.Account;
import com.eightbitcloud.internode.data.CounterStyle;
import com.eightbitcloud.internode.data.MeasuredValue;
import com.eightbitcloud.internode.data.MetricGroup;
import com.eightbitcloud.internode.data.Plan;
import com.eightbitcloud.internode.data.PlanInterval;
import com.eightbitcloud.internode.data.Provider;
import com.eightbitcloud.internode.data.Service;
import com.eightbitcloud.internode.data.ServiceIdentifier;
import com.eightbitcloud.internode.data.Unit;
import com.eightbitcloud.internode.data.UsageRecord;
import com.eightbitcloud.internode.data.Value;
import com.eightbitcloud.internode.provider.AccountUpdateException;
import com.eightbitcloud.internode.provider.ProviderFetcher;
import com.eightbitcloud.internode.provider.WrongPasswordException;
import com.eightbitcloud.internode.util.DateTools;
import com.eightbitcloud.internode.util.XMLTools;

public class InternodeFetcher implements ProviderFetcher {
    public static final String METRIC_GROUP = "Metered";
    public static final String USAGE_VALUE = "Usage";
    public static String SERVICETYPE_KEY = "ServiceType";
    public static String SERVICEURL_KEY = "ServiceURL";
    
    

    private Provider provider;
    private URL baseURL;
    private DefaultHttpClient httpClient;
    private DocumentBuilderFactory dbf;

    public InternodeFetcher(Provider provider, KeyStore trustStore) {
        this.provider = provider;
        try {
            dbf = DocumentBuilderFactory.newInstance();
            
            baseURL = new URL("https://customer-webtools-api.internode.on.net/api/v1.5/");

            SSLSocketFactory sf = new SSLSocketFactory(trustStore);
            Scheme httpsScheme = new Scheme("https", sf, 443);
            SchemeRegistry schemeRegistry = new SchemeRegistry();
            schemeRegistry.register(httpsScheme);

            HttpParams params = new BasicHttpParams();
            ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
            params.setParameter(HttpProtocolParams.USER_AGENT, "NodeDroid/0.01 (Android Usage Meter <nodedroid@crimsoncactus.net>)");
            httpClient = new DefaultHttpClient(cm, params);
        } catch (Exception e) {
            // This is pretty unlikely
            e.printStackTrace();
        }

    }
    
    
    


    public Element request(URL url, Account account) throws IOException, ParserConfigurationException, IllegalStateException, SAXException, InterruptedException {
        final HttpGet conn = new HttpGet(url.toString());
        conn.addHeader("Authorization", "Basic " + new String(Base64.encodeBase64(new String(account.getUsername()+":"+account.getPassword()).getBytes())));

        HttpResponse response = executeThenCheckIfInterrupted(conn);

        switch (response.getStatusLine().getStatusCode()) {
        // For some bizarre reason, the server sometimes returns a correct result but with a 500 result code?!?! 
        case HttpStatus.SC_INTERNAL_SERVER_ERROR:
            Log.w(NodeUsage.TAG, "Server responded with 500 result, but attempting to parse anyway");
        case HttpStatus.SC_OK:
            DocumentBuilder db = dbf.newDocumentBuilder();
            Element root;
            try {
                Document doc = db.parse(response.getEntity().getContent());
                root = doc.getDocumentElement();
            } catch (Exception ex) {
                Log.e(NodeUsage.TAG, "Error parsing result for " + url, ex);
                throw new IOException("Error parsing result: " + ex.getMessage());
            }
                
            // If the top node is an "error" node, then its actaully an error
            if (root.getNodeName().equals("error")) {
                String msg = XMLTools.getChildText(root, "msg");
                throw new ServerErrorException(msg);
            } else {
                return root;
            }
        case HttpStatus.SC_UNAUTHORIZED:
            throw new WrongPasswordException();
        default:
            throw new IOException("Response was " + response.getStatusLine());
        }
    }
    
    private URL getServiceURL(Service service) throws MalformedURLException {
        return new URL(service.getProperty(SERVICEURL_KEY).toString() + "/service");
    }
    private URL getUsageURL(Service service) throws MalformedURLException  {
        return new URL(service.getProperty(SERVICEURL_KEY).toString() + "/usage");
    }
    private URL getHistoryURL(Service service) throws MalformedURLException  {
        return new URL(service.getProperty(SERVICEURL_KEY).toString() + "/history");
    }

    
    
    

    public void updateService(Service service) throws AccountUpdateException {
        try {
            updateServiceDetails(service);
            updateUsage(service);
            updateHistory(service);
        } catch (Exception e) {
            throw new AccountUpdateException("Error updating usage for " + service, e);
        }

    }
    
    private boolean parseBoolean(String val) {
        return "yes".equals(val);
    }


    private void updateServiceDetails(Service service) throws IOException, ParseException, IllegalStateException, ParserConfigurationException, SAXException, InterruptedException {
        Element result = XMLTools.getAPINode(request(getServiceURL(service), service.getAccount()));
        Element e = XMLTools.getNode(result, "service");
        
        Plan plan = new Plan();
        MetricGroup group = service.getMetricGroup(METRIC_GROUP);
        plan.setName(XMLTools.getChildText(e, "plan"));

        plan.setProperty("Username", XMLTools.getChildText(e, "username"));
        plan.setProperty("Carrier", XMLTools.getChildText(e, "carrier"));
        plan.setProperty("Speed", XMLTools.getChildText(e, "speed"));
        plan.setNextRollover(DateTools.parseAdelaideDate(XMLTools.getChildText(e, "rollover")));
        plan.setInterval(PlanInterval.valueOf(XMLTools.getChildText(e, "plan-interval")));
        plan.setCost(parseAmount(XMLTools.getNode(e, "plan-cost")));

        service.setProperty("UsageRating", XMLTools.getChildText(e, "usage-rating"));

        group.setExcessCharged(parseBoolean(XMLTools.getChildText(e, "excess-charged")));
        group.setExcessShaped(parseBoolean(XMLTools.getChildText(e, "excess-shaped")));
        group.setExcessRestrictAccess(parseBoolean(XMLTools.getChildText(e, "excess-restrict-access")));
        group.setAllocation(parseQuota(XMLTools.getNode(e, "quota")));

        
        service.setPlan(plan);
    }
    
    private Value parseAmount(Element e) throws IOException {
        String type = e.getAttribute("units");
        if (type.equals("aud"))
            return new Value((long) (Double.parseDouble(e.getFirstChild().getNodeValue()) * 100), Unit.CENT);
        else
            throw new IOException("Unknown units of " + type);
    }






    private void updateUsage(Service s) throws IllegalStateException, MalformedURLException, IOException, ParserConfigurationException, SAXException, InterruptedException {
        Element result = XMLTools.getAPINode(request(getUsageURL(s), s.getAccount()));

        Element node = XMLTools.getNode(result, "traffic");
        long val = Long.parseLong(node.getFirstChild().getNodeValue());

        MeasuredValue usage = s.getMetricGroup(METRIC_GROUP).getComponent(USAGE_VALUE);
        usage.setAmount(new Value(val, Unit.BYTE));
    }



    private Value parseQuota(Element e) throws IOException {
        String type = e.getAttribute("units");
        if (type == null || type.equals("")) {
            type = e.getAttribute("unit");
        }
        if (type.equals("bytes"))
            return new Value(Long.parseLong(e.getFirstChild().getNodeValue()), Unit.BYTE);
        else
            throw new IOException("Unknown units of " + type);
    }


    private void updateHistory(Service service) throws IllegalStateException, MalformedURLException, IOException, ParserConfigurationException, SAXException, ParseException, InterruptedException {
        Element result = XMLTools.getAPINode(request(getHistoryURL(service), service.getAccount()));

        Element e = XMLTools.getNode(result, "usagelist");
        NodeList nl = e.getElementsByTagName("usage");
        
        MeasuredValue usage = service.getMetricGroup(METRIC_GROUP).getComponent(USAGE_VALUE);
        usage.clearUsageRecords();
        
        for (int i = 0; i < nl.getLength(); i++) {
            Element usageE = (Element) nl.item(i);
            
            Date day = DateTools.parseAdelaideDate(usageE.getAttribute("day"));
            Value amount = parseQuota(XMLTools.getNode(usageE, "traffic"));
            usage.addUsageRecord(new UsageRecord(day, amount));
         
        }
//        Log.i(NodeUsage.TAG, "History has " + history.size() + " entries for " + service + " in account " + service.getAccount());
        
    }





    public void fetchServices(Account account) throws AccountUpdateException {
        try {
            Element response = request(baseURL, account);
            NodeList services = XMLTools.getNode(XMLTools.getAPINode(response), "services").getElementsByTagName("service");
            
            Set<Service> oldServices = new HashSet<Service>(account.getAllServices());
            for (int i = 0; i < services.getLength(); i++) {
                Element serviceElement = (Element) services.item(i);
                ServiceIdentifier accountNumber = new ServiceIdentifier(provider.getName(), serviceElement.getFirstChild().getNodeValue());
                

                Service service = account.getService(accountNumber);
                if (service == null) {
                    service = new Service();
                    service.setIdentifier(accountNumber);
                    account.addService(service);
                }
                oldServices.remove(service);
                
                // Set up the Metric Group for the Account - at this stage, we only measure metered usage.
                MetricGroup mg = service.getMetricGroup(METRIC_GROUP);
                if (mg == null) {
                    mg = new MetricGroup(service, METRIC_GROUP, Unit.BYTE, CounterStyle.QUOTA);
                    mg.setGraphTypes(UsageGraphType.MONTHLY_USAGE, UsageGraphType.YEARLY_USAGE);
                    mg.setStyle(CounterStyle.QUOTA);
                
                    MeasuredValue val = new MeasuredValue(Unit.BYTE);
                    val.setName(USAGE_VALUE);
                    val.setAmount(new Value(0, Unit.BYTE));
                    mg.setComponents(Collections.singletonList(val));
                    service.setMetricGroups(Collections.singletonList(mg));
                }
                
                service.setProperty(SERVICETYPE_KEY, ServiceType.valueOf(serviceElement.getAttribute("type")));
                service.setProperty(SERVICEURL_KEY, new URL(baseURL, serviceElement.getAttribute("href")));
                
            }
            
            for (Service service: oldServices) {
                account.removeService(service);
            }
            
        } catch (final Exception ex) {
            throw new AccountUpdateException("Error loading services", ex);
        }
    }





    public void testUsernameAndPassword(Account account) throws AccountUpdateException, WrongPasswordException {
        try {
            request(baseURL, account);
        } catch (WrongPasswordException ex) {
            throw ex;
        } catch (Exception e) {
            throw new AccountUpdateException("Error checking password", e);
        }
    }




    private HttpResponse executeThenCheckIfInterrupted(HttpRequestBase m) throws InterruptedException, ClientProtocolException, IOException {
        HttpResponse resp = httpClient.execute(m);
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
        return resp;
    }
}
