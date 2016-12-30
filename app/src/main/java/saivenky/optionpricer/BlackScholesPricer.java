package saivenky.optionpricer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Created by saivenky on 12/29/16.
 */

public class BlackScholesPricer {
    private static final double TRADING_DAYS = 254;
    private final double r;

    public BlackScholesPricer() {

        r = 0.0;
    }

    double d1(double spot, double strike, double timeToExpiry, double sigma) {
        return (Math.log(spot / strike) + ((r + (sigma*sigma / 2))*timeToExpiry)) / (sigma * Math.sqrt(timeToExpiry));
    }

    double d3(double timeToExpiry, double sigma) {
        return sigma*Math.sqrt(timeToExpiry);
    }


    public BlackScholesPrice callPrice(double spot, double strike, double timeToExpiry, double sigma) {
        double d1 = d1(spot, strike, timeToExpiry, sigma);
        double d2 = d1 - d3(timeToExpiry, sigma);
        BlackScholesPrice price = new BlackScholesPrice();
        price.price = NormalDist.cdf(d1)*spot - NormalDist.cdf(d2)*strike*Math.exp(-r*timeToExpiry);
        price.delta = NormalDist.cdf(d1);
        return price;
    }

    public BlackScholesPrice putPrice(double spot, double strike, double timeToExpiry, double sigma) {
        double d1 = d1(spot, strike, timeToExpiry, sigma);
        double d2 = d1 - d3(timeToExpiry, sigma);
        BlackScholesPrice price = new BlackScholesPrice();
        price.price = -NormalDist.cdf(-d1)*spot + NormalDist.cdf(-d2)*strike*Math.exp(-r*timeToExpiry);
        price.delta = -NormalDist.cdf(-d1);
        return price;
    }

    private static final double PRECISION = 0.0001;

    public double calculateCallImpliedVol(double actualCallPrice, double underlying, double strike, double timeToExpiry) {
        double vol = 1;
        double step = 0.5;
        double callPrice = callPrice(underlying, strike, timeToExpiry, vol).price;

        while (Math.abs(actualCallPrice - callPrice) > PRECISION) {
            if (actualCallPrice < callPrice) {
                vol -= step;
                double newCallPrice = callPrice(underlying, strike, timeToExpiry, vol).price;
                if (actualCallPrice > newCallPrice || Double.isNaN(newCallPrice)) {
                    vol += step;
                    step *= 0.1;
                    callPrice = callPrice(underlying, strike, timeToExpiry, vol).price;
                }
                else {
                    callPrice = newCallPrice;
                }
            }
        }
        return vol;
    }

    public double calculatePutImpliedVol(double actualPutPrice, double underlying, double strike, double timeToExpiry) {
        double vol = 1;
        double step = 0.5;
        double putPrice = putPrice(underlying, strike, timeToExpiry, vol).price;

        while (Math.abs(actualPutPrice - putPrice) > PRECISION) {
            if (actualPutPrice < putPrice) {
                vol -= step;
                double newPutPrice = putPrice(underlying, strike, timeToExpiry, vol).price;
                if (actualPutPrice > newPutPrice || Double.isNaN(newPutPrice)) {
                    vol += step;
                    step *= 0.1;
                    putPrice = putPrice(underlying, strike, timeToExpiry, vol).price;
                }
                else {
                    putPrice = newPutPrice;
                }
            }
        }
        return vol;
    }


    private static final String EXPIRY_TIME = "T16:00";
    public static double daysToExpiry(String expiry) {
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

    public static void main(String[] args) throws ParseException {
        BlackScholesPricer bsp = new BlackScholesPricer();
        double daysToExpiry = daysToExpiry("2016-12-30");
        //System.out.println(bsp.callPrice(57.01, 57.5, 5./254.));
        System.out.println(bsp.putPrice(56.32, 56, daysToExpiry/TRADING_DAYS, 0.1297));
        System.out.println(bsp.calculatePutImpliedVol(0.09, 56.32, 56, daysToExpiry/TRADING_DAYS));

    }
}
