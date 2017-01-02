package saivenky.trading;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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

    public void clearTrades() {
        for(OptionTrade optionTrade : optionTrades) {
            OptionChainRetriever.DEFAULT.removeExpiry(optionTrade.expiry);
        }
        optionTrades.clear();
        stockTrades.clear();
        importantPrices.clear();
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

    public String getQuotesForOptions() {
        StringBuilder result = new StringBuilder();
        for(OptionTrade optionTrade : optionTrades) {
            OptionChain optionChain = OptionChainRetriever.DEFAULT.getOptionChain(optionTrade.expiry);
            if (!optionChain.isExpired) {
                Option option = optionChain.getOption(optionTrade.isCall, optionTrade.strike);
                result.append(option.toString()+"\n");
                result.append(option.theo+"\n");
                result.append("----");
            }
        }

        return result.toString();
    }

    public String describePnL() {
        StringBuilder builder = new StringBuilder();
        Map<Double, Double> priceToPnl = getPriceToPnl();
        for (double price : priceToPnl.keySet()) {
            builder.append(createPnlLine(price, priceToPnl.get(price)));
        }
        return builder.toString();
    }

    public Map<Double, Double> getPriceToPnl() {
        TreeMap<Double, Double> priceToPnl = new TreeMap<>();
        if (optionTrades.isEmpty() && stockTrades.isEmpty()) {
            return priceToPnl;
        }

        double minimum = importantPrices.first();
        double maximum = importantPrices.last();
        double range = maximum - minimum;
        double belowAboveRange = 0.1 * range;
        double belowMin = 0;
        double aboveMax = Math.round(2 * maximum / 100) * 100;

        double prevPrice = belowMin;
        double prevPnl = getPnl(prevPrice);
        priceToPnl.put(prevPrice, prevPnl);

        for(double price : importantPrices) {
            double pnl = getPnl(price);
            if((prevPnl < 0 && pnl > 0) || (prevPnl > 0 && pnl < 0)) {
                double breakeven = calculateIntercept(price, pnl, prevPrice, prevPnl);
                priceToPnl.put(breakeven, getPnl(breakeven));
            }

            priceToPnl.put(price, pnl);
            prevPrice = price;
            prevPnl = pnl;
        }

        double price = aboveMax;
        double pnl = getPnl(price);
        if((prevPnl < 0 && pnl > 0) || (prevPnl > 0 && pnl < 0)) {
            double breakeven = calculateIntercept(price, pnl, prevPrice, prevPnl);
            priceToPnl.put(breakeven, getPnl(breakeven));
        }
        priceToPnl.put(price, pnl);

        priceToPnl.remove(priceToPnl.firstKey());
        priceToPnl.remove(priceToPnl.lastKey());
        return priceToPnl;
    }

    public Theo getTheo(double underlying) {
        Theo totalTheo = new Theo();
        for(OptionTrade optionTrade : optionTrades) {
            Theo theo;
            OptionChain optionChain = OptionChainRetriever.DEFAULT.getOptionChain(optionTrade.expiry);
            if (optionChain.isExpired) {
                theo = optionTrade.getTheo(underlying, 0);
            }
            else {
                Option option = optionChain.getOption(optionTrade.isCall, optionTrade.strike);
                theo = optionTrade.getTheo(underlying, option.calculatedImpliedVol);
            }

            totalTheo.add(theo);
        }

        for(StockTrade stockTrade : stockTrades) {
            Theo theo = stockTrade.getTheo(underlying, 0);
            totalTheo.add(theo);
        }

        return totalTheo;
    }

    public String describeTheo(double underlying) {
        Theo totalTheo = getTheo(underlying);
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
        System.out.println(ts.describeTheo(OptionChainRetriever.DEFAULT.underlying));
    }
}
