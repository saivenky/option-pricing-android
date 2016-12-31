package saivenky.pricing;

/**
 * Created by saivenky on 12/29/16.
 */

public class Theo {
    public double price;
    public double delta;
    public double gamma;
    public double vega;
    public double theta;

    @Override
    public String toString() {
        return String.format("(PRICE: %.4f, DELTA: %.4f, GAMMA: %.4f, VEGA: %.4f, THETA: %.4f)", price, delta, gamma, vega, theta);
    }

    public void multiplyWithSize(double size) {
        price *= size;
        delta *= size;
        gamma *= size;
        vega *= size;
        theta *= size;
    }

    public void add(Theo theo) {
        price += theo.price;
        delta += theo.delta;
        gamma += theo.gamma;
        vega += theo.vega;
        theta += theo.theta;
    }

    private static String SEPARATOR = "-------------------";

    public String prettyString() {
        return String.format(
                SEPARATOR + "\nPRICE: %.4f\nDELTA: %.4f\nGAMMA: %.4f\nVEGA: %.4f\nTHETA: %.4f\n" + SEPARATOR, price, delta, gamma, vega, theta/ IPricer.TRADING_DAYS);
    }
}
