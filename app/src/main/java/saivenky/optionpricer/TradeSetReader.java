package saivenky.optionpricer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

public class TradeSetReader {
    TradeSet create(Reader reader) throws IOException {
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

        OptionsData data = new OptionsData();
        data.getData("2016-12-30");

        double delta = 0;
        //data.stock.regularMarketPrice = 55.90;
        double underlying = data.stock.regularMarketPrice;
        for(ITrade trade : ts.trades) {
            System.out.println("\n"+trade.fullDescription());
            if(trade instanceof OptionTrade) {
                OptionTrade optionTrade = (OptionTrade) trade;
                OptionLine line = data.getOptionLine(optionTrade.isCall, optionTrade.strike);
                BlackScholesPrice bsp = optionTrade.getTheo(underlying, line.calculatedIV);
                System.out.printf("%.2f last(%.2f) (%.2f %.2f): %.4f %.4f\n", line.strike, line.lastPrice, line.bid, line.ask, line.calculatedIV, bsp.delta);
                delta += bsp.delta;
            }
            else {
                StockTrade stockTrade = (StockTrade) trade;
                delta += stockTrade.getTheo(underlying, 0).delta;
            }
        }

        System.out.println("\n---------------------");
        ts.describePnL();
        System.out.printf("\nDelta: %.4f\n", delta);
        ts.describeAtPrice(underlying, ts.getPnl(underlying));
    }
}
