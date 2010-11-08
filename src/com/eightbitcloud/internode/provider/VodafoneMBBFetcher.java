package com.eightbitcloud.internode.provider;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;

import com.eightbitcloud.internode.UsageGraphType;
import com.eightbitcloud.internode.data.Account;
import com.eightbitcloud.internode.data.CounterStyle;
import com.eightbitcloud.internode.data.MeasuredValue;
import com.eightbitcloud.internode.data.MetricGroup;
import com.eightbitcloud.internode.data.Plan;
import com.eightbitcloud.internode.data.Provider;
import com.eightbitcloud.internode.data.ProviderStore;
import com.eightbitcloud.internode.data.Service;
import com.eightbitcloud.internode.data.ServiceIdentifier;
import com.eightbitcloud.internode.data.Unit;
import com.eightbitcloud.internode.data.UsageRecord;
import com.eightbitcloud.internode.data.Value;

public class VodafoneMBBFetcher extends AbstractFetcher {
    private static final String REMAINING_SMS = "REMAINING_SMS";
    private static final String REMAINING_DATA = "REMAINING_DATA";
    private static final String DATA_GROUP = "Data";
    private static final String SMS_GROUP = "SMS";
    private static final String USAGE = "USAGE";

    // private static final String EX_CAP = "Other";

    public DateFormat expiryDateFormatter = new SimpleDateFormat("EEE MMM dd hh:mm:ss zzz yyyy");

    public TimeZone melbourneTZ;

    public VodafoneMBBFetcher(Provider provider) {
        super(provider);
    }
    
    @Override
    public HttpClient createHttpClient() {
        BasicHttpParams params = new BasicHttpParams();
        params.setParameter(HttpProtocolParams.USER_AGENT, "NodeDroid/2.02 (Android Usage Meter <nodedroid@crimsoncactus.net>)");
        HttpClientParams.setCookiePolicy(params, CookiePolicy.RFC_2109);

        HttpClient client = new DefaultHttpClient(params);
        return client;
    }
    
   
    
