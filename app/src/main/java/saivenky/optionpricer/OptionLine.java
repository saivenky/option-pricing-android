package saivenky.optionpricer;

import org.mson.JSONObject;

/**
 * Created by saivenky on 12/29/16.
 */

public class OptionLine {
    String symbol;
    double strike;
    double lastPrice;
    double bid;
    double ask;
    double impliedVolatility;

    public OptionLine(JSONObject object) {
        symbol = object.getString("contractSymbol");
        strike = object.getDouble("strike");
        lastPrice = object.getDouble("lastPrice");
        bid = object.getDouble("bid");
        ask = object.getDouble("ask");
        impliedVolatility = object.getDouble("impliedVolatility");
    }

    @Override
    public String toString() {
        return String.format("%s %.2f: %.2f", symbol, strike, lastPrice);
    }
}
