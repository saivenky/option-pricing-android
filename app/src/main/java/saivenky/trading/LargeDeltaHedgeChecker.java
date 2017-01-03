package saivenky.trading;

import saivenky.pricing.Theo;

public class LargeDeltaHedgeChecker {
    public static double LARGE_DELTA = 20;
    private static final double PRICE_CHANGE = 0.05;

    public DeltaHedgeResult check(TradeSet trades, double underlying) {
        Theo theo = trades.getTheo(underlying);
        if (Math.abs(theo.delta) < LARGE_DELTA) {
            double goalDelta = (theo.delta < 0) ? -LARGE_DELTA : LARGE_DELTA;
            double changeNeededBeforeHedge = (goalDelta - theo.delta) / theo.gamma;
            if(Math.abs(changeNeededBeforeHedge) <= PRICE_CHANGE) {
                DeltaHedgeResult result = new DeltaHedgeResult();
                result.isHedgeNeeded = true;
                result.isCurrent = false;
                result.delta = goalDelta;
                result.price = underlying + changeNeededBeforeHedge;

                return result;
            }
        }

        DeltaHedgeResult result = new DeltaHedgeResult();
        result.isHedgeNeeded = Math.abs(theo.delta) >= LARGE_DELTA;
        result.isCurrent = true;
        result.delta = theo.delta;
        result.delta = underlying;

        return result;
    }


}
