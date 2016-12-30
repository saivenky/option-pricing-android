package saivenky.optionpricer;

/**
 * Created by saivenky on 12/29/16.
 */

public class BlackScholesPrice {
    double price;
    double delta;
    double gamma;

    @Override
    public String toString() {
        return String.format("%.4f (delta: %.4f, gamma: %.4f)", price, delta, gamma);
    }
}
