package saivenky.data;

import org.mson.JSONArray;
import org.mson.JSONException;
import org.mson.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.TreeSet;

import saivenky.pricing.BlackScholesPricer;
import saivenky.pricing.IPricer;

public class OptionChain {
    public boolean isExpired;
    public String expiry;
    public Option[] calls;
    public Option[] puts;
    public Stock stock;

    private static String createUrl(String stockSymbol, String expiry) {
        long epochTime = epochTime(expiry);
        return String.format("https://query2.finance.yahoo.com/v7/finance/options/%s?formatted=false&lang=en-US&region=US&straddle=false&date=%d", stockSymbol, epochTime);
    }

    public void getData(String expiry) throws IOException, JSONException {
        isExpired = IPricer.timeToExpiry(expiry) < 0;

        this.expiry = expiry;
        String urlPath = createUrl("SBUX", expiry);

        URL url = new URL(urlPath);
        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = br.readLine()) != null) {
            sb.append(line);
        }

        JSONObject jsonObject = new JSONObject(sb.toString());
        JSONArray result = jsonObject.getJSONObject("optionChain").getJSONArray("result");
        JSONObject options = result.getJSONObject(0).getJSONArray("options").getJSONObject(0);
        JSONObject stock = result.getJSONObject(0).getJSONObject("quote");
        JSONArray calls = options.getJSONArray("calls");
        JSONArray puts = options.getJSONArray("puts");

        this.stock = new Stock(stock);
        if (isExpired) {
            this.calls = null;
            this.puts = null;
            return;
        }

        this.calls = getOptionLines(calls);
        this.puts = getOptionLines(puts);

        double timeToExpiry = IPricer.timeToExpiry(expiry);
        System.out.println(timeToExpiry);
        double underlyingPrice = this.stock.regularMarketPrice;
        for(Option call : this.calls) {
            call.calculatedImpliedVol = BlackScholesPricer.DEFAULT.getImpliedVol(call.getPrice(), true, underlyingPrice, call.strike, timeToExpiry);
            call.theo = BlackScholesPricer.DEFAULT.getTheo(true, underlyingPrice, call.strike, timeToExpiry, call.calculatedImpliedVol);
        }

        for(Option put : this.puts) {
            put.calculatedImpliedVol = BlackScholesPricer.DEFAULT.getImpliedVol(put.getPrice(), false, underlyingPrice, put.strike, timeToExpiry);
            put.theo = BlackScholesPricer.DEFAULT.getTheo(false, underlyingPrice, put.strike, timeToExpiry, put.calculatedImpliedVol);
        }
    }

    private Option[] getOptionLines(JSONArray optionsArray) {
        int length = optionsArray.length();
        Option[] options = new Option[length];
        for(int i = 0; i < length; i++) {
            Option ol = new Option(optionsArray.getJSONObject(i));
            options[i] = ol;
        }

        return options;
    }

    private static final double PRECISION = 0.00001;

    boolean isCloseTo(double a, double b) {
        return Math.abs(a - b) < PRECISION;
    }

    public Option getOption(boolean isCall, double strike) {
        Option[] options = isCall ? calls : puts;
        for(int i = 0; i < options.length; i++) {
            if(isCloseTo(options[i].strike, strike)) return options[i];
        }

        return null;
    }

    private static final String EXPIRY_TIME = "T16:00";
    private static final long YAHOO_WEIRD_EXPIRY_OFFSET = -24*60*60;
    private static final TimeZone EASTERN = TimeZone.getTimeZone("America/New_York");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
    private static final long TIME_ZONE_OFFSET = TimeZone.getDefault().getRawOffset() - EASTERN.getRawOffset();
    static {
        TIME_FORMAT.setTimeZone(EASTERN);
    }

    private static long epochTime(String expiry) {
        try {
            Date date = TIME_FORMAT.parse(expiry + EXPIRY_TIME);
            long expiryMillis = date.getTime();
            return expiryMillis / 1000 + YAHOO_WEIRD_EXPIRY_OFFSET - TIME_ZONE_OFFSET / 1000;
        }
        catch(ParseException e) {
            return 0;
        }
    }

    @Override
    public String toString() {
        TreeSet<Double> strikes = new TreeSet<>();
        double timeToExpiry = IPricer.timeToExpiry(expiry);
        for(Option call : calls) {
            strikes.add(call.strike);
        }
        for(Option put : puts) {
            strikes.add(put.strike);
        }

        String result = "";
        result += "call delta iv strike put iv delta\n";
        for(double strike : strikes) {
            //  bid price ask  delta gamma theta strike
            Option call = getOption(true, strike);
            Option put = getOption(false, strike);
            if (call == null) {
                call = Option.EMPTY;
            }
            if (put == null) {
                put = Option.EMPTY;
            }
            result += line(call.getPrice(), strike, put.getPrice());
            result += line(call.theo.price, strike, put.theo.price);
            result += line(call.theo.delta, strike, put.theo.delta);
            result += line(call.calculatedImpliedVol, strike, put.calculatedImpliedVol);
            result += "------------------\n";
        }

        return result;
    }

    String line(double... nums) {
        String result = "";
        for(double num : nums) {
            result += String.format("%6.4f ", num);
        }
        result += "\n";
        return result;
    }

    public static void main(String[] args) throws IOException, JSONException {
        OptionChain od = new OptionChain();
        od.getData("2017-01-06");
        System.out.println(od);
    }
}
