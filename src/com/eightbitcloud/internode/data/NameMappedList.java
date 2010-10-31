package com.eightbitcloud.internode.data;

import java.util.ArrayList;

@SuppressWarnings("serial")
public class NameMappedList<I  extends NamedThing> extends ArrayList<I>{

    public I getItemNamed(String name) {
        for (I g: this) {
            if (g.getName().equals(name))
                return g;
        }
        return null;
    }
    
    @Override
    public boolean add(I toAdd) {
        if (getItemNamed(toAdd.getName()) != null) {
            throw new IllegalArgumentException("Already has an object with that name");
        }
        return super.add(toAdd);
    }
}
