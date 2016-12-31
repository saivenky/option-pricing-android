package saivenky.trading;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import saivenky.data.Option;
import saivenky.data.OptionChain;
import saivenky.pricing.Theo;

public class TradeSet {
    List<ITrade> trades;
    TreeSet<Double> importantPrices;

    public TradeSet() {
        trades = new ArrayList<>();
        importantPrices = new TreeSet<>();
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

    String createPnlLine(double underlying, double pnl) {
        return String.format("%4.2f: %4.2f\n", underlying, pnl);
    }

    public String describePnL() {
        StringBuilder builder = new StringBuilder();
        double prevPrice = 0.0;
        double prevPnl = getPnl(0.0);
        builder.append(createPnlLine(prevPrice, prevPnl));

        for(double price : importantPrices) {
            double pnl = getPnl(price);
            if((prevPnl < 0 && pnl > 0) || (prevPnl > 0 && pnl < 0)) {
                double breakeven = calculateIntercept(price, pnl, prevPrice, prevPnl);
                builder.append(createPnlLine(breakeven, getPnl(breakeven)));
            }

            builder.append(createPnlLine(price, pnl));
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

    public String describeTheo(OptionChain optionChain) {
        double underlying = optionChain.stock.regularMarketPrice;
        Theo totalTheo = new Theo();
        for(ITrade trade : trades) {
            Theo theo;
            if(trade instanceof OptionTrade) {
                OptionTrade optionTrade = (OptionTrade) trade;
                if (optionChain.isExpired) {
                    theo = optionTrade.getTheo(underlying, 0);
                }
                else {
                    Option option = optionChain.getOption(optionTrade.isCall, optionTrade.strike);
                    theo = option.theo;
                }
            }
            else {
                StockTrade stockTrade = (StockTrade) trade;
                theo = stockTrade.getTheo(underlying, 0);
            }

            totalTheo.add(theo);
        }

        return totalTheo.prettyString() +
                String.format("\nCurrent Underlying: %.2f\nCurrent PnL: %.2f\n", underlying, getPnl(underlying));
    }

    public double calculateIntercept(double price1, double pnl1, double price2, double pnl2) {
        double slope = ((pnl2 - pnl1) / (price2 - price1));
        return -pnl1/slope + price1;
    }

    public static void main(String[] args) throws IOException {
        OptionChain optionChain = new OptionChain();
        optionChain.getData("2016-12-30");

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

        System.out.println(ts.describePnL());
        System.out.println(ts.describeTheo(optionChain));
    }
}
