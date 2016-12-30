package saivenky.optionpricer;

public class StockTrade implements ITrade {

    double price;
    int quantity;

    @Override
    public double getValue(double underlying) {
        return (underlying - price) * quantity;
    }

    @Override
    public double getPnL(double underlying) {
        return getValue(underlying);
    }

    @Override
    public double getStrike() {
        return price;
    }

    public static StockTrade parse(String text) {
        StockTrade trade = new StockTrade();
        String[] split = text.split(" ");
        trade.quantity = Integer.parseInt(split[0]);
        trade.price = Double.parseDouble(split[2]);
        return trade;
    }

    @Override
    public double getBreakevenUnderlyingPrice() {
        return price;
    }

    @Override
    public String toString() {
        return String.format("%d @ %.2f", quantity, price);
    }

    @Override
    public String fullDescription() {
        String simple = toString();
        return simple;
    }
}
