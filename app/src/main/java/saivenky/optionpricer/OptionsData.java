package saivenky.optionpricer;

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

public class OptionsData {
    private static final BlackScholesPricer pricer = new BlackScholesPricer();

    OptionLine[] calls;
    OptionLine[] puts;
    StockQuote stock;

    void getData(String expiry) throws IOException, JSONException {
        long epochTime = epochTime(expiry);
        String urlPath = String.format("https://query2.finance.yahoo.com/v7/finance/options/SBUX?formatted=false&lang=en-US&region=US&straddle=false&date=%d", epochTime);

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

        this.stock = new StockQuote(stock);
        this.calls = getOptionLines(calls);
        this.puts = getOptionLines(puts);

        double timeToExpiry = BlackScholesPricer.timeToExpiry(expiry);
        for(OptionLine call : this.calls) {
            call.calculatedIV = pricer.calculateCallImpliedVol(call.getTheoPrice(), this.stock.regularMarketPrice, call.strike, timeToExpiry);
        }

        for(OptionLine put : this.puts) {
            put.calculatedIV = pricer.calculatePutImpliedVol(put.getTheoPrice(), this.stock.regularMarketPrice, put.strike, timeToExpiry);
        }
    }

    OptionLine[] getOptionLines(JSONArray optionsArray) {
        int length = optionsArray.length();
        OptionLine[] options = new OptionLine[length];
        for(int i = 0; i < length; i++) {
            OptionLine ol = new OptionLine(optionsArray.getJSONObject(i));
            options[i] = ol;
        }

        return options;
    }

    OptionLine getOptionLine(boolean isCall, double strike) {
        OptionLine[] lines = isCall ? calls : puts;
        for(int i = 0; i < lines.length; i++) {
            if(isCloseTo(lines[i].strike, strike)) return lines[i];
        }

        return null;
    }

    private static final double PRECISION = 0.00001;
    boolean isCloseTo(double a, double b) {
        return Math.abs(a - b) < PRECISION;
    }

    static final String EXPIRY_TIME = "T16:00";
    static final long YAHOO_WEIRD_EXPIRY_OFFSET = -24*60*60;
    static long epochTime(String expiry) {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        TimeZone eastern = TimeZone.getTimeZone("America/New_York");
        isoFormat.setTimeZone(eastern);
        long timezoneOffset = TimeZone.getDefault().getRawOffset() - eastern.getRawOffset();
        try {
            Date date = isoFormat.parse(expiry + EXPIRY_TIME);
            long expiryMillis = date.getTime();
            return expiryMillis / 1000 + YAHOO_WEIRD_EXPIRY_OFFSET - timezoneOffset / 1000;
        }
        catch(ParseException e) {
            return 0;
        }
    }

    public static void main(String[] args) throws IOException, JSONException {
        OptionsData od = new OptionsData();
        od.getData("2016-12-30");
        for (OptionLine o : od.calls) {
            System.out.printf("%.2f: %.2f\n", o.strike, o.lastPrice);
        }
        Date d = new Date(epochTime("2016-12-30") *1000);
        System.out.println(d);
    }
}
