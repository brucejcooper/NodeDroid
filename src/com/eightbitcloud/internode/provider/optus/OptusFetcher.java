package com.eightbitcloud.internode.provider.optus;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.util.Log;

import com.eightbitcloud.internode.NodeUsage;
import com.eightbitcloud.internode.UsageGraphType;
import com.eightbitcloud.internode.data.Account;
import com.eightbitcloud.internode.data.CounterStyle;
import com.eightbitcloud.internode.data.MeasuredValue;
import com.eightbitcloud.internode.data.MetricGroup;
import com.eightbitcloud.internode.data.Plan;
import com.eightbitcloud.internode.data.Provider;
import com.eightbitcloud.internode.data.Service;
import com.eightbitcloud.internode.data.Unit;
import com.eightbitcloud.internode.data.UsageRecord;
import com.eightbitcloud.internode.data.Value;
import com.eightbitcloud.internode.provider.AccountUpdateException;
import com.eightbitcloud.internode.provider.ProviderFetcher;
import com.eightbitcloud.internode.provider.WrongPasswordException;
import com.eightbitcloud.internode.util.DateTools;

public class OptusFetcher implements ProviderFetcher {
    public static final String CAP_GROUP = "Cap";
    public static final String FREE_DATA_GROUP = "Free Data";
    public static final String FREE_CALLS_GROUP = "Free Calls";
    public static final String DATA_PACK_GROUP = "Data";
    private static final String USAGE_URL = "UsageURL";
    private static final String SMAGENTKEY = "<input type=hidden name=smagentname value=";
    private static final BigDecimal GST_MULTIPLIER = new BigDecimal("1.1");
    private static final String EX_CAP = "Other";
    private HttpClient httpClient;
    public DateFormat rolloverFormatter = new SimpleDateFormat("dd/MM/yyyy");

    public DateFormat eventTimeFormatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss aa");

    public TimeZone melbourneTZ;
    private Provider provider;


    
    public OptusFetcher(Provider provider) {
        this.provider = provider;
        httpClient = new DefaultHttpClient();
        melbourneTZ = TimeZone.getTimeZone("GMT+1000");
        rolloverFormatter.setTimeZone(melbourneTZ);
    }
    
    private HttpResponse executeThenCheckIfInterrupted(HttpRequestBase m) throws InterruptedException, ClientProtocolException, IOException {
        HttpResponse resp = httpClient.execute(m);
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
        return resp;
    }

