package com.eightbitcloud.internode.data;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class Value implements Comparable<Value> {
    
    
    private long amt;
    private Unit unit;
    
    public static final long KILOBYTE = 1000;
    public static final long MEGABYTE = 1000 * KILOBYTE;
    public static final long GIGABYTE = 1000 * MEGABYTE;
    private static final NumberFormat f = new DecimalFormat("#.##");

    
    public Value(long amt, Unit unit) {
        this.amt = amt;
        this.unit = unit;
    }
    
    public Value(String prefsValue) {
        int pos = prefsValue.indexOf(' ');
        amt = Long.parseLong(prefsValue.substring(0, pos));
        unit = Unit.valueOf(prefsValue.substring(pos+1));
    }
    
    public String getPrefValue() {
        return new StringBuilder().append(amt).append(' ').append(unit).toString();
    }
    
    private void ensureCompatibleWith(Value toAdd) {
        if (toAdd.unit != this.unit) {
            throw new IncompatibleUnitsError();
        }
    }
    
    public Value plus(Value other) {
        ensureCompatibleWith(other);
        return new Value(this.amt + other.amt, this.unit);
    }

    public Value minus(Value other) {
        ensureCompatibleWith(other);
        return new Value(this.amt - other.amt, this.unit);
    }

    
    public Value negate() {
        return new Value(-amt, unit);
    }
    
    public Value divideByNumber(long amt) {
        return new Value(this.amt / amt, this.unit);
    }
    
    public double divideByValue(Value val) {
        if (val.getUnit() != getUnit())
            throw new IncompatibleUnitsError();
        return this.amt / (double)val.amt;
    }
    
    public long getAmt() {
        return amt;
    }
    public Unit getUnit() {
        return unit;
    }

    public int compareTo(Value another) {
        ensureCompatibleWith(another);
        long diff = this.amt = another.amt;
        return diff < 0 ? -1 : diff == 0 ? 0 : 1;
    }
    
    @Override
    public boolean equals(Object another) {
        if (!(another instanceof Value)) {
            return false;
        }
        Value vo = (Value) another;
        return unit == vo.unit && amt == vo.amt;
    }
    
    @Override
    public int hashCode() {
        return (int) amt;
    }
    
    
    public static String formatSize(long i) {
        double nGig = i / (double) GIGABYTE;
        if (nGig >= 1) {
            return f.format(nGig) + "G";
        } else {
            double nMeg = i/ (double) MEGABYTE; 
            return f.format(nMeg) + "M";
        }
    }

    
    @Override
    public String toString() {
        switch (unit) {
        case BYTE:  return formatSize(amt);
        case CENT:  return String.format("$%d.%02d", getAmt() / 100, getAmt() % 100);
        case COUNT: return Long.toString(amt);
        case MILLIS:
            long seconds = getAmt() / 1000;
            return String.format("%d:%02d", seconds / 60, seconds % 60);
        default:    return getAmt() + " " + unit;
        }
    }

    public boolean isGreaterThan(Value maxValue) {
        ensureCompatibleWith(maxValue);
        return getAmt() > maxValue.getAmt();
    }

    public static Value max(Value val1, Value val2) {
        val1.ensureCompatibleWith(val2);
        return new Value(Math.max(val1.getAmt(), val2.getAmt()), val1.getUnit());
    }

    public Value multiplyBy(int i) {
        return new Value(getAmt()*i, getUnit());
    }
}

