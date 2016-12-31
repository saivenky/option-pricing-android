package saivenky.trading;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import saivenky.data.OptionChain;

public class TradeSetReader {
    public TradeSet create(Reader reader) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line;

        TradeSet tradeSet = new TradeSet();
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
        return tradeSet;
    }

    public static void main(String[] args) throws IOException {
        File f = new File("/home/saivenky/desktop/trades.txt");
        FileReader reader = new FileReader("/home/saivenky/desktop/trades.txt");
        TradeSetReader tsr = new TradeSetReader();
        TradeSet ts = tsr.create(reader);

        OptionChain optionChain = new OptionChain();
        optionChain.getData("2016-12-30");

        System.out.println(ts.describePnL());
        System.out.println(ts.describeTheo(optionChain));
    }
}
