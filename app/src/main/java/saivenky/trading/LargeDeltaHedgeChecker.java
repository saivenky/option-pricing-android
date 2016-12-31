package saivenky.trading;

import saivenky.data.OptionChainRetriever;
import saivenky.pricing.Theo;

public class LargeDeltaHedgeChecker {
    public static double LARGE_DELTA = 80;

    public boolean check(TradeSet trades) {
        double underlying = OptionChainRetriever.DEFAULT.underlying;
        Theo theo = trades.getTheo(underlying);
        return Math.abs(theo.delta) >= LARGE_DELTA;
    }
}