    private String login(Account account) throws AccountUpdateException, InterruptedException {
        try {
            HttpGet loginPage = new HttpGet("https://secure.broadband.vodafone.com.au/CRMVOD/login");
            HttpResponse resp = executeThenCheckIfInterrupted(loginPage);
            if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                throw new IOException("Expected OK Response");
            resp.getEntity().consumeContent();

            // TODO should extract post location from previous body.
            HttpPost loginPost = new HttpPost("https://secure.broadband.vodafone.com.au/CRMVOD/login");

            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("forgottenpin", ""));
            formparams.add(new BasicNameValuePair("login", "true"));
            formparams.add(new BasicNameValuePair("phoneOrAccountNumber", account.getUsername()));
            formparams.add(new BasicNameValuePair("accountPIN", account.getPassword()));

            loginPost.setEntity(new UrlEncodedFormEntity(formparams, "UTF-8"));
            resp = executeThenCheckIfInterrupted(loginPost, account.getUsername(), account.getPassword());

            if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                throw new IOException("Expected OK from login: " + resp.getStatusLine());

            String loginResult = EntityUtils.toString(resp.getEntity());
            return loginResult;
        } catch (IOException ex) {
            throw new AccountUpdateException("Error logging in", ex);

        }
    }

    public List<ServiceUpdateDetails> fetchAccountUpdates(Account account) throws AccountUpdateException, InterruptedException {
        try {
            List<ServiceUpdateDetails> result = new ArrayList<ServiceUpdateDetails>();
            String loginResult = login(account);

            int startTable = loginResult.indexOf("<table id=\"account_summary_details_prepaid\"");
            if (startTable == -1) {
                throw new AccountUpdateException("Could not fund account summary start", null);
            }
            int endTable = loginResult.indexOf("</table>", startTable);

            String tableStr = loginResult.substring(startTable, endTable + 8);

            Pattern valueMatcher = Pattern.compile("<label( class=\"textbold\")?>([^<]*)</label>");
            Matcher m = valueMatcher.matcher(tableStr);
            String[] row;
            
            Date expiry;
            Value remainingData = new Value(0, Unit.BYTE);
            Value remainingSMS = new Value(0, Unit.COUNT);
            
            
            while ((row = getValues(2, m, 2)) != null) {
                String key = row[0];
                String value = row[1];

                if (key.equals("Credit Expiry Date")) {
                    expiry = expiryDateFormatter.parse(value);
                    System.out.println("Expires at " + expiry);
                } else if (key.equals("Data")) {
                    String[] a = value.split(" ");
                    if (a[1].equals("Mbytes")) {
                        remainingData = new Value(Long.parseLong(a[0]) * 1000L * 1000L, Unit.BYTE);
                    } else {
                        throw new AccountUpdateException("Unknown units of " + a[1], null);
                    }
                } else if (key.equals("SMS")) {
                    String[] a = value.split(" ");
                    if (a[1].equals("messages")) {
                        remainingSMS = new Value(Integer.parseInt(a[0]), Unit.COUNT);
                    } else {
                        throw new AccountUpdateException("Unknown units of " + a[1], null);
                    }
                } else {
                    System.out.println("Unknown key " + key);
                }
            }
            
            account.setProperty(REMAINING_DATA, remainingData.getPrefValue());
            account.setProperty(REMAINING_SMS, remainingSMS.getPrefValue());
            

            startTable = loginResult.indexOf("List of mobile numbers on account with associated charges\">", endTable);
            if (startTable == -1) {
                throw new AccountUpdateException("Could not fund account numbers start", null);
            }
            endTable = loginResult.indexOf("</table>", startTable);
            tableStr = loginResult.substring(startTable, endTable + 8);

            

            
            valueMatcher = Pattern.compile("<td class=\"tableBody\">([^<]*)</td>");
            m = valueMatcher.matcher(tableStr);
            while ((row = getValues(3, m, 1)) != null) {
                String number = row[0];
                String name = row[1];
                String planName = row[2];
                
                ServiceIdentifier accountNumber = new ServiceIdentifier(provider.getName(), number);
                
                ServiceUpdateDetails service = new ServiceUpdateDetails(accountNumber);

                Plan plan = new Plan();
                plan.setName(planName);
                
                service.setPlan(plan);
                service.setProperty("Name", name);
                result.add(service);
            }
            
            return result;
        } catch (IOException ex) {
            throw new AccountUpdateException("Error logging in", ex);
        } catch (ParseException ex) {
            throw new AccountUpdateException("Error logging in", ex);
        }
    }
    
    String[] getValues(int num, Matcher m, int group) throws IOException {
        String[] result = new String[num];
        
        for (int i = 0; i < num; i++) {
            if (!m.find()) {
                if (i == 0) {
                    return null;
                }
                else 
                    throw new IOException("Expected another value, but there was not");
            }
            result[i] = m.group(group);
        }
        return result;
    }
    
    

    public void fetchServiceDetails(Service service) throws AccountUpdateException, InterruptedException {
        try {
            
            MetricGroup dataGroup = new MetricGroup(service, DATA_GROUP, Unit.BYTE, CounterStyle.SIMPLE);
            dataGroup.setGraphTypes(UsageGraphType.MONTHLY_USAGE);
            dataGroup.setStyle(CounterStyle.SIMPLE);
        
            MeasuredValue dataMval = new MeasuredValue(Unit.BYTE);
            dataMval.setName(USAGE);
            dataMval.setAmount(new Value(service.getAccount().getProperty(REMAINING_DATA)));
            dataGroup.setComponents(Collections.singletonList(dataMval));
            service.addMetricGroup(dataGroup);

            
            MetricGroup smsGroup = new MetricGroup(service, SMS_GROUP, Unit.COUNT, CounterStyle.SIMPLE);
            smsGroup.setGraphTypes(UsageGraphType.MONTHLY_USAGE);
            smsGroup.setStyle(CounterStyle.SIMPLE);
            MeasuredValue smsMval = new MeasuredValue(Unit.COUNT);
            smsMval.setName(USAGE);
            smsMval.setAmount(new Value(service.getAccount().getProperty(REMAINING_SMS)));
            smsGroup.setComponents(Collections.singletonList(smsMval));
            service.addMetricGroup(smsGroup);
            
            
            

            
            
            
            
            HttpPost usageGet = new HttpPost("https://secure.broadband.vodafone.com.au/CRMVOD/usage");
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("usageType", "0"));   // All Usage
            formparams.add(new BasicNameValuePair("mobileNumber", service.getIdentifier().getAccountNumber()));
            formparams.add(new BasicNameValuePair("billNumber", "0"));  // Current
            formparams.add(new BasicNameValuePair("buttonGo.x", "0"));  // Why do we care upon which pixel the button was pressed again?
            formparams.add(new BasicNameValuePair("buttonGo.y", "0"));
            formparams.add(new BasicNameValuePair("submit", "buttonGo"));
            
            usageGet.setEntity(new UrlEncodedFormEntity(formparams, "UTF-8"));
            HttpResponse resp = executeThenCheckIfInterrupted(usageGet);
    
            if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                throw new IOException("Expected OK from usage fetch: " + resp.getStatusLine());
            
            String usageStr = EntityUtils.toString(resp.getEntity());
            
            Pattern valueMatcher = Pattern.compile("<td\\s+class=\"tableBody\"\\s*>([^<]*)</td>");
            Matcher m = valueMatcher.matcher(usageStr);
            String[] row;
            while ((row = getValues(7, m, 1)) != null) {
                String type = row[0];
                
                int startOfBullshit = row[1].indexOf("00:00:00");
                Date date = expiryDateFormatter.parse(row[1].substring(0,startOfBullshit) + row[2] + ":00" + row[1].substring(startOfBullshit+8));
                
                String destination= row[3];
                String classification = row[4];
                
                Value amt;
                if (row[5].equals("-")) {
                     amt = new Value(1, Unit.COUNT);
                } else {
                    amt = new Value((long) (Double.valueOf(row[5])*1000*1000), Unit.BYTE);
                }
                Value cost = new Value((long) (Double.valueOf(row[6].replace("&nbsp;", ""))*100), Unit.CENT);
                
                UsageRecord r = new UsageRecord(date, amt);
                r.setCost(cost);
                r.setDescription(destination);
                
                if (type.equals("Data/GPRS")) {
                    dataMval.addUsageRecord(r);
                } else if (type.equals("SMS")) {
                    smsMval.addUsageRecord(r);
                }
            }
            
            
        } catch (ParseException ex) {
            throw new AccountUpdateException("Error logging in", ex);
        } catch (IOException ex) {
            throw new AccountUpdateException("Error logging in", ex);
        }
    } 

    public void testUsernameAndPassword(Account account) throws AccountUpdateException, WrongPasswordException {
        try {
            login(account);
        } catch (InterruptedException e) {
            // Unlikely this will happen, but re-throw it anyway, otherwise its
            // an attack option
            throw new AccountUpdateException("Interrupted", e);
        }

    }

}
