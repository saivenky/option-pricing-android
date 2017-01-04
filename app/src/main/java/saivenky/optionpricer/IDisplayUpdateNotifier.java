package saivenky.optionpricer;

import java.util.Map;

import saivenky.pricing.Theo;

/**
 * Created by saivenky on 1/4/17.
 */
public interface IDisplayUpdateNotifier {
    void updateStockPrice(double underlying);
    void updateTotalTheo(Theo totalTheo);
    void updateCurrentPnl(double pnl);
    void updateClosePnl(double pnl);
    void updatePnl(Map<Double, Double> priceToPnl);

    void updateStockPriceOnUi(double underlying);
    void updateTotalTheoOnUi(Theo totalTheo);
    void updateCurrentPnlOnUi(double pnl);
    void updateClosePnlOnUi(double pnl);
    void updatePnlOnUi(Map<Double, Double> priceToPnl);

}
