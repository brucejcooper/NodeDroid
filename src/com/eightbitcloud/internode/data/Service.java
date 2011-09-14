package com.eightbitcloud.internode.data;

import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.eightbitcloud.internode.PreferencesSerialiser;
import com.eightbitcloud.internode.provider.ServiceUpdateDetails;


/**
 * Representation of an account's ussage.  This should be accessed through the AccountProvider Content Provider.
 * @author bruce
 *
 */
public class Service {
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.eightbitcloud.internode.service";
    public static final String ITEM_TYPE = "vnd.android.cursor.item/vnd.eightbitcloud.internode.service";

    public static final String ID = "_id";
    public static final String SERVICE_PROVIDER = "SERVICE_PROV";
    public static final String SERVICE_ID = "SERVICE_ID";
    public static final String ACCOUNT_ID = "ACCOUNT";
    public static final String DATA = "DATA";
    public static final String UPDATED = "UPDATED";
    public static final String STATUS = "STATUS";
    
    
    private static final String MG = "mg";
    private static final String PLAN2 = "plan";
    private static final String TYPE = "type";
    
    private int id;
    private int accountID;
    private ServiceIdentifier identifier;
    private Date lastUpdate;
    private UpdateStatus updateStatus = UpdateStatus.IDLE;

    
    private Plan plan;
    private NameMappedList<MetricGroup> metricGroups = new NameMappedList<MetricGroup>();
    private ServiceType serviceType = ServiceType.MONTHLY_QUOTA_WITH_EXCESS;
    
    
    public Service() {
        
    }
    
    public Service(Cursor cursor) {
        int idColumn = cursor.getColumnIndex(ID);
        int serviceProviderColumn = cursor.getColumnIndex(SERVICE_PROVIDER);
        int serviceIdColumn = cursor.getColumnIndex(SERVICE_ID);
        int accountIDColumn = cursor.getColumnIndex(ACCOUNT_ID);
        int updateColumn = cursor.getColumnIndex(UPDATED);
        int statusColumn = cursor.getColumnIndex(STATUS);
        int dataColumn = cursor.getColumnIndex(DATA);

        id = cursor.getInt(idColumn);
        identifier = new ServiceIdentifier(cursor.getString(serviceProviderColumn), cursor.getString(serviceIdColumn));
        accountID = cursor.getInt(accountIDColumn);
        
        long l = cursor.getLong(updateColumn);
        lastUpdate = l == -1 ? null : new Date(l);
        updateStatus = UpdateStatus.values()[cursor.getInt(statusColumn)];
        
        // Plan, serviceType and metricGroups come from Data
        try {
            readFrom(new JSONObject(cursor.getString(dataColumn)));
        } catch (JSONException e) {
            Log.e("service", "Error procesisng JSON", e);
            throw new IllegalArgumentException("Error JSON processing");
        }
    }
    
    
    public Service(Account account, ServiceUpdateDetails sud) {
        setAccountID(account.getId());
        identifier = sud.getIdentifier();
        plan = sud.getPlan();
        
    }
    
    
    public ContentValues getValues() {
        ContentValues v = new ContentValues();
        if (id != 0) {
            v.put(ID, id);
        }
        v.put(ACCOUNT_ID, accountID);
        v.put(SERVICE_PROVIDER, identifier.getProvider());
        v.put(SERVICE_ID, identifier.getAccountNumber());
        v.put(UPDATED, lastUpdate == null ? -1L : lastUpdate.getTime());
        v.put(STATUS, updateStatus.ordinal());
        
        JSONObject obj = new JSONObject();
        try {
            writeTo(obj);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Error JSON processing");
        }
        v.put(DATA, obj.toString());
        return v;
    }

    
    
    public int getAccountID() {
        return accountID;
    }

    public void setAccountID(int accountID) {
        this.accountID = accountID;
    }

    
    
    public void setMetricGroups(List<MetricGroup> metricGroups) {
        this.metricGroups.clear();
        for (MetricGroup g: metricGroups) {
            addMetricGroup(g);
        }
    }
    public void addMetricGroup(MetricGroup g) {
        metricGroups.add(g);
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
    
    public ServiceType getServiceType() {
        return serviceType;
    }
    
    public void setServiceType(ServiceType type) {
        this.serviceType = type;
    }

    public MetricGroup getMetricGroup(String metricGroupName) {
        return metricGroups.getItemNamed(metricGroupName);
    }
    
    public List<MetricGroup> getAllMetricGroups() {
        return metricGroups;
    }
    
    @Override
    public String toString() {
        return getIdentifier().toString();
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
    


    public Date getLastUpdate() {
        return lastUpdate;
    }


    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }


    public UpdateStatus getUpdateStatus() {
        return updateStatus;
    }


    public void setUpdateStatus(UpdateStatus updateStatus) {
        this.updateStatus = updateStatus;
    }



    public void writeTo(JSONObject obj) throws JSONException {
        obj.put(PLAN2, plan == null ? null : PreferencesSerialiser.createJSONRepresentation(plan));
        obj.put(TYPE, serviceType.toString());
        obj.put(MG, PreferencesSerialiser.createJSONRepresentation(metricGroups));
    }

    public void readFrom(JSONObject obj) throws JSONException {
        if (obj.has(PLAN2)) {
            this.plan = new Plan();
            this.plan.readFrom(obj.getJSONObject(PLAN2));
        }
        this.serviceType = obj.has(TYPE) ? ServiceType.valueOf(obj.getString(TYPE)) : ServiceType.MONTHLY_QUOTA_WITH_EXCESS;
        PreferencesSerialiser.populateJSONList(this.metricGroups = new NameMappedList<MetricGroup>(), MetricGroup.class, obj.getJSONArray(MG));
        for (MetricGroup mg: this.metricGroups) {
            mg.__setService(this);
        }
    }


}
