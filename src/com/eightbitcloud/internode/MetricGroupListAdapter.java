package com.eightbitcloud.internode;

import java.util.List;

import com.eightbitcloud.internode.R;
import com.eightbitcloud.internode.ServiceView.ViewHolder;
import com.eightbitcloud.internode.data.MetricGroup;
import com.eightbitcloud.internode.data.Provider;
import com.eightbitcloud.internode.data.ProviderStore;
import com.eightbitcloud.internode.data.Service;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.FrameLayout.LayoutParams;

public class MetricGroupListAdapter extends BaseAdapter {
    
    private Service service;
    private Typeface font;
    private LayoutInflater inflater;

    public MetricGroupListAdapter(Context ctx, Service service, Typeface font) {
        inflater = LayoutInflater.from(ctx);
        this.service = service;
        this.font = font;
    }
    
    public void setService(Service service) {
        this.service = service;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return service == null ? 0 : service.getMetricGroupCount();
    }

    @Override
    public MetricGroup getItem(int position) {
        List<MetricGroup> groups = service.getAllMetricGroups();
        return groups.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MetricGroup item = getItem(position);
        ViewHolder holder;
        
        if (convertView == null || ((ViewHolder) convertView.getTag()).style != item.getStyle()) {
            holder = new ViewHolder();
            holder.style = item.getStyle();
            convertView = inflater.inflate(holder.style.getLayoutId(), null);
            holder.name = (TextView) convertView.findViewById(R.id.metricgrouplabel);
            holder.name.setTypeface(font);
            convertView.setTag(holder);

            convertView.setLayoutParams(new ListView.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.name.setText(item.getName() + ':');
        
        switch (item.getStyle()) {
        case SIMPLE:
            if (holder.value == null) {
                holder.value = (TextView) convertView.findViewById(R.id.metricgroupsimplevalue);
            }
            holder.value.setText(item.getValue().toString());
            break;
        default:
            if (holder.graph == null) {
                holder.graph = (QuotaBarGraph) convertView.findViewById(R.id.metricgroupgraph);
                Provider prov = ProviderStore.getInstance().getProvider(service.getIdentifier().getProvider());
                holder.graph.setGraphColors(prov.getGraphColors());
            }
            holder.graph.setUsage(item.getComponents(), item.getAllocation());
            holder.graph.setTime(service.getPlan() == null ? 0.0f : (float) service.getPlan().getPercentgeThroughMonth(System.currentTimeMillis()));
            break;
        }
        return convertView;
    }
    
}
