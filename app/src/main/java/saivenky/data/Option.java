package saivenky.data;

import org.mson.JSONObject;

import saivenky.pricing.Theo;

/**
 * Created by saivenky on 12/29/16.
 */

public class Option {
    public static final Option EMPTY = new Option();

    public String symbol;
    public double strike;
    public double lastPrice;
    public double bid;
    public double ask;
    public double impliedVolatility;
    public double calculatedImpliedVol;
    public Theo theo;

    public Option(JSONObject object) {
        symbol = object.getString("contractSymbol");
        strike = object.getDouble("strike");
        lastPrice = object.getDouble("lastPrice");
        bid = object.getDouble("bid");
        ask = object.getDouble("ask");
        impliedVolatility = object.getDouble("impliedVolatility");
    }

    private Option() {
        bid = Double.NaN;
        ask = Double.NaN;
        lastPrice = Double.NaN;
        calculatedImpliedVol = Double.NaN;
        theo = new Theo();
        theo.delta = Double.NaN;
        theo.price = Double.NaN;
    }

    double getPrice() {
        return lastPrice;
        //return (bid + ask) / 2;
    }

    @Override
    public String toString() {
        return String.format("%s %.2f: %.2f", symbol, strike, lastPrice);
    }
}
