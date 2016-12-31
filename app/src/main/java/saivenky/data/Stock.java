package saivenky.data;


import org.mson.JSONObject;

public class Stock {
    public double regularMarketPrice;
    public double bid;
    public double ask;

    public Stock(JSONObject object) {
        regularMarketPrice = object.getDouble("regularMarketPrice");
        bid = object.getDouble("bid");
        ask = object.getDouble("ask");
    }
}
