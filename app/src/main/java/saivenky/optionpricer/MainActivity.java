package saivenky.optionpricer;

import android.app.NotificationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.StringReader;

import saivenky.data.OptionChainRetriever;
import saivenky.data.Stock;
import saivenky.pricing.Theo;
import saivenky.trading.StockTrade;
import saivenky.trading.TradeSet;
import saivenky.trading.TradeSetReader;

public class MainActivity extends AppCompatActivity {
    private static final long OPTION_CHAIN_LOOP_INTERVAL_MILLIS = 180000;
    private static final long STOCK_UPDATE_AND_LARGE_HEDGE_NOTIFY_LOOP_INTERVAL_MILLIS = 5000;
    private static final long STOCK_UPDATE_AND_LARGE_HEDGE_NOTIFY_LOOP_SLOW_INTERVAL_MILLIS = 60000;

    private static final long OPTION_CHAIN_VALIDITY_MILLIS = OPTION_CHAIN_LOOP_INTERVAL_MILLIS / 2;
    private static final long STOCK_VALIDITY_MILLIS = 5000;

    private EditText mUnderlying;
    private EditText mTrades;
    private TextView mPnlView;
    private Button mPnlButton;
    private Button mQuoteButton;
    private Button mHedgeButton;
    private Button mUpdatesButton;
    private boolean isFastUpdating;

    private WorkerThread workerThread;

    LoopingTask optionChainDataUpdateLoopingTask;
    LoopingTask largeDeltaHedgeNotifyLoopingTask;

    private TradeSet tradeSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Stock.initialize("SBUX");
        OptionChainRetriever.initialize(Stock.DEFAULT);

        mUnderlying = (EditText) findViewById(R.id.underlying);
        mTrades = (EditText) findViewById(R.id.trades);
        mPnlView = (TextView) findViewById(R.id.pnl_view);
        mPnlButton = (Button) findViewById(R.id.pnl_button);
        mQuoteButton = (Button) findViewById(R.id.quote_button);
        mHedgeButton = (Button) findViewById(R.id.hedge_button);
        mUpdatesButton = (Button) findViewById(R.id.updates_button);

        mPnlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                readTradesAndCalculatePnl();
            }
        });
        mQuoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getQuotesForOptions();
            }
        });
        mHedgeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addHedge();
            }
        });

        tradeSet = new TradeSet();
        workerThread = new WorkerThread();
        workerThread.start();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        OptionChainUpdateTask optionChainUpdateTask = new OptionChainUpdateTask(OPTION_CHAIN_VALIDITY_MILLIS);
        optionChainDataUpdateLoopingTask = new LoopingTask(workerThread.getHandler(), optionChainUpdateTask, OPTION_CHAIN_LOOP_INTERVAL_MILLIS);
        StockUpdateAndLargeDeltaHedgeNotifyTask deltaHedgeTask = new StockUpdateAndLargeDeltaHedgeNotifyTask(notificationManager, this, tradeSet, STOCK_VALIDITY_MILLIS);
        largeDeltaHedgeNotifyLoopingTask = new LoopingTask(workerThread.getHandler(), deltaHedgeTask, STOCK_UPDATE_AND_LARGE_HEDGE_NOTIFY_LOOP_INTERVAL_MILLIS);

        optionChainDataUpdateLoopingTask.start();

        isFastUpdating = true;
        mUpdatesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isFastUpdating = !isFastUpdating;
                if(isFastUpdating) {
                    largeDeltaHedgeNotifyLoopingTask.updateInterval(STOCK_UPDATE_AND_LARGE_HEDGE_NOTIFY_LOOP_INTERVAL_MILLIS);
                }
                else {
                    largeDeltaHedgeNotifyLoopingTask.updateInterval(STOCK_UPDATE_AND_LARGE_HEDGE_NOTIFY_LOOP_SLOW_INTERVAL_MILLIS);
                }
                mUpdatesButton.setText(isFastUpdating ? "Slower" : "Faster");
            }
        });
    }

    private void addHedge() {
        if (tradeSet.isEmpty()) return;

        final String underlying = mUnderlying.getText().toString();
        double underlyingPrice;
        if(underlying == null || underlying.isEmpty()) {
            underlyingPrice = Stock.DEFAULT.regularMarketPrice;
        }
        else {
            underlyingPrice = Double.parseDouble(underlying);
        }

        Theo theo = tradeSet.getTheo(underlyingPrice);
        StockTrade stockTrade = StockTrade.createHedgeTrade(theo, underlyingPrice, DELTA_HEDGE_ROUNDING);
        tradeSet.addTrade(stockTrade);
        mTrades.setText(mTrades.getText() + "\n" + stockTrade.toString());
    }

    private static int DELTA_HEDGE_ROUNDING = 10;

    private void getQuotesForOptions() {
        final String tradesText = mTrades.getText().toString();

        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    StringReader reader = new StringReader(tradesText);
                    TradeSetReader tsr = new TradeSetReader();
                    tradeSet.clearTrades();
                    tsr.addToSet(reader, tradeSet);

                    OptionChainRetriever.DEFAULT.retrieveDataForAll();

                    return tradeSet.getQuotesForOptions();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(String result) {
                mPnlView.setText(result);
            }
        };

        task.execute();
    }

    private void readTradesAndCalculatePnl() {
        final String tradesText = mTrades.getText().toString();
        final String underlying = mUnderlying.getText().toString();

        AsyncTask<Void, Void, PnlAndTheoResult> task = new AsyncTask<Void, Void, PnlAndTheoResult>() {
            @Override
            protected PnlAndTheoResult doInBackground(Void... voids) {
                try {
                    largeDeltaHedgeNotifyLoopingTask.cancel();
                    StringReader reader = new StringReader(tradesText);
                    TradeSetReader tsr = new TradeSetReader();
                    tradeSet.clearTrades();
                    tsr.addToSet(reader, tradeSet);

                    OptionChainRetriever.DEFAULT.retrieveDataForAll();

                    double underlyingPrice;
                    if(underlying == null || underlying.isEmpty()) {
                        underlyingPrice = Stock.DEFAULT.regularMarketPrice;
                    }
                    else {
                        underlyingPrice = Double.parseDouble(underlying);
                    }
                    PnlAndTheoResult result = new PnlAndTheoResult();
                    result.priceToPnlDescription = tradeSet.describePnL();
                    result.theoDescription = tradeSet.describeTheo(underlyingPrice);
                    largeDeltaHedgeNotifyLoopingTask.start();

                    return result;
                }
                catch (Exception e) {
                    PnlAndTheoResult result = new PnlAndTheoResult();
                    result.priceToPnlDescription = "Error reading trades and calculating PnL";
                    result.theoDescription = e.getMessage();
                    e.printStackTrace();
                    return result;
                }
            }

            @Override
            protected void onPostExecute(PnlAndTheoResult result) {
                mPnlView.setText(result.priceToPnlDescription + "\n" + result.theoDescription);
            }
        };

        task.execute();

    }

    private class PnlAndTheoResult {
        String priceToPnlDescription;
        String theoDescription;
    }
}


