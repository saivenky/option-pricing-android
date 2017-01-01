package saivenky.trading;

import saivenky.data.OptionChainRetriever;
import saivenky.pricing.Theo;

public class LargeDeltaHedgeChecker {
    public static double LARGE_DELTA = 80;

    public DeltaHedgeResult check(TradeSet trades) {
        double underlying = OptionChainRetriever.DEFAULT.underlying;
        Theo theo = trades.getTheo(underlying);
        DeltaHedgeResult result = new DeltaHedgeResult();
        result.isHedgeNeeded = Math.abs(theo.delta) >= LARGE_DELTA;
        result.currentDelta = theo.delta;

        return result;
    }
}
