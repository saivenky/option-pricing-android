package saivenky.optionpricer;

public interface ITrade {
    double MAX_UNDERLYING = 10000000;
    double getValue(double underlying);
    double getPnL(double underlying);
    double getStrike();
    double getBreakevenUnderlyingPrice();
    String fullDescription();
    BlackScholesPrice getTheo(double underlying, double impliedVol);
}
