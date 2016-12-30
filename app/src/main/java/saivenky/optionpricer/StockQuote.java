package saivenky.optionpricer;


import org.mson.JSONObject;

public class StockQuote {
    double regularMarketPrice;
    double bid;
    double ask;

    public StockQuote(JSONObject object) {
        regularMarketPrice = object.getDouble("regularMarketPrice");
        bid = object.getDouble("bid");
        ask = object.getDouble("ask");
    }
}
