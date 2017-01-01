package saivenky.optionpricer;

import saivenky.data.OptionChainRetriever;

/**
 * Created by saivenky on 1/1/17.
 */

public class OptionChainDataUpdateTask implements Runnable {

    @Override
    public void run() {
        System.out.println("Updating option chain data");
        OptionChainRetriever.DEFAULT.retrieveDataForAll();
    }
}
