package com.eightbitcloud.internode.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateTools {
    public static final DateFormat DATE_PARSER_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static final DateFormat LOCAL_DATE_FORMAT = new SimpleDateFormat("HH:mm dd MMM");
    
    /**
     * When viewing the graph as a daily view (i.e. we're viewiing the last month),
     * this formatter is used to format the labels
     */
    public static final DateFormat MONTH_FORMATTER = new SimpleDateFormat(" dd MMM");
    public static final DateFormat YEAR_FORMATTER = new SimpleDateFormat(" MMM");
    public static final DateFormat PREFS_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
    public static final TimeZone adelaideTZ;

    public static final long SECOND = 1000;
    public static final long MINUTE = 60 * SECOND;
    public static final long HOUR = 60 * MINUTE;
    public static final long DAY = 24 * HOUR;

    static {
        adelaideTZ = TimeZone.getTimeZone("GMT+930");
        DATE_PARSER_FORMAT.setTimeZone(adelaideTZ);
        PREFS_DATE_FORMAT.setTimeZone(adelaideTZ);
    }
    

    
    
    // Make sure the Time is set to Midnight, Adelaide Time
    public static Date ensureMidnight(Date d, TimeZone tz) {
        Calendar c = Calendar.getInstance(tz);
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        
        return c.getTime();
    }
    
    public static Date parseAdelaideDate(String dt) throws ParseException {
        Date parsed = DATE_PARSER_FORMAT.parse(dt);
        Date midnighted = ensureMidnight(parsed, adelaideTZ);
//        Log.d(TAG, "took string '" + dt + "'");
//        Log.d(TAG, "Parsed to " + parsed );
//        Log.d(TAG, "corrected to " + midnighted);
//        Log.d(TAG, "which displays as " + LOCAL_DATE_FORMAT.format(midnighted));
        return midnighted;
        
    }

}
