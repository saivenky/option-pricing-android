package saivenky.pricing;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public abstract class IPricer {
    public static double TRADING_DAYS = 254;
    public static final String EXPIRY_TIME = "T16:00";

    public abstract Theo getTheo(boolean isCall, double spot, double strike, double timeToExpiry, double sigma);
    public abstract double getImpliedVol(double actualPrice, boolean isCall, double spot, double strike, double timeToExpiry);

    public static double timeToExpiry(String expiry) {
        return daysToExpiry(expiry) / TRADING_DAYS;
    }

    private static double daysToExpiry(String expiry) {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        isoFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        try {
            Date date = isoFormat.parse(expiry + EXPIRY_TIME);
            long expiryMillis = date.getTime();
            return (expiryMillis - (new Date()).getTime()) / 1000. / 60. / 60. / 24;
        } catch (ParseException e) {
            return 0;
        }
    }
}
