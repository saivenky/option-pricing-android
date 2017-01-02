package saivenky.optionpricer;

import saivenky.data.OptionChainRetriever;

/**
 * Created by saivenky on 1/1/17.
 */

public class OptionChainUpdateTask implements Runnable {

    private long validityTimeMillis;

    public OptionChainUpdateTask(long validityTimeMillis) {

        this.validityTimeMillis = validityTimeMillis;
    }

    @Override
    public void run() {
        System.out.println("Updating option chain data");
        if (OptionChainRetriever.DEFAULT.lastUpdate + validityTimeMillis > System.currentTimeMillis()) {
            System.out.println("Options data still valid. Skipping update");
            return;
        }

        OptionChainRetriever.DEFAULT.retrieveDataForAll();
    }
}
