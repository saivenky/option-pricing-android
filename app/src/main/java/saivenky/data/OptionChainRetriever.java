package saivenky.data;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by saivenky on 12/31/16.
 */

public class OptionChainRetriever {
    public static final OptionChainRetriever DEFAULT = new OptionChainRetriever();
    ConcurrentHashMap<String, OptionChain> optionsByExpiry;

    public double underlying;

    private OptionChainRetriever() {
        optionsByExpiry = new ConcurrentHashMap<>();
        underlying = Double.NaN;
    }

    public OptionChain getOptionChain(String expiry) {
        if(optionsByExpiry.containsKey(expiry)) return optionsByExpiry.get(expiry);
        return null;
    }

    public void addExpiry(String expiry) {
        if(optionsByExpiry.containsKey(expiry)) return;
        OptionChain options = new OptionChain();
        optionsByExpiry.put(expiry, options);
    }

    public void retrieveDataForAll() {
        for(String expiry : optionsByExpiry.keySet()) {
            OptionChain options = optionsByExpiry.get(expiry);
            try {
                options.getData(expiry);
            } catch(Exception e) {}
            underlying = options.stock.regularMarketPrice;
        }
    }

    public void removeExpiry(String expiry) {
        optionsByExpiry.remove(expiry);
    }
}
