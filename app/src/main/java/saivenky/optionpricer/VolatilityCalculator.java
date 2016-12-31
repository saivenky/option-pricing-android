package saivenky.optionpricer;

import saivenky.data.HistoricalPrices;
import saivenky.pricing.Stats;
import saivenky.pricing.StatsCalculator;

public class VolatilityCalculator {
    private static final double TRADING_DAYS = 254.;
    private final StatsCalculator statsCalculator;

    public VolatilityCalculator() {
        this.statsCalculator = new StatsCalculator();
    }

    public double calculateVolatility(Stats stats) {
        return stats.standardDeviation * Math.sqrt(TRADING_DAYS);
    }

    public double calculateVolatility(double[] percentChanges) {
        Stats stats = statsCalculator.calculateStandardDeviation(percentChanges);
        return calculateVolatility(stats);
    }

    public double[] calculatePriceDifferences(double[] pricesReverseChronOrder) {
        double[] percentChange = new double[pricesReverseChronOrder.length - 1];
        for(int i = 0; i < percentChange.length; i++) {
            double day = pricesReverseChronOrder[pricesReverseChronOrder.length - i - 1];
            double nextDay = pricesReverseChronOrder[pricesReverseChronOrder.length - i - 2];
            percentChange[i] = (nextDay - day) / day;
        }

        return percentChange;
    }

    public static void main(String[] args) {
        HistoricalPrices hp = new HistoricalPrices();
        VolatilityCalculator vc = new VolatilityCalculator();
        double[] prices = hp.getClosingPrices(30);
        double[] percents = vc.calculatePriceDifferences(prices);
        System.out.println(vc.calculateVolatility(percents));
    }
}
