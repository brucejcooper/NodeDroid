package com.eightbitcloud.internode;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateKeyFormatter implements KeyFormatter {
    public static final DateKeyFormatter MONTH_FORMATTER = new DateKeyFormatter(new SimpleDateFormat(" dd MMM"));
    public static final DateKeyFormatter YEAR_FORMATTER = new DateKeyFormatter(new SimpleDateFormat(" MMM"));
    private DateFormat format;
    
    public DateKeyFormatter(DateFormat format) {
        this.format = format;
    }

    public String format(Object key) {
        if (!(key instanceof Date)) {
            return key.toString();
        }
        return format.format((Date) key);
    }

}
