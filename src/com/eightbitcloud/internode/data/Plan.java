package com.eightbitcloud.internode.data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.eightbitcloud.internode.PreferencesSerialiser;

public class Plan extends ThingWithProperties implements PreferencesSerialisable {
    private static final String EXTRAS2 = "extras";
    private static final String INTERVAL2 = "interval";
    private static final String COST2 = "cost";
    private static final String ROLLOVER = "rollover";
    private static final String DESCRIPTION2 = "description";
    private static final String NAME2 = "name";
    private String description;
    private String name;
    private Date rolloverDate;
    private Value cost;
    private PlanInterval interval;
    private List<String> extras = new ArrayList<String>();
    transient private Date cachedPreviousRollover;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNextRollover(Date date) {
        this.rolloverDate = date;
        this.cachedPreviousRollover = null;
    }

    public Date getNextRollover() {
        return rolloverDate;
    }

    public Date getPreviousRollover() {
        if (cachedPreviousRollover == null) {
            Date next = getNextRollover();
            if (next != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(next);
                cal.add(Calendar.MONTH, -1);
                cachedPreviousRollover = cal.getTime();
            }
        }
        return cachedPreviousRollover;
    }

    public long getBillingPeriodLength() {
        return getNextRollover().getTime() - getPreviousRollover().getTime();
    }

    public double getPercentgeThroughMonth(long now) {
        long timeElapsed = now - getPreviousRollover().getTime();
        return (timeElapsed / (double) getBillingPeriodLength());
    }

    public void setCost(Value cost) {
        assert cost.getUnit() == Unit.CENT;
        this.cost = cost;
    }

    public Value getCost() {
        return cost;
    }

    public void setInterval(PlanInterval valueOf) {
        interval = valueOf;
    }

    public PlanInterval getInterval() {
        return interval;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(cost).append(' ').append(name).append(" which rolls over on " + rolloverDate);
        return sb.toString();
    }

    public void addPlanExtra(String extra) {
        extras.add(extra);
    }

    public List<String> getPlanExtras() {
        return extras;
    }

    @Override
    public void writeTo(JSONObject obj) throws JSONException {
        super.writeTo(obj);
        obj.put(NAME2, name);
        obj.put(DESCRIPTION2, description);
        if (rolloverDate != null) {
            obj.put(ROLLOVER, rolloverDate.getTime());
        }
        if (cost != null) {
            obj.put(COST2, cost.getPrefValue());
        }
        obj.put(INTERVAL2, interval);
        obj.put(EXTRAS2, PreferencesSerialiser.createJSONRepresentationForStrings(extras));
    }

    @Override
    public void readFrom(JSONObject obj) throws JSONException {
        super.readFrom(obj);
        name = obj.getString(NAME2);
        description = obj.has(DESCRIPTION2) ? obj.getString(DESCRIPTION2) : null;
        if (obj.has(ROLLOVER)) {
            rolloverDate = new Date(obj.getLong(ROLLOVER));
        }
        
        if (obj.has(COST2)) {
            cost = new Value(obj.getString(COST2));
        }
        interval = obj.has(INTERVAL2) ? PlanInterval.valueOf(obj.getString(INTERVAL2)) : null;
        extras = PreferencesSerialiser.createStringArray(obj.getJSONArray(EXTRAS2));
    }

}
