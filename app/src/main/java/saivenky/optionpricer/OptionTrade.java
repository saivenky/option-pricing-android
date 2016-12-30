package saivenky.optionpricer;

public class OptionTrade implements ITrade {
    private static final BlackScholesPricer pricer = new BlackScholesPricer();

    static final double COMMISSION_COST = 6.95;
    static final double EA_COST = 6.95;
    static final double PER_CONTRACT_COST = 0.75;

    static final double CONTRACT_SIZE = 100;

    boolean isBuy;
    boolean isCall;
    double price;
    double strike;
    int quantity;
    String expiry;

    double getSize() {
        return quantity * CONTRACT_SIZE;
    }
    double getTradeCost() {
        return COMMISSION_COST + quantity * PER_CONTRACT_COST;
    }

    double getEaCost() {
        return EA_COST;
    }

    double getTradeCloseCostBasis() {
        double tradesCost = (getTradeCost()*2)/getSize();
        double sign = (isBuy) ? 1 : -1;
        return price + sign*tradesCost;
    }

    public double getValue(double underlying) {
        double value = underlying - strike;
        double sign = (isCall) ? 1 : -1;
        return Math.max(0, sign * value * getSize());
    }

    private double getExecutionCost(double underlying) {
        if(isCall) {
            if(isBuy && underlying >= strike) {
                return EA_COST;
            }
            if(!isBuy && underlying >= strike) {
                return EA_COST;
            }
        }
        if(!isCall) {
            if(isBuy && underlying <= strike) {
                return EA_COST;
            }
            if(!isBuy && underlying <= strike) {
                return EA_COST;
            }
        }
        return 0;
    }

    double getInitialPnL() {
        double sign = isBuy ? -1 : 1;
        double value = sign * price * getSize();
        return value - getTradeCost();
    }

    public double getPnL(double underlying) {
        double sign = isBuy ? 1 : -1;
        return getInitialPnL() + sign*getValue(underlying) - getExecutionCost(underlying);
    }

    @Override
    public double getStrike() {
        return strike;
    }

    public double getBreakevenUnderlyingPrice() {
        double buySign = isBuy ? 1 : -1;
        double callSign = isCall ? 1 : -1;
        double profitSign = buySign * callSign;
        return strike - (profitSign*getInitialPnL())  / getSize();
    }

    @Override
    public String toString() {
        return String.format("%s%d x %.2f %s @ %.2f %s", isBuy ? "+" : "-", quantity, strike, isCall ? "C" : "P", price, expiry);
    }

    public String fullDescription() {
        String simple = this.toString();
        double initialCost = getInitialPnL();
        double buySign = isBuy ? 1 : -1;
        double callSign = isCall ? 1 : -1;
        double profitSign = buySign * callSign;
        double breakevenUnderlyingPrice = strike - (profitSign*initialCost)  / getSize();
        double breakevenEaUnderlyingPrice = strike - (profitSign*(initialCost - getEaCost())) / getSize();
        double minEaUnderlyingPrice = strike + callSign * EA_COST / getSize();
        return String.format("%s\nInitial PnL: %.2f\nBreakeven Price: %.2f\nBreakeven OptionTrade Price: %.2f\nBreakeven EA Price (min %.2f): %.2f",
                simple, initialCost, breakevenUnderlyingPrice, getTradeCloseCostBasis(), minEaUnderlyingPrice, breakevenEaUnderlyingPrice);
    }

    public BlackScholesPrice getTheo(double underlying, double impliedVol) {
        BlackScholesPrice bsp = this.isCall ?
                pricer.callPrice(underlying, strike, BlackScholesPricer.timeToExpiry(expiry), impliedVol) :
                pricer.putPrice(underlying, strike, BlackScholesPricer.timeToExpiry(expiry), impliedVol);
        double sign = (isBuy) ? 1 : -1;
        bsp.delta *= sign * getSize();
        return bsp;
    }

    void describeRisk() {
        double downRisk = getPnL(0);
        double upRisk = getPnL(MAX_UNDERLYING);
        if (downRisk < 0) {
            System.out.println("Down Risk");
        }
        if(upRisk < 0) {
            System.out.println("Up Risk");
        }
    }

    public static OptionTrade parse(String text) {
        OptionTrade trade = new OptionTrade();
        String[] split = text.split(" ");
        int size = Integer.parseInt(split[0]);
        trade.isBuy = !(size < 0);
        trade.quantity = size;
        if (!trade.isBuy) trade.quantity *= -1;
        trade.strike = Double.parseDouble(split[2]);
        trade.isCall = split[3].equals("C");
        trade.price = Double.parseDouble(split[5]);
        trade.expiry = split[6];
        return trade;
    }

    public static void main(String[] args) {
        OptionTrade trade = new OptionTrade();
        trade.isBuy = false;
        trade.isCall = true;
        trade.price = 0.4;
        trade.strike = 57.5;
        trade.quantity = 3;
        trade.expiry = "2016-12-30";
        System.out.println(trade.fullDescription());
        trade.describeRisk();
        OptionTrade trade2 = OptionTrade.parse("-3 x 57.5 C @ 0.4 2016-12-30");
        System.out.println(trade2.fullDescription());
    }
}
