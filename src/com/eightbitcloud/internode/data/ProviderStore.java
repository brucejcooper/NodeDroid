package com.eightbitcloud.internode.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.eightbitcloud.internode.GraphColors;

import android.graphics.Color;

public class ProviderStore {

    private static ProviderStore store;
    Map<String,Provider> providers = new HashMap<String,Provider>();
    
    public ProviderStore() {
        // TODO make this come from an xml file.
        Provider internodeProv = new Provider();
        internodeProv.setName("Internode");
        internodeProv.setLogoURL("http://www.internode.on.net/images/base/internode_logo.gif");
        internodeProv.setUrl("http://www.internode.on.net");
        internodeProv.setTextColour("#ffffff");
        internodeProv.setBackgroundResource("inodeheader");
        internodeProv.setGraphColors(new GraphColors(0xFF166d6e, 0xFFf47836, 0xff43be6d));
        addProvider(internodeProv);

        Provider optusProv = new Provider();
        optusProv.setName("Optus");
        optusProv.setLogoURL("http://www.optus.com.au/home/assets/images/shared/logo-yes-optus.gif");
        optusProv.setUrl("http://www.optus.com.au/");
        optusProv.setTextColour("#006685");        
        optusProv.setGraphColors(new GraphColors(0xff000000, 0xff006685, 
//                0xffFFE77F, 0xff674C99, 0xff807299, 0xff807299, 0xffFFD000
                0xffFF9D00,
                0xffF6FF00,  0xffA97E38,
                0xffFFCE7F, 0xffFFD000          
        ));
        optusProv.setBackgroundResource("optusheader"); //#ffd100
        addProvider(optusProv);

    }
    
    private static Map<String,Integer> createSingleColorMap(int color) {
        return Collections.singletonMap(null, color);
        
    }
    
    public static ProviderStore getInstance() {
        if (store == null) {
            store = new ProviderStore();
            
        }
        return store;
    }
    
    
    public void addProvider(Provider prov) {
        providers.put(prov.getName().toLowerCase(), prov);
    }


    public Provider getProvider(String name) {
        return providers.get(name.toLowerCase());
    }
    
    public Collection<Provider> getAllProviders() {
        List<Provider> result = new ArrayList<Provider>(providers.values());
        Collections.sort(result, new Comparator<Provider>() {
            public int compare(Provider object1, Provider object2) {
                return object1.getName().compareTo(object2.getName());
            }
        });
        return result;
    }
}
