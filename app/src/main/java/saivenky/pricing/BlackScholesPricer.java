package saivenky.pricing;

import java.text.ParseException;

import saivenky.trading.ITrade;

/**
 * Created by saivenky on 12/29/16.
 */

public class BlackScholesPricer extends IPricer {
    private static final double RISK_FREE_INTEREST = Math.pow(0.008, 1);
    private static final double PRECISION = 0.00001;
    private static final double IMPLIED_VOL_PRECISION = 1e-6;
    private static final int MAX_ITERATIONS = 10;
    public static IPricer DEFAULT = new BlackScholesPricer(RISK_FREE_INTEREST);

    private final double r;

    private BlackScholesPricer(double riskFreeInterestRate) {
        r = riskFreeInterestRate;
    }

    double d1(double spot, double strike, double timeToExpiry, double sigma) {
        return (Math.log(spot / strike) + ((r + (sigma*sigma / 2))*timeToExpiry)) / (sigma * Math.sqrt(timeToExpiry));
    }


    double d3(double timeToExpiry, double sigma) {
        return sigma*Math.sqrt(timeToExpiry);
    }

    @Override
    public Theo getTheo(boolean isCall, double spot, double strike, double timeToExpiry, double sigma) {
        if (timeToExpiry < 0) timeToExpiry = 0;
        double d1 = d1(spot, strike, timeToExpiry, sigma);
        double d2 = d1 - d3(timeToExpiry, sigma);
        Theo theo = new Theo();
        if(isCall) {
            theo.price = NormalDist.cdf(d1)*spot - NormalDist.cdf(d2)*strike*Math.exp(-r*timeToExpiry);
            theo.delta = NormalDist.cdf(d1);
        }
        else {
            theo.price = -NormalDist.cdf(-d1)*spot + NormalDist.cdf(-d2)*strike*Math.exp(-r*timeToExpiry);
            theo.delta = -NormalDist.cdf(-d1);
        }

        theo.gamma = NormalDist.pdf(d1) / (spot * sigma * Math.sqrt(timeToExpiry));
        theo.vega = spot * NormalDist.pdf(d1) * Math.sqrt(timeToExpiry);

        return theo;
    }

    @Override
    public double getImpliedVol(double actualPrice, boolean isCall, double spot, double strike, double timeToExpiry) {
        // call put parity => C - P = S - DK
        // P = C - (S - DK)
        // C = P + (S - DK)
        double callPutSign = isCall ? 1 : -1;
        if (callPutSign * (spot - strike) > 0) {
            double parityValue = spot - Math.exp(-r*timeToExpiry) * strike;
            double putCallPrice = actualPrice - callPutSign * parityValue;
            System.out.println("PARITY" + strike);
            return getImpliedVol(putCallPrice, !isCall, spot, strike, timeToExpiry);
        }

        if(actualPrice  < 0) return 0;
        double sigma = Math.sqrt(Math.PI * 2 / timeToExpiry) * actualPrice / spot;
        Theo theo = getTheo(isCall, spot, strike, timeToExpiry, sigma);

        int iterations = 0;
        double update = (theo.price - actualPrice) / theo.vega;
        while (Math.abs(update) > IMPLIED_VOL_PRECISION && iterations < MAX_ITERATIONS) {
            if (update > 0.1) {
                update = 0.1;
            }
            if (update < -0.1) {
                update = -0.1;
            }
            sigma = sigma - update;
            theo = getTheo(isCall, spot, strike, timeToExpiry, sigma);
            update = (theo.price - actualPrice) / theo.vega;
            iterations++;
        }

        return sigma;
    }

    public static void main(String[] args) throws ParseException {
        System.out.println(BlackScholesPricer.DEFAULT.getImpliedVol(1.875, true, 21, 20, 0.25));
    }
}
