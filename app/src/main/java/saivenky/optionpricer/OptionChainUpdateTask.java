package saivenky.optionpricer;

import saivenky.data.OptionChainRetriever;
import saivenky.data.Stock;

/**
 * Created by saivenky on 1/1/17.
 */

public class OptionChainUpdateTask implements Runnable {

    private long validityTimeMillis;
    private IDisplayUpdateNotifier displayUpdateNotifier;

    public OptionChainUpdateTask(long validityTimeMillis, IDisplayUpdateNotifier displayUpdateNotifier) {

        this.validityTimeMillis = validityTimeMillis;
        this.displayUpdateNotifier = displayUpdateNotifier;
    }

    @Override
    public void run() {
        System.out.println("Updating option chain data");
        if (OptionChainRetriever.DEFAULT.lastUpdate + validityTimeMillis > System.currentTimeMillis()) {
            System.out.println("Options data still valid. Skipping update");
            return;
        }

        OptionChainRetriever.DEFAULT.retrieveDataForAll();
        this.displayUpdateNotifier.updateStockPriceOnUi(Stock.DEFAULT.regularMarketPrice);
    }
}
