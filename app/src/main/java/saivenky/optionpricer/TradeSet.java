package saivenky.optionpricer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class TradeSet {
    List<ITrade> trades = new ArrayList<>();
    TreeSet<Double> importantPrices = new TreeSet<>();

    public TradeSet() {
    }
    void addTrade(ITrade trade) {
        importantPrices.add(trade.getStrike());
        trades.add(trade);
    }

    double getPnl(double underlying) {
        double pnl = 0.0;
        for(ITrade trade : trades) {
            pnl += trade.getPnL(underlying);
        }
        return pnl;
    }

    void describeAtPrice(double underlying, double pnl) {
        System.out.printf("%.2f: %.2f\n", underlying, pnl);
    }

    void describePnL() {
        double prevPrice = 0.0;
        double prevPnl = getPnl(0.0);
        describeAtPrice(prevPrice, prevPnl);

        for(double price : importantPrices) {
            double pnl = getPnl(price);
            if((prevPnl < 0 && pnl > 0) || (prevPnl > 0 && pnl < 0)) {
                double breakeven = calculateIntercept(price, pnl, prevPrice, prevPnl);
                describeAtPrice(breakeven, getPnl(breakeven));
            }

            describeAtPrice(price, pnl);
            prevPrice = price;
            prevPnl = pnl;
        }

        double price = ITrade.MAX_UNDERLYING;
        double pnl = getPnl(price);
        if((prevPnl < 0 && pnl > 0) || (prevPnl > 0 && pnl < 0)) {
            double breakeven = calculateIntercept(price, pnl, prevPrice, prevPnl);
            System.out.printf("%.2f: %.2f\n", breakeven, getPnl(breakeven));
        }
        System.out.printf("%.2f: %.2f\n", price, pnl);
    }

    public static void main(String[] args) throws IOException {
        TradeSet ts = new TradeSet();
        ts.addTrade(OptionTrade.parse("+3 x 56 C @ 1.11 2016-12-30"));
        ts.addTrade(OptionTrade.parse("+6 x 56 P @ 0.13 2016-12-30"));
        ts.addTrade(OptionTrade.parse("-3 x 57.5 C @ 0.22 2016-12-30"));
        ts.addTrade(StockTrade.parse("50 @ 56.32"));
        ts.addTrade(StockTrade.parse("50 @ 56.48"));

        for(ITrade trade : ts.trades) {
            System.out.println("\n"+trade.fullDescription());
        }

        ts.describePnL();

        OptionsData od = new OptionsData();
        od.getData("2016-12-30");
        ts.describeAtPrice(od.stock.regularMarketPrice, ts.getPnl(od.stock.regularMarketPrice));
    }

    public double calculateIntercept(double price1, double pnl1, double price2, double pnl2) {
        double slope = ((pnl2 - pnl1) / (price2 - price1));
        return -pnl1/slope + price1;
    }
}
