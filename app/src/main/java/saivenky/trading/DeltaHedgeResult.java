package saivenky.trading;

/**
 * Created by saivenky on 1/1/17.
 */

public class DeltaHedgeResult {
    public boolean isHedgeNeeded;
    public boolean isCurrent;
    public double delta;
    public double price;

    @Override
    public String toString() {
        if (!isHedgeNeeded) return "No hedge needed";
        String formatString = isCurrent ? "Current: deltas %.1f @ $%.2f" : "Needed soon: deltas %.1f @ $%.2f";
        return String.format(formatString, delta, price);
    }
}
