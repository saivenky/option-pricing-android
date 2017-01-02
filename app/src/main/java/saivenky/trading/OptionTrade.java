package saivenky.trading;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import saivenky.pricing.IPricer;
import saivenky.pricing.Theo;
import saivenky.pricing.BlackScholesPricer;

public class OptionTrade implements ITrade {
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

    double getTotalQuantity() {
        return quantity * CONTRACT_SIZE;
    }
    double getTradeCost() {
        return COMMISSION_COST + quantity * PER_CONTRACT_COST;
    }

    double getEaCost() {
        return EA_COST;
    }

    double getTradeCloseCostBasis() {
        double tradesCost = (getTradeCost()*2)/ getTotalQuantity();
        double sign = (isBuy) ? 1 : -1;
        return price + sign*tradesCost;
    }

    public double getValue(double underlying) {
        double value = underlying - strike;
        double sign = (isCall) ? 1 : -1;
        return Math.max(0, sign * value * getTotalQuantity());
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
        double value = sign * price * getTotalQuantity();
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
        return strike - (profitSign*getInitialPnL())  / getTotalQuantity();
    }

    @Override
    public String toString() {
        return String.format("O %s%d %.2f%s %.2f %s", isBuy ? "+" : "-", quantity, strike, isCall ? "+" : "-", price, formatToInputExpiry(expiry));
    }

    public String fullDescription() {
        String simple = this.toString();
        double initialCost = getInitialPnL();
        double buySign = isBuy ? 1 : -1;
        double callSign = isCall ? 1 : -1;
        double profitSign = buySign * callSign;
        double breakevenUnderlyingPrice = strike - (profitSign*initialCost)  / getTotalQuantity();
        double breakevenEaUnderlyingPrice = strike - (profitSign*(initialCost - getEaCost())) / getTotalQuantity();
        double minEaUnderlyingPrice = strike + callSign * EA_COST / getTotalQuantity();
        return String.format("%s\nInitial PnL: %.2f\nBreakeven Price: %.2f\nBreakeven OptionTrade Price: %.2f\nBreakeven EA Price (min %.2f): %.2f",
                simple, initialCost, breakevenUnderlyingPrice, getTradeCloseCostBasis(), minEaUnderlyingPrice, breakevenEaUnderlyingPrice);
    }

    public Theo getTheo(double underlying, double impliedVol) {
        Theo theo = BlackScholesPricer.DEFAULT.getTheo(isCall, underlying, strike, IPricer.timeToExpiry(expiry), impliedVol);
        int sign = (isBuy) ? 1 : -1;
        double size = sign * getTotalQuantity();
        theo.multiplyWithSize(size);
        return theo;
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
        //+3 54+ 0.33 1/6
        OptionTrade trade = new OptionTrade();
        String[] split = text.split(" ");
        int size = Integer.parseInt(split[0]);
        trade.isBuy = !(size < 0);
        trade.quantity = size;
        if (!trade.isBuy) trade.quantity *= -1;

        String callPutAndStrike = split[1];
        trade.isCall = isCall(callPutAndStrike.charAt(callPutAndStrike.length() - 1));
        trade.strike = Double.parseDouble(callPutAndStrike.substring(0, callPutAndStrike.length() - 1));
        trade.price = Double.parseDouble(split[2]);

        trade.expiry = parseExpiry(split[3]);
        return trade;
    }

    private static boolean isCall(char callPut) {
        if(callPut == '+') return true;
        else if (callPut == '-') return false;
        return true;
    }


    private static final SimpleDateFormat EXPIRY_INPUT_FORMAT = new SimpleDateFormat("M/d");
    private static final SimpleDateFormat EXPIRY_OUTPUT_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final Calendar CALENDAR = Calendar.getInstance();

    static {
        EXPIRY_OUTPUT_FORMAT.setTimeZone(CALENDAR.getTimeZone());
    }

    private static String parseExpiry(String expiryInput) {
        Date date;
        try {
            date = EXPIRY_INPUT_FORMAT.parse(expiryInput);
        } catch (ParseException e) {
            System.err.println("Bad expiry input: " + e.getMessage());
            date = new Date();
        }
        int currentYear = CALENDAR.get(Calendar.YEAR);
        CALENDAR.setTime(date);
        CALENDAR.set(Calendar.YEAR, currentYear);
        return EXPIRY_OUTPUT_FORMAT.format(CALENDAR.getTime());
    }

    private static String formatToInputExpiry(String outputExpiry) {
        Date date;
        try {
            date = EXPIRY_OUTPUT_FORMAT.parse(outputExpiry);
        } catch (ParseException e) {
            System.err.println("Bad expiry input: " + e.getMessage());
            date = new Date();
        }

        return EXPIRY_INPUT_FORMAT.format(date);
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
