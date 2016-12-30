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

    String describeAtPrice(double underlying, double pnl) {
        return String.format("%.2f: %.2f\n", underlying, pnl);
    }

    String describePnL() {
        StringBuilder builder = new StringBuilder();
        double prevPrice = 0.0;
        double prevPnl = getPnl(0.0);
        builder.append(describeAtPrice(prevPrice, prevPnl));

        for(double price : importantPrices) {
            double pnl = getPnl(price);
            if((prevPnl < 0 && pnl > 0) || (prevPnl > 0 && pnl < 0)) {
                double breakeven = calculateIntercept(price, pnl, prevPrice, prevPnl);
                builder.append(describeAtPrice(breakeven, getPnl(breakeven)));
            }

            builder.append(describeAtPrice(price, pnl));
            prevPrice = price;
            prevPnl = pnl;
        }

        double price = ITrade.MAX_UNDERLYING;
        double pnl = getPnl(price);
        if((prevPnl < 0 && pnl > 0) || (prevPnl > 0 && pnl < 0)) {
            double breakeven = calculateIntercept(price, pnl, prevPrice, prevPnl);
            builder.append(String.format("%.2f: %.2f\n", breakeven, getPnl(breakeven)));
        }
        builder.append(String.format("%.2f: %.2f\n", price, pnl));
        return builder.toString();
    }

    public static void main(String[] args) throws IOException {
        System.out.println("starting");
        OptionsData data = new OptionsData();
        data.getData("2016-12-30");
        System.out.println("calculating pnl");
        TradeSet ts = new TradeSet();
        ts.addTrade(OptionTrade.parse("+3 x 56 C @ 1.11 2016-12-30"));
        ts.addTrade(OptionTrade.parse("+6 x 56 P @ 0.13 2016-12-30"));
        ts.addTrade(OptionTrade.parse("-3 x 57.5 C @ 0.22 2016-12-30"));
        ts.addTrade(StockTrade.parse("50 @ 56.32"));
        ts.addTrade(StockTrade.parse("50 @ 56.48"));


        ts.addTrade(StockTrade.parse("100 @ 56.92"));

        /*
        // Don't include these
        ts.addTrade(StockTrade.parse("30 @ 57.80"));
        ts.addTrade(StockTrade.parse("20 @ 38.05"));
        ts.addTrade(StockTrade.parse("20 @ 38.25"));
        ts.addTrade(StockTrade.parse("20 @ 37.45"));
        ts.addTrade(StockTrade.parse("10 @ 33.82"));
        //

        */
        ts.addTrade(StockTrade.parse("350 @ 55.96"));
        ts.addTrade(StockTrade.parse("-280 @ 56.00"));

        ts.addTrade(StockTrade.parse("150 @ 55.82"));
        ts.addTrade(StockTrade.parse("100 @ 55.75"));
        ts.addTrade(StockTrade.parse("80 @ 55.52"));

        //Hypothetical trades
        //ts.addTrade(StockTrade.parse("100 @ 55.80"));


        double delta = 0;
        data.stock.regularMarketPrice = 55.55;
        double underlying = data.stock.regularMarketPrice;
        for(ITrade trade : ts.trades) {
            System.out.println("\n"+trade.fullDescription());
            if(trade instanceof OptionTrade) {
                OptionTrade optionTrade = (OptionTrade) trade;
                OptionLine line = data.getOptionLine(optionTrade.isCall, optionTrade.strike);
                BlackScholesPrice bsp = optionTrade.getTheo(underlying, line.calculatedIV);
                System.out.printf("%.2f last(%.2f) (%.2f %.2f): %.4f %.4f\n", line.strike, line.lastPrice, line.bid, line.ask, line.calculatedIV, bsp.delta);
                delta += bsp.delta;
            }
            else {
                StockTrade stockTrade = (StockTrade) trade;
                delta += stockTrade.getTheo(underlying, 0).delta;
            }
        }


        System.out.println("\n---------------------");
        System.out.println(ts.describePnL());
        System.out.printf("\nDelta: %.4f\n", delta);
        System.out.println(ts.describeAtPrice(underlying, ts.getPnl(underlying)));
    }

    public double calculateIntercept(double price1, double pnl1, double price2, double pnl2) {
        double slope = ((pnl2 - pnl1) / (price2 - price1));
        return -pnl1/slope + price1;
    }
}