    public void fetchServices(Account account) throws AccountUpdateException, InterruptedException {
        try {
            HttpGet loginPage = new HttpGet("http://www.optus.com.au/login");
            HttpResponse resp = executeThenCheckIfInterrupted(loginPage);
            if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                throw new IOException("Expected OK Response");
            String body = EntityUtils.toString(resp.getEntity());
            int pos = body.indexOf(SMAGENTKEY);
            if (pos == -1) {
                throw new IOException("Couldn't find key");
            }
            int closePos = body.indexOf('"', pos+SMAGENTKEY.length()+1);
            String key = body.substring(pos + SMAGENTKEY.length() + 1, closePos);
            
            
            //<input type=hidden name=smagentname value="Hp78kFUMHCYddNe1TJmGKxT57SuEDCMoLqqOzRBGImr20+txe1ylGUCySYZ/CvHj">
    
            // TODO should extract post location from previous body.
            HttpPost loginPost = new HttpPost("https://my.optus.com.au/signon/Optus/login_ext.sec");
    
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("SMENC", "ISO-8859-1"));
            formparams.add(new BasicNameValuePair("SMLOCALE", "US-EN"));
            formparams.add(new BasicNameValuePair("target", "HTTPS://my.optus.com.au/web/oscportal.portal?_nfpb=true&_pageLabel=myaccount&site=personal"));
            formparams.add(new BasicNameValuePair("smauthreason", "0"));
            formparams.add(new BasicNameValuePair("smagentname", key));
            formparams.add(new BasicNameValuePair("postpreservationdata", ""));
            formparams.add(new BasicNameValuePair("USER", account.getUsername()));
            formparams.add(new BasicNameValuePair("PASSWORD", account.getPassword()));
    
            loginPost.setEntity(new UrlEncodedFormEntity(formparams, "UTF-8"));
            resp = executeThenCheckIfInterrupted(loginPost);
        
            if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                throw new IOException("Expected OK from login: " + resp.getStatusLine());
            
            String loginResult = EntityUtils.toString(resp.getEntity());
            
            
            
            Pattern accountNumberPattern = Pattern.compile("<div\\s+class=\"prodcut_number\">([^<]*)</div>");
            Pattern accountOwnerPattern = Pattern.compile("<div\\s+class=\"account_owner_name\">\\s*Account Name:([^<]*)</div>");
            Pattern accountAddressPattern = Pattern.compile("<div\\s+class=\"account_owner_address\">\\s*Account Address:([^<]*)</div>");
            
            Matcher m = accountNumberPattern.matcher(loginResult);
            if (!m.find())
                throw new IOException("Couldn't find account number");
            String accountNumber = cleanupSpaces(m.group(1));

            Matcher m2 = accountOwnerPattern.matcher(loginResult);
            if (!m2.find(m.end()))
                throw new IOException("Couldn't find account Owner");
            String owner = cleanupSpaces(m2.group(1));

            Matcher m3 = accountAddressPattern.matcher(loginResult);
            if (!m3.find(m2.end()))
                throw new IOException("Couldn't find account address");
            String address = cleanupSpaces(m3.group(1));
            
            account.setProperty("Owner", owner);
            account.setProperty("AccountNumber", accountNumber);
            account.setProperty("Address", address);

            
            
            // We need to cut it up between the items 
            Pattern itemSeparatorPattern = Pattern.compile("<li class=\"service_item [^\"]*\">");
            List<String> itemSections = cutUp(loginResult, itemSeparatorPattern, m3.end());
            
            for (String section: itemSections) {
                // Get the Phone number.  
                // TODO technically there could be multiple of these, and it might be possibel to have non-digits.
                Pattern phoneNumberPattern = Pattern.compile("<ol class=\"service_items\">\\s*<li>\\s*<div>\\s*([0-9]+)\\s*</div>\\s*</li>");
                Matcher phoneNumberMatcher = phoneNumberPattern.matcher(section);
                if (!phoneNumberMatcher.find())
                    throw new IOException("Expected to find service items....");
                String phoneNumber = phoneNumberMatcher.group(1);
                
                
                Pattern usageURLPattern = Pattern.compile("<div class=\"account_usage\">\\s*<a href=\"([^\"]+)\">");
                Matcher usageURLMatcher = usageURLPattern.matcher(section);
                if (!usageURLMatcher.find())
                    throw new IOException("Expected to find url for usage");
                String usageURL = prependHost(usageURLMatcher.group(1));
                
                Service service = account.getService(phoneNumber);
                if (service == null) {
                    service = new Service();
                    service.setIdentifier(phoneNumber);
                    service.setProperty(USAGE_URL, usageURL);
                    account.addService(service);
                }
            }
            
        } catch (IOException ex) {
            throw new AccountUpdateException("Error logging in", ex);
        }
    }
    
    

    Pattern usageSummaryPattern = Pattern.compile("([0-9]+(.[0-9]+)?) ([MKG])B");
    Pattern SMSCountPattern = Pattern.compile("([0-9]+) SMS");
    Pattern callCountPattern = Pattern.compile("([0-9]+) Calls?");
    Pattern amountPattern = Pattern.compile("\\$([0-9]+(\\.[0-9]{2,3})?)");


    private Value parseValue(String amt) throws IOException {
        Matcher m = usageSummaryPattern.matcher(amt);
        if (m.matches()) {
            char mult = m.group(3).charAt(0);
            BigDecimal bd = new BigDecimal(m.group(1));
            switch (mult) {
            case 'K':
                bd = bd.multiply(new BigDecimal(1000));
                break;
            case 'M':
                bd = bd.multiply(new BigDecimal(1000 * 1000));
                break;
            case 'G':
                bd = bd.multiply(new BigDecimal(1000 * 1000 * 1000));
                break;
            default:
                throw new IOException("Unknown data multiplier " + mult);
            }
            return new Value(bd.longValue(), Unit.BYTE);
        }

        m = amountPattern.matcher(amt);
        if (m.matches()) {
            return parseAmount(m, 1, true);
        }

        
        m = SMSCountPattern.matcher(amt);
        if (m.matches()) {
            return new Value(Long.parseLong(m.group(1)), Unit.COUNT);
        }
        m = callCountPattern.matcher(amt);
        if (m.matches()) {
            return new Value(Long.parseLong(m.group(1)), Unit.COUNT);
        }
        throw new IOException("Unkknown pattern for summary value " + amt);
    }

    private Value parseAmount(Matcher m, int index, boolean multiplForGST) {
        BigDecimal bd = new BigDecimal(m.group(index)).multiply(new BigDecimal(100));
        if (multiplForGST) {
            bd = bd.multiply(GST_MULTIPLIER);
        }
        bd.setScale(0, BigDecimal.ROUND_HALF_EVEN);
        Value amt = new Value(bd.longValue(), Unit.CENT);
        return amt;

    }

    public void updateService(Service service) throws AccountUpdateException, InterruptedException {
        String usageDoc = null;
        try {
            HttpGet intermediatePage = new HttpGet((String)service.getProperty(USAGE_URL));
            HttpResponse resp = executeThenCheckIfInterrupted(intermediatePage);
            
            if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                throw new IOException("Expected OK Response");
    
            String intermediateDoc = EntityUtils.toString(resp.getEntity());
            
            Pattern urlPattern = Pattern.compile("<a href=\"([^\"]+)\">\\s*" + service.getIdentifier() + "\\s*</a>");
            Matcher urlMatcher = urlPattern.matcher(intermediateDoc);
            if (!urlMatcher.find())
                throw new IOException("Couldn't find url for real usage");
            String realURL = prependHost(urlMatcher.group(1));
            
            HttpGet usagePage = new HttpGet(realURL);
            resp = executeThenCheckIfInterrupted(usagePage);
            
            if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                throw new IOException("Expected OK Response");
    
            usageDoc = EntityUtils.toString(resp.getEntity());
            
            
            Pattern serviceNumberPattern = Pattern.compile("<td>\\s*Service\\s+Number:[^<]*</td>\\s*<td>\\s*<strong>\\s*([0-9]+)\\s*</strong>");
            Matcher serviceNumberMatcher = serviceNumberPattern.matcher(usageDoc);
            if (!serviceNumberMatcher.find())
                throw new IOException("Couldn't find service Number");
            String serviceNumber = serviceNumberMatcher.group(1);
            
//            System.out.println("Service number is " + serviceNumber); // Kind of redundant
            
            Pattern planPattern = Pattern.compile("<td VALIGN=\"TOP\">\\s*Plan:[^<]*</td>\\s*<td>\\s*<strong>\\s*([^<]+)\\s*</strong>([^<]+)</td>");
            Matcher planMatcher = planPattern.matcher(usageDoc);
            if (!planMatcher.find(serviceNumberMatcher.end()))
                throw new IOException("Couldn't find Plan");
            String planName = cleanupSpaces(planMatcher.group(1));
            String planExt = cleanupSpaces(planMatcher.group(2));
            
            
            // TODO assumes a CAP
            ensureGroup(CAP_GROUP, service, Unit.CENT, CounterStyle.CAP, UsageGraphType.BREAKDOWN, UsageGraphType.MONTHLY_USAGE);
            ensureGroup(DATA_PACK_GROUP, service, Unit.BYTE, CounterStyle.CAP, UsageGraphType.MONTHLY_USAGE);
            ensureGroup(FREE_DATA_GROUP, service, Unit.BYTE, CounterStyle.SIMPLE, UsageGraphType.MONTHLY_USAGE);

            
            Plan plan = new Plan();
            plan.setName(planName);
            plan.setDescription(planExt);
            
            Pattern costPattern = Pattern.compile("\\$([0-9]+)(.[0-9]{2})?");
            Matcher costMatcher = costPattern.matcher(planName);
            if (!costMatcher.find()) {
                throw new IOException("Couldn't find plan cost in plan name");
            }
            plan.setCost(parseAmount(costMatcher, 1, false));
            
            Pattern capSizePattern = Pattern.compile("\\$([0-9]+(.[0-9]{2})?)\\s+\\(\\$([0-9]+(.[0-9]{2})?) ex GST\\) of cap");
            Matcher capSizeMatcher = capSizePattern.matcher(planExt);
            
            if (!capSizeMatcher.find())
                throw new IOException("Couldn't find cap size");
            service.getMetricGroup(CAP_GROUP).setAllocation(parseAmount(capSizeMatcher, 1, false));
        
            
            
            Pattern boltOnPattern = Pattern.compile("Bolt-on:\\s+</td>\\s*<td\\s+colspan=\"2\"\\s+align=\"left\">\\s*<table>");
            Matcher boltOnMatcher = boltOnPattern.matcher(usageDoc);
            if (!boltOnMatcher.find(planMatcher.end()))
                throw new IOException("Expected to find Bolt Ons");

            int endOfTable = usageDoc.indexOf("</table>", boltOnMatcher.end()+1);
            Pattern p2 = Pattern.compile("\\s*<tr>\\s*<td>([^<]*)</td>\\s*</tr>");
            Matcher m2 = p2.matcher(usageDoc);
            m2.region(boltOnMatcher.end(), endOfTable);

            Pattern dataPattern = Pattern.compile("([0-9]+)([MG])B of included data");
            while (m2.find()) {
                String extra = m2.group(1);
                
                plan.addPlanExtra(extra);
                Matcher m = dataPattern.matcher(extra);
                if (m.matches()) {
                    long amt = Long.parseLong(m.group(1));
                    if (m.group(2).equals("M")) {
                        amt *= 1000 * 1000;
                    } else {
                        amt *= 1000 * 1000 * 1000;
                    }
                    Value quota = new Value(amt, Unit.BYTE);
                    service.getMetricGroup(DATA_PACK_GROUP).setAllocation(quota);
                }
            }
            
            
            service.setPlan(plan);

            
    
            Pattern monthStartPattern = Pattern.compile("<td>\\s*Date From:\\s+</td>\\s*<td>\\s*<input\\s+size=\"[0-9]+\"\\s*type=\"[a-z]+\"\\s*name=\"fromDate\"\\s*value=\"([0-9]+/[0-9]+/[0-9]+)\">");
            Matcher monthStartMatcher = monthStartPattern.matcher(usageDoc);
            if (!monthStartMatcher.find(planMatcher.end()))
                throw new IOException("Couldn't find Start Date");
            String startDateStr = cleanupSpaces(monthStartMatcher.group(1));
            Date startDate = DateTools.ensureMidnight(rolloverFormatter.parse(startDateStr), melbourneTZ);
            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);
            cal.add(Calendar.MONTH,1);
            
            plan.setNextRollover(cal.getTime());
            
            
            
            int start = usageDoc.indexOf("<table class=\"usagesummary\">", monthStartMatcher.end());
            int end = usageDoc.indexOf("</table>", start+20);
            
            Pattern summaryRowPattern = Pattern.compile("<tr><td[^>]*>([^<]*)</td><td[^>]*>([^<]*)</td><td[^>]*>([^<]*)</td></tr>");
            Matcher summaryRowMatcher = summaryRowPattern.matcher(usageDoc);
            summaryRowMatcher.region(start, end);
            
            
            while (summaryRowMatcher.find()) {
                String metric = summaryRowMatcher.group(1);
                String amt = summaryRowMatcher.group(2);
                Value parsedAmount = parseValue(amt);
                Value cost = parseValue(summaryRowMatcher.group(3));
                if (cost.getUnit() != Unit.CENT) {
                    throw new IOException("cost isn't a cost");
                }
                
                String group = inferGroupForMetric(metric);
                
                MetricGroup mg = service.getMetricGroup(group);
                if (mg == null) {
                    ensureGroup(group, service, Unit.CENT, CounterStyle.SIMPLE, UsageGraphType.BREAKDOWN, UsageGraphType.MONTHLY_USAGE);
                    mg = service.getMetricGroup(group);
                }
                MeasuredValue comp = mg.getComponent(metric);
                if (comp == null) {
                    comp = new MeasuredValue(mg.getAllocation().getUnit());
                    comp.setName(metric);
                    mg.addComponent(comp);
                }
                
                if (mg.getAllocation().getUnit() == Unit.CENT) {
                    comp.setAmount(cost);
                } else {
                    comp.setAmount(parsedAmount);
                }
                
                
            }
            
            Pattern totalPattern = Pattern.compile("<td class=\"footer\">([0-9$\\.]+)</td></tr>");
            Matcher totalMatcher = totalPattern.matcher(usageDoc);
            totalMatcher.region(start, end);
            if (!totalMatcher.find())
                throw new IOException("Couldn't find total");
            
//            System.out.println("Total is " + totalMatcher.group(1));
            
            
            
            // Now get the individual day tables
            Pattern transactionPattern = Pattern.compile("<tr><td[^>]*>([^<]*)</td><td[^>]*>([^<]*)</td><td[^>]*>([^<]*)</td><td[^>]*>([^<]*)</td><td[^>]*>([^<]*)</td></tr>");
            Matcher transactionPatternMatcher = transactionPattern.matcher(usageDoc);
            start = usageDoc.indexOf("<table class=\"unbilledUsageDetail\">");
    
            Pattern datePattern = Pattern.compile("<tbody id=\"([0-9]+/[0-9]+/[0-9]+)\">");
            Matcher datePatternMatcher = datePattern.matcher(usageDoc);
    
            
            while (start != -1) {
                end = usageDoc.indexOf("</table>", start+20);
                transactionPatternMatcher.region(start,end);
                datePatternMatcher.region(start,end);
                
                if (!datePatternMatcher.find())
                    throw new IOException("couldn't find date");
                String date = datePatternMatcher.group(1);
                
                
                while (transactionPatternMatcher.find()) {
                    Date time = eventTimeFormatter.parse(date + ' ' + transactionPatternMatcher.group(1));
                    String destination = transactionPatternMatcher.group(2);
                    String quantity = transactionPatternMatcher.group(4);
                    String originalType = transactionPatternMatcher.group(3);
                    String type = mapTXToValueName(originalType, quantity);
                    Value cost = parseValue(transactionPatternMatcher.group(5));


                    
                    MetricGroup group = service.getMetricGroup(inferGroupForMetric(type));
                    MeasuredValue value = group.getComponent(type);
                    if (value == null) {
                        Log.d(NodeUsage.TAG, "No group named " + type + " in group " + group + "(it has " + group.getComponents() +"), creating");
                        value = new MeasuredValue(group.getValue().getUnit());
                        value.setName(type);
                        group.addComponent(value);
                        
                    }
                    Log.d(NodeUsage.TAG, "REad " + time + "/" + destination + "/" + quantity + "/" + originalType + "(" +type + ")/"+ cost + ". Value is " + value.getName());

                    Matcher timeMatcher = timePattern.matcher(quantity);
                    Value quantityValue;
                    if (value.getUnits() == Unit.BYTE) {
                        quantityValue = new Value(Integer.parseInt(quantity)*1024, Unit.BYTE);
                    } else if (value.getUnits() == Unit.CENT) {
                        quantityValue = cost;
                    } else if (timeMatcher.matches()) {
                        long amt = Integer.parseInt(timeMatcher.group(1))*60*60 + 
                        Integer.parseInt(timeMatcher.group(2))*60 + 
                        Integer.parseInt(timeMatcher.group(3));
                         quantityValue = new Value(amt*1000, Unit.MILLIS);
                    } else {
                        quantityValue = new Value(Integer.parseInt(quantity), Unit.COUNT);
                    }
                    
                    
                    UsageRecord record = new UsageRecord(time, quantityValue);
                    record.setCost(cost);
                    record.setDescription(destination);
                    
                    value.addUsageRecord(record);
                }
                
            
                // Look for a new day...
                start = usageDoc.indexOf("<table class=\"unbilledUsageDetail\">", end);
            }
        } catch (ParseException ex) {
            throw new AccountUpdateException("Error logging in", ex);
        } catch (IOException ex) {
            Log.e(NodeUsage.TAG, "Error parsing results, doc is " + usageDoc);
            throw new AccountUpdateException("Error Parsing Results", ex);
        }
    }



    private void ensureGroup(String name, Service service, Unit unit, CounterStyle style, UsageGraphType... types) {
        MetricGroup group = service.getMetricGroup(name);
        if (group == null) {
            group = new MetricGroup(service, name, unit, style);
            group.setGraphTypes(types);
            service.addMetricGroup(group);
            
        }
    }
    
    Pattern timePattern = Pattern.compile("([0-9]{2}):([0-9]{2}):([0-9]{2})");
    Map<String,String> txToComponents;

    private String mapTXToValueName(String type, String quantity) {
        if (txToComponents == null) {
            txToComponents = new HashMap<String,String>();
            txToComponents.put("Mobile", "Mobile Call Charges");
            txToComponents.put("Optus SMS", "SMS - Text Messaging");
            txToComponents.put("DIV-VoiceMail", "Other");
            txToComponents.put("Freephone", "Mobile Call Charges");
            txToComponents.put("Handset Feature", "Mobile Call Charges");
            txToComponents.put("Voicemail", "Other");
            
            txToComponents.put("Internet", "Data - Mobile Internet");
            txToComponents.put("Social Internet", "Social Internet");

        
        }
        String result =  txToComponents.get(type);
        if (result == null) {
            // Assume all calls are included in cap.  TODO This is probably wrong
            if (timePattern.matcher(quantity).matches()) {
                result = "Mobile Call Charges";
            } else {
                Log.i(NodeUsage.TAG, "Can not find type " + type + " in map, and it isn't a time... so its something else.");
                result = "Other";
            }
        }
        return result;
            
    }

    
    
    private String inferGroupForMetric(String metric) {
        if (metric.equalsIgnoreCase("Data - Mobile Internet")) {
            return DATA_PACK_GROUP;
        } else if (metric.equalsIgnoreCase("Social Internet")) {
            return FREE_DATA_GROUP;
//        } else if (metric.equalsIgnoreCase("Other")) {
//            return FREE_CALLS_GROUP;
//        } else if (metric.equalsIgnoreCase("SMS-Text Messaging Int'l")) {
//            return EX_CAP;
        } else {
            return CAP_GROUP;
        }
    }

    private List<String> cutUp(String doc, Pattern itemSeparatorPattern, int searchStartPosition) throws IOException {
        List<String> result = new ArrayList<String>();
        Matcher itemMatcher = itemSeparatorPattern.matcher(doc);
        List<Integer> itemPositions = new ArrayList<Integer>();
        if (!itemMatcher.find(searchStartPosition))
            throw new IOException("Couldn't find service item");
        itemPositions.add(itemMatcher.start());
        
        while (itemMatcher.find(itemMatcher.end())) {
            itemPositions.add(itemMatcher.start());
        }
        itemPositions.add(doc.length());
        
        Iterator<Integer> it = itemPositions.iterator();
        int start = it.next();
        while (it.hasNext()) {
            int end = it.next();
            result.add(doc.substring(start, end));
            start = end;
        }
        return result;

    }

    private String cleanupSpaces(String group) {
        StringBuilder result = new StringBuilder();
        boolean spaceBeforeNextCharacter = false; // Deals with leading spaces
        boolean characterHasBeenOutput = false;
        for (int i = 0; i < group.length(); i++) {
            char ch = group.charAt(i);
            // TODO vulnerable to overflow
            boolean nbsp = ch == '&' && group.charAt(i+1) == 'n' &&group.charAt(i+2) == 'b' && group.charAt(i+3) == 's' && group.charAt(i+4) == 'p' && group.charAt(i+5) == ';'; 
            
            if (Character.isWhitespace(ch) || nbsp) {
                spaceBeforeNextCharacter = true;
                if (nbsp) {
                    i += 5;
                }
            } else {
                if (spaceBeforeNextCharacter && characterHasBeenOutput)
                    result.append(' ');
                characterHasBeenOutput = true;
                spaceBeforeNextCharacter = false;
                result.append(ch);
            }
        }
        return result.toString();
    }


    private String prependHost(String usageURL) {
        if (usageURL.startsWith("/")) {
            return "https://my.optus.com.au" + usageURL;
        }
        return usageURL;
    }

    public void testUsernameAndPassword(Account account) throws AccountUpdateException, WrongPasswordException {
        // TODO Does nothing at the moment!
        
    }


}
