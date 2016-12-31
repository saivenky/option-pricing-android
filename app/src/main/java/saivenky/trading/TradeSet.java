package saivenky.trading;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import saivenky.data.Option;
import saivenky.data.OptionChain;
import saivenky.data.OptionChainRetriever;
import saivenky.pricing.Theo;

public class TradeSet {
    List<OptionTrade> optionTrades;
    List<StockTrade> stockTrades;
    TreeSet<Double> importantPrices;

    public TradeSet() {
        optionTrades = new ArrayList<>();
        stockTrades = new ArrayList<>();
        importantPrices = new TreeSet<>();
    }

    void addTrade(ITrade trade) {
        if (trade instanceof OptionTrade) {
            addOptionTrade((OptionTrade) trade);
        }
        else addStockTrade((StockTrade) trade);
    }
    void addOptionTrade(OptionTrade trade) {
        importantPrices.add(trade.getStrike());
        OptionChainRetriever.DEFAULT.addExpiry(trade.expiry);
        optionTrades.add(trade);
    }

    void addStockTrade(StockTrade trade) {
        importantPrices.add(trade.getStrike());
        stockTrades.add(trade);
    }

    double getPnl(double underlying) {
        double pnl = 0.0;
        for(OptionTrade trade : optionTrades) {
            pnl += trade.getPnL(underlying);
        }
        for(StockTrade trade : stockTrades) {
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

    public String describeTheo() {
        double underlying = OptionChainRetriever.DEFAULT.underlying;
        Theo totalTheo = new Theo();
        for(OptionTrade optionTrade : optionTrades) {
            Theo theo;
            OptionChain optionChain = OptionChainRetriever.DEFAULT.getOptionChain(optionTrade.expiry);
            if (optionChain.isExpired) {
                theo = optionTrade.getTheo(underlying, 0);
            }
            else {
                Option option = optionChain.getOption(optionTrade.isCall, optionTrade.strike);
                theo = option.theo;
            }

            totalTheo.add(theo);
        }

        for(StockTrade stockTrade : stockTrades) {
            Theo theo = stockTrade.getTheo(underlying, 0);
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
        ts.addTrade(OptionTrade.parse("-3 x 56 C @ 0.3 2017-01-06"));
        ts.addTrade(OptionTrade.parse("+10 x 55.5 P @ 0.13 2017-01-06"));
        ts.addTrade(StockTrade.parse("300 @ 55.5"));

        OptionChainRetriever.DEFAULT.retrieveDataForAll();
        System.out.println(ts.describePnL());
        System.out.println(ts.describeTheo());
    }
}
