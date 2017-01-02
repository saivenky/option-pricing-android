package saivenky.trading;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import saivenky.data.OptionChain;
import saivenky.data.OptionChainRetriever;
import saivenky.data.Stock;

public class TradeSetReader {
    public TradeSet create(Reader reader) throws IOException {
        TradeSet tradeSet = new TradeSet();
        addToSet(reader, tradeSet);
        return tradeSet;
    }

    public void addToSet(Reader reader, TradeSet tradeSet) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line;

        while((line = bufferedReader.readLine()) != null) {
            if(line.isEmpty()) continue;
            ITrade trade;
            switch(line.charAt(0)) {
                case 'O': trade = OptionTrade.parse(line.substring(2)); break;
                case 'S': trade = StockTrade.parse(line.substring(2)); break;
                default: trade = null;
            }
            if (trade != null) tradeSet.addTrade(trade);
        }

        bufferedReader.close();
    }

    public static void main(String[] args) throws IOException {
        Stock.initialize("SBUX");
        OptionChainRetriever.initialize(Stock.DEFAULT);

        FileReader reader = new FileReader("/home/saivenky/desktop/trades.txt");
        TradeSetReader tsr = new TradeSetReader();
        TradeSet ts = tsr.create(reader);

        OptionChainRetriever.DEFAULT.retrieveDataForAll();
        System.out.println(ts.describePnL());
        System.out.println(ts.describeTheo(Stock.DEFAULT.regularMarketPrice));
    }
}
