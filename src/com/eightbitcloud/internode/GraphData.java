package com.eightbitcloud.internode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.eightbitcloud.internode.data.Value;

public class GraphData<T,D extends Value> {

    List<T> xaxisData;
    Map<T,D[]> data;
    GraphStyle style;
    KeyFormatter formatter;
    
    public GraphData(List<T> xaxis, Map<T,D[]> data, GraphStyle style, KeyFormatter formatter) {
        this.xaxisData = xaxis;
        this.data = data;
        this.formatter = formatter;
        this.style = style;
    }
    
    public GraphData(Map<T,D[]> data, GraphStyle style, KeyFormatter formatter) {
        this(new ArrayList<T>(data.keySet()), data, style, formatter);
    }
}