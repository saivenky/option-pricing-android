package saivenky.data;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by saivenky on 12/31/16.
 */

public class OptionChainRetriever {
    public static OptionChainRetriever DEFAULT;
    public static void initialize(Stock stock) {
        DEFAULT = new OptionChainRetriever(stock);
    }

    ConcurrentHashMap<String, OptionChain> optionsByExpiry;

    private Stock stock;

    private OptionChainRetriever(Stock stock) {
        this.stock = stock;
        optionsByExpiry = new ConcurrentHashMap<>();
    }

    public OptionChain getOptionChain(String expiry) {
        if(optionsByExpiry.containsKey(expiry)) return optionsByExpiry.get(expiry);
        return null;
    }

    public void addExpiry(String expiry) {
        if(optionsByExpiry.containsKey(expiry)) return;
        OptionChain options = new OptionChain(expiry, stock);
        optionsByExpiry.put(expiry, options);
    }

    public void retrieveDataForAll() {
        for(OptionChain options : optionsByExpiry.values()) {
            try {
                options.getData();
            } catch(Exception e) {}
        }
    }

    public void removeExpiry(String expiry) {
        optionsByExpiry.remove(expiry);
    }
}
