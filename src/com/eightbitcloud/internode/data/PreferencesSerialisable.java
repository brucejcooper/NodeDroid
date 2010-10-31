package com.eightbitcloud.internode.data;

import org.json.JSONException;
import org.json.JSONObject;


public interface PreferencesSerialisable {
    public void writeTo(JSONObject obj) throws JSONException;
    public void readFrom(JSONObject obj) throws JSONException;
}
