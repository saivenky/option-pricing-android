package saivenky.trading;

import saivenky.pricing.Theo;

public interface ITrade {
    double MAX_UNDERLYING = 100000;
    double getValue(double underlying);
    double getPnL(double underlying);
    double getStrike();
    double getBreakevenUnderlyingPrice();
    String fullDescription();
    Theo getTheo(double underlying, double impliedVol);
}
