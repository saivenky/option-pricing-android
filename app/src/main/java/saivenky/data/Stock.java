package saivenky.data;


import org.mson.JSONArray;
import org.mson.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class Stock {
    public static Stock DEFAULT;
    public static void initialize(String symbol) {
        DEFAULT = new Stock(symbol);
    }

    public double regularMarketPrice;
    public double bid;
    public double ask;

    public final String symbol;
    private final String dataUrl;

    private Stock(String symbol) {
        this.symbol = symbol;
        dataUrl = createUrl(this.symbol);
    }

    private static String createUrl(String stockSymbol) {
        return String.format("http://finance.google.com/finance/info?client=ig&q=%s", stockSymbol);
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
        catch(IOException e) {}
        return sb;
    }

    private static final int RAW_DATA_OFFSET = 3;
    private static final String LAST_PRICE = "l";

    public void getData() {
        StringBuilder sb = getRaw();

        JSONArray jsonArray = new JSONArray(sb.substring(RAW_DATA_OFFSET));
        JSONObject result = jsonArray.getJSONObject(0);
        regularMarketPrice = result.getDouble(LAST_PRICE);
    }

    void update(JSONObject object) {
        regularMarketPrice = object.getDouble("regularMarketPrice");
        bid = object.getDouble("bid");
        ask = object.getDouble("ask");
    }

    public static void main(String[] args) {
        Stock stock = new Stock("SBUX");
        stock.getData();
        System.out.println(stock.regularMarketPrice);
    }
}
