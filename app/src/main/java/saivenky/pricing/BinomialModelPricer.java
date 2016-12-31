package saivenky.pricing;

public class BinomialModelPricer {
    private static final double INTEREST = 0;

    private double mInterest;
    private int mStepsPerDay;
    private int mSteps;

    public BinomialModelPricer(double daysToExpiry, int stepsPerDay) {
        mInterest = INTEREST;
        mStepsPerDay = stepsPerDay;
        mSteps = (int)daysToExpiry * stepsPerDay;
    }

    public double calculatePutPrice(double underlying, double strike, double volatility, double volSmileFactor) {
        volatility *= 1 + (Math.pow(underlying - strike, 2) * volSmileFactor);
        double timeStep = 1./mStepsPerDay;
        double upAmount = Math.exp(volatility * Math.sqrt(timeStep));
        double downAmount = 1./upAmount;
        double upProb = (Math.exp(mInterest * timeStep)-downAmount)/(upAmount-downAmount);
        return calculatePutPriceWithProb(underlying, strike, upAmount, upProb);
    }

    public double calculateCallPrice(double underlying, double strike, double volatility, double volSmileFactor) {
        volatility *= 1 + (Math.pow(underlying - strike, 2) * volSmileFactor);
        double timeStep = 1./mStepsPerDay;
        double upAmount = Math.exp(volatility * Math.sqrt(timeStep));
        double downAmount = 1./upAmount;
        double upProb = (Math.exp(mInterest * timeStep)-downAmount)/(upAmount-downAmount);
        return calculateCallPriceWithProb(underlying, strike, upAmount, upProb);
    }

    public double calculatePutPriceWithProb(double underlying, double strike, double upAmount, double upProb) {
        double downProb = 1 - upProb;

        double[] onExpiry = new double[mSteps];
        for (int i = 0; i < mSteps; i++) {
            double stockAtExpiry = underlying * Math.pow(upAmount, 2*i - mSteps + 1);
            onExpiry[i] = putValue(stockAtExpiry, strike);
        }

        for (int i = mSteps-1; i > 0; i--) {
            double[] prev = new double[i];
            for(int j = 0; j < i; j++) {
                prev[j] = downProb * onExpiry[j] + upProb * onExpiry[j+1];
                double stockAtTime = underlying * Math.pow(upAmount, 2*j - i + 1);
                prev[j] = Math.max(prev[j], putValue(stockAtTime, strike));
            }
            onExpiry = prev;
        }

        return onExpiry[0];
    }

    public double calculateCallPriceWithProb(double underlying, double strike, double upAmount, double upProb) {
        double downProb = 1 - upProb;

        double[] onExpiry = new double[mSteps];
        for (int i = 0; i < mSteps; i++) {
            double stockAtExpiry = underlying * Math.pow(upAmount, 2*i - mSteps + 1);
            onExpiry[i] = callValue(stockAtExpiry, strike);
        }

        for (int i = mSteps-1; i > 0; i--) {
            double[] prev = new double[i];
            for(int j = 0; j < i; j++) {
                prev[j] = downProb * onExpiry[j] + upProb * onExpiry[j+1];
                double stockAtTime = underlying * Math.pow(upAmount, 2*j - i + 1);
                prev[j] = Math.max(prev[j], callValue(stockAtTime, strike));
            }
            onExpiry = prev;
        }

        return onExpiry[0];
    }

    public double putValue(double underlying, double strike) {
        return Math.max(strike - underlying, 0);
    }

    public double callValue(double underlying, double strike) {
        return Math.max(underlying - strike, 0);
    }

    private static final double PRECISION = 0.0001;
    public double calculateImpliedVol(double putPrice, double underlying, double strike, double interest, double volSmileFactor) {
        double vol = 1;
        double step = 0.5;
        double putPriceWithVol = calculatePutPrice(underlying, strike, vol, volSmileFactor);
        while (Math.abs(putPrice - putPriceWithVol) > PRECISION) {
            if (putPrice < putPriceWithVol) {
                vol -= step;
                double newPutPriceWithVol = calculatePutPrice(underlying, strike, vol, volSmileFactor);
                if (putPrice > newPutPriceWithVol || Double.isNaN(newPutPriceWithVol)) {
                    vol += step;
                    step *= 0.1;
                    putPriceWithVol = calculatePutPrice(underlying, strike, vol, volSmileFactor);
                }
                else {
                    putPriceWithVol = newPutPriceWithVol;
                }
            }
        }
        return vol;
    }

    public static void main(String[] args) {
        int daysUntilExpiry = 5;
        BinomialModelPricer bmp = new BinomialModelPricer(daysUntilExpiry, 2);

        double interest = 0;
        double volSmileFactor = 0.02;
        double underlying = 57.66;
        double strike = 57.5;
        double strikeIncrement = 0.5;
        double lowStrike = strike - 4 * strikeIncrement;
        double highStrike = strike + 4 * strikeIncrement;

        System.out.println(interest);
        double bid = 0.42;
        double ask = 0.44;

        double vol = bmp.calculateImpliedVol((bid + ask) / 2, underlying, strike, interest, volSmileFactor);

        for (double p = lowStrike; p <= highStrike; p += strikeIncrement) {
            double price = bmp.calculatePutPrice(underlying, p, vol, volSmileFactor);
            double price2 = bmp.calculatePutPriceWithProb(underlying, p, 1.008473, 0.64285);
            System.out.printf("%.2f: %f\n", p, price);
        }
    }
}
