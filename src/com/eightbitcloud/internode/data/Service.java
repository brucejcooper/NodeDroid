package com.eightbitcloud.internode.data;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.eightbitcloud.internode.PreferencesSerialiser;


/**
 * This, and everything inside it, is serialisable so that we can write it out
 * to preferences for storage.  The Accounts themselves aren't serialisable, as
 * we want to be able to make changes to the data structures without data
 * compatability problems.  In the case of a Service being incompatible, we 
 * ignore the error and regenerate the data from the provider instead.
 * 
 * As a result, account is transient, so that it doesn't get serialised.
 * 
 * @author bruce
 *
 */
public class Service extends ThingWithProperties implements PreferencesSerialisable {
    
    private static final String MG = "mg";
    private static final String PLAN2 = "plan";
    private static final String IDACCT = "idacct";
    private static final String IDPROV = "idprov";
    private transient Account account;
    private ServiceIdentifier identifier;
    private Plan plan;
    private NameMappedList<MetricGroup> metricGroups = new NameMappedList<MetricGroup>();

    
    public void setMetricGroups(List<MetricGroup> metricGroups) {
        this.metricGroups.clear();
        for (MetricGroup g: metricGroups) {
            addMetricGroup(g);
        }
    }
    public void addMetricGroup(MetricGroup g) {
        metricGroups.add(g);
    }
    public Account getAccount() {
        return account;
    }
    public void setAccount(Account account) {
        this.account = account;
    }
    public ServiceIdentifier getIdentifier() {
        return identifier;
    }
    public void setIdentifier(ServiceIdentifier identifier) {
        this.identifier = identifier;
    }
    public Plan getPlan() {
        return plan;
    }
    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public MetricGroup getMetricGroup(String metricGroupName) {
        return metricGroups.getItemNamed(metricGroupName);
    }
    
    public List<MetricGroup> getAllMetricGroups() {
        return metricGroups;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("[Service ").append(getIdentifier()).append('/').append(" on plan ").append(plan).append(". Groups: ").append(metricGroups).append("]");
        
        return sb.toString();
    }
    public int getMetricGroupCount() {
        
       return metricGroups.size();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Service other = (Service) obj;
        if (identifier == null) {
            if (other.identifier != null)
                return false;
        } else if (!identifier.equals(other.identifier))
            return false;
        return true;
    }
    
    @Override
    public void writeTo(JSONObject obj) throws JSONException {
        super.writeTo(obj);
        obj.put(IDPROV, identifier.getProvider());
        obj.put(IDACCT, identifier.getAccountNumber());
        obj.put(PLAN2, plan == null ? null : PreferencesSerialiser.createJSONRepresentation(plan));
        obj.put(MG, PreferencesSerialiser.createJSONRepresentation(metricGroups));
    }

    @Override
    public void readFrom(JSONObject obj) throws JSONException {
        super.readFrom(obj);
        this.identifier = new ServiceIdentifier(obj.getString(IDPROV), obj.getString(IDACCT));
        this.plan = new Plan();
        this.plan.readFrom(obj.getJSONObject(PLAN2));
        PreferencesSerialiser.populateJSONList(this.metricGroups = new NameMappedList<MetricGroup>(), MetricGroup.class, obj.getJSONArray(MG));
        for (MetricGroup mg: this.metricGroups) {
            mg.__setService(this);
        }
    }

}
