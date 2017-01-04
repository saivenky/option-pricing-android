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
    public long lastUpdate;
    public String expiry;
    public Option[] calls;
    public Option[] puts;

    private Stock stock;

    private long epochTime;
    private String dataUrl;

    OptionChain(String expiry, Stock stock) {
        this.expiry = expiry;
        epochTime = epochTime(this.expiry);
        dataUrl = createUrl(stock.symbol, epochTime);
        isExpired = IPricer.timeToExpiry(expiry) < 0;
        this.stock = stock;
    }

    private static String createUrl(String stockSymbol, long epochTime) {
        return String.format("https://query2.finance.yahoo.com/v7/finance/options/%s?formatted=false&lang=en-US&region=US&straddle=false&date=%d", stockSymbol, epochTime);
    }

    private StringBuilder getRaw() {
        StringBuilder sb = new StringBuilder();

        try {
            URL url = new URL(dataUrl);
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        catch(IOException e) {
            System.err.printf("Error downloading raw data %s: %s\n", expiry, e.getMessage());
        }

        if (sb.length() == 0) {
            System.err.printf("No options data received %s\n", expiry);
        }

        return sb;
    }

    public void getData() {
        lastUpdate = System.currentTimeMillis();
        isExpired = IPricer.timeToExpiry(expiry) < 0;

        StringBuilder sb = getRaw();

        JSONObject jsonObject = new JSONObject(sb.toString());
        JSONArray result = jsonObject.getJSONObject("optionChain").getJSONArray("result");
        JSONObject options = result.getJSONObject(0).getJSONArray("options").getJSONObject(0);
        JSONObject stock = result.getJSONObject(0).getJSONObject("quote");
        JSONArray calls = options.getJSONArray("calls");
        JSONArray puts = options.getJSONArray("puts");

        this.stock.update(stock, lastUpdate);

        if (isExpired) {
            this.calls = null;
            this.puts = null;
            return;
        }

        this.calls = getOptionLines(calls);
        this.puts = getOptionLines(puts);

        double timeToExpiry = IPricer.timeToExpiry(expiry);
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
        if (options == null) {
            System.err.printf("No option data (isCall: %s)\n", isCall);
            return null;
        }

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

        for(Option call : calls) {
            strikes.add(call.strike);
        }
        for(Option put : puts) {
            strikes.add(put.strike);
        }

        String result = "";
        result += "call delta iv strike put iv delta\n";
        for(double strike : strikes) {
            Option call = getOption(true, strike);
            Option put = getOption(false, strike);
            if (call == null) {
                call = Option.EMPTY;
            }
            if (put == null) {
                put = Option.EMPTY;
            }
            result += line("Price", call.getPrice(), strike, put.getPrice());
            result += line("Theo", call.theo.price, strike, put.theo.price);
            result += line("Delta", call.theo.delta, strike, put.theo.delta);
            result += line("Gamma", call.theo.gamma, strike, put.theo.gamma);
            result += line("Theta", call.theo.theta / IPricer.TRADING_DAYS, strike, put.theo.theta / IPricer.TRADING_DAYS);
            result += line("IV", call.calculatedImpliedVol, strike, put.calculatedImpliedVol);
            result += "------------------\n";
        }

        return result;
    }

    String line(String header, double... nums) {
        String result = header + " ";
        for(double num : nums) {
            result += String.format("%6.4f ", num);
        }
        result += "\n";
        return result;
    }

    public static void main(String[] args) throws IOException, JSONException {
        Stock.initialize("SBUX");
        OptionChain od = new OptionChain("2017-01-06", Stock.DEFAULT);
        od.getData();
        System.out.println(od);
    }
}
