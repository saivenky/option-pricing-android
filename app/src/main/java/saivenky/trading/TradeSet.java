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
import saivenky.data.Stock;
import saivenky.pricing.BlackScholesPricer;
import saivenky.pricing.IPricer;
import saivenky.pricing.Theo;

public class TradeSet {
    List<OptionTrade> optionTrades;
    StockTrade stockTrade;
    double cash;
    TreeSet<Double> importantPrices;

    public TradeSet() {
        optionTrades = new ArrayList<>();
        stockTrade = new StockTrade();
        cash = 0;
        importantPrices = new TreeSet<>();
    }

    public void addTrade(ITrade trade) {
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
        importantPrices.remove(stockTrade.getStrike());

        double totalValue = stockTrade.getValue(0) + trade.getValue(0);
        int totalQuantity = stockTrade.quantity + trade.quantity;
        if (totalQuantity == 0) {
            cash += totalValue;
            stockTrade.price = 0;
            stockTrade.quantity = 0;
        }
        else {
            stockTrade.price = -totalValue / totalQuantity;
            stockTrade.quantity = totalQuantity;
            importantPrices.add(stockTrade.getStrike());
        }
    }


    public boolean isEmpty() {
        return optionTrades.isEmpty() && stockTrade.quantity == 0;
    }

    public void clearTrades() {
        for(OptionTrade optionTrade : optionTrades) {
            OptionChainRetriever.DEFAULT.removeExpiry(optionTrade.expiry);
        }
        optionTrades.clear();
        stockTrade.quantity = 0;
        stockTrade.price = 0;
        cash = 0;
        importantPrices.clear();
    }

    public double getPnl(double underlying) {
        double pnl = 0.0;
        for(OptionTrade trade : optionTrades) {
            pnl += trade.getPnL(underlying);
        }
        pnl += stockTrade.getPnL(underlying);
        pnl += cash;
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
        if (optionTrades.isEmpty() && stockTrade.quantity == 0) {
            priceToPnl.put(0., cash);
            return priceToPnl;
        }

        double maximum = importantPrices.last();
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

        Theo stockTheo = stockTrade.getTheo(underlying, 0);
        totalTheo.add(stockTheo);

        return totalTheo;
    }

    //TODO: Simplify this. Too many theo calculations
    public double getClosePnl(double underlying) {
        double pnl = 0.0;
        for(OptionTrade optionTrade : optionTrades) {
            double theoPrice;
            OptionChain optionChain = OptionChainRetriever.DEFAULT.getOptionChain(optionTrade.expiry);
            if (optionChain.isExpired) {
                theoPrice = optionTrade.isCall ? Math.max(0, underlying - optionTrade.strike) : Math.max(0, optionTrade.strike - underlying);
            }
            else {
                Option option = optionChain.getOption(optionTrade.isCall, optionTrade.strike);
                double timeToExpiry = IPricer.timeToExpiry(optionTrade.expiry);
                Theo theo = BlackScholesPricer.DEFAULT.getTheo(optionTrade.isCall, underlying, optionTrade.strike, timeToExpiry, option.calculatedImpliedVol);
                theoPrice = theo.price;
            }

            pnl += optionTrade.getClosePnl(underlying, theoPrice);
        }

        Theo stockTheo = stockTrade.getTheo(underlying, 0);
        pnl += stockTrade.getClosePnl(underlying, stockTheo.price);
        pnl += cash;
        return pnl;
    }

    public String describeTheo(double underlying) {
        Theo totalTheo = getTheo(underlying);
        return describeTheo(totalTheo, underlying);
    }

    public String describeTheo(Theo totalTheo, double underlying) {
        return totalTheo.prettyString() +
                String.format("\nCurrent Underlying: %.2f\nCurrent PnL: %.2f\n", underlying, getPnl(underlying));
    }

    public double calculateIntercept(double price1, double pnl1, double price2, double pnl2) {
        double slope = ((pnl2 - pnl1) / (price2 - price1));
        return -pnl1/slope + price1;
    }

    public static void main(String[] args) throws IOException {
        Stock.initialize("SBUX");
        OptionChainRetriever.initialize(Stock.DEFAULT);

        TradeSet ts = new TradeSet();
        ts.addTrade(OptionTrade.parse("-3 x 56 C @ 0.3 2017-01-06"));
        ts.addTrade(OptionTrade.parse("+10 x 55.5 P @ 0.13 2017-01-06"));
        ts.addTrade(StockTrade.parse("300 @ 55.5"));

        OptionChainRetriever.DEFAULT.retrieveDataForAll();
        System.out.println(ts.describePnL());
        System.out.println(ts.describeTheo(Stock.DEFAULT.regularMarketPrice));
    }
}
