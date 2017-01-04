package saivenky.optionpricer;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.StringReader;
import java.util.Map;

import saivenky.data.OptionChainRetriever;
import saivenky.data.Stock;
import saivenky.pricing.Theo;
import saivenky.trading.StockTrade;
import saivenky.trading.TradeSet;
import saivenky.trading.TradeSetReader;

import static saivenky.pricing.IPricer.TRADING_DAYS;

public class MainActivity extends AppCompatActivity implements IDisplayUpdateNotifier {
    private static final long OPTION_CHAIN_LOOP_INTERVAL_MILLIS = 180000;
    private static final long STOCK_UPDATE_AND_LARGE_HEDGE_NOTIFY_LOOP_INTERVAL_MILLIS = 5000;
    private static final long STOCK_UPDATE_AND_LARGE_HEDGE_NOTIFY_LOOP_SLOW_INTERVAL_MILLIS = 60000;

    private static final long OPTION_CHAIN_VALIDITY_MILLIS = OPTION_CHAIN_LOOP_INTERVAL_MILLIS / 2;
    private static final long STOCK_VALIDITY_MILLIS = 5000;

    private static final String SHARED_PREFERENCES_TRADES_KEY = BuildConfig.APPLICATION_ID + ".MainActivity.trades";

    private Handler uiHandler = new Handler(Looper.getMainLooper());

    private EditText mUnderlying;
    private EditText mTrades;
    private TextView mPnlView;
    private Button mPnlButton;
    private Button mQuoteButton;
    private Button mHedgeButton;
    private Button mUpdatesButton;

    private EditText mCurrentUnderlying;
    private EditText mCurrentPnl;
    private EditText mClosePnl;
    private EditText mCurrentPrice;
    private EditText mCurrentDelta;
    private EditText mCurrentGamma;
    private EditText mCurrentVega;
    private EditText mCurrentTheta;

    private boolean isFastUpdating;

    private WorkerThread workerThread;
    LoopingTask optionChainDataUpdateLoopingTask;
    LoopingTask largeDeltaHedgeNotifyLoopingTask;

    private TradeSet tradeSet;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveTradesText();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveTradesText();
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveTradesText();
    }

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

        mCurrentUnderlying = (EditText) findViewById(R.id.current_underlying);
        mCurrentPnl = (EditText) findViewById(R.id.current_pnl);
        mClosePnl = (EditText) findViewById(R.id.current_close_pnl);
        mCurrentPrice = (EditText) findViewById(R.id.current_price);
        mCurrentDelta = (EditText) findViewById(R.id.current_delta);
        mCurrentGamma = (EditText) findViewById(R.id.current_gamma);
        mCurrentVega = (EditText) findViewById(R.id.current_vega);
        mCurrentTheta = (EditText) findViewById(R.id.current_theta);

        loadTradesText();

        tradeSet = new TradeSet();
        workerThread = new WorkerThread();
        workerThread.start();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        OptionChainUpdateTask optionChainUpdateTask = new OptionChainUpdateTask(OPTION_CHAIN_VALIDITY_MILLIS, this);
        optionChainDataUpdateLoopingTask = new LoopingTask(workerThread.getHandler(), optionChainUpdateTask, OPTION_CHAIN_LOOP_INTERVAL_MILLIS);
        StockUpdateAndLargeDeltaHedgeNotifyTask deltaHedgeTask = new StockUpdateAndLargeDeltaHedgeNotifyTask(notificationManager, this, tradeSet, STOCK_VALIDITY_MILLIS, this);
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
                    result.currentUnderlying = underlyingPrice;
                    result.currentPnl = tradeSet.getPnl(underlyingPrice);
                    result.currentClosePnl = tradeSet.getClosePnl(underlyingPrice);
                    result.currentTheo = tradeSet.getTheo(underlyingPrice);
                    largeDeltaHedgeNotifyLoopingTask.start();

                    return result;
                }
                catch (Exception e) {
                    PnlAndTheoResult result = new PnlAndTheoResult();
                    result.priceToPnlDescription = "Error reading trades and calculating PnL";
                    e.printStackTrace();
                    return result;
                }
            }

            @Override
            protected void onPostExecute(PnlAndTheoResult result) {
                mPnlView.setText(result.priceToPnlDescription);
                updateManualStockPrice(result.currentUnderlying);
                updateManualCurrentPnl(result.currentPnl);
                updateManualClosePnl(result.currentClosePnl);
                updateManualTotalTheo(result.currentTheo);
            }
        };

        task.execute();

    }

    void loadTradesText() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        mTrades.setText(sharedPref.getString(SHARED_PREFERENCES_TRADES_KEY, ""));
    }

    void saveTradesText() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(SHARED_PREFERENCES_TRADES_KEY, mTrades.getText().toString());
        editor.commit();
    }

    boolean hasManualUnderlyingPrice() {
        String underlyingText = mUnderlying.getText().toString();
        return underlyingText != null && !underlyingText.isEmpty();
    }

    @Override
    public void updateStockPrice(double underlying) {
        if(hasManualUnderlyingPrice()) return;
        updateManualStockPrice(underlying);
    }

    void updateManualStockPrice(double underlying) {
        mCurrentUnderlying.setText(String.format("%.2f", underlying));
    }

    @Override
    public void updateTotalTheo(Theo totalTheo) {
        if(hasManualUnderlyingPrice()) return;
        updateManualTotalTheo(totalTheo);
    }

    public void updateManualTotalTheo(Theo totalTheo) {
        if(totalTheo == null) return;
        mCurrentPrice.setText(String.format("%.2f", totalTheo.price));
        mCurrentDelta.setText(String.format("%.1f", totalTheo.delta));
        mCurrentGamma.setText(String.format("%.3f", totalTheo.gamma / 100)); // delta change per 0.01 spot change
        mCurrentVega.setText(String.format("%.2f", totalTheo.vega / 100)); // $ change per 1% change in vol
        mCurrentTheta.setText(String.format("%.2f", totalTheo.theta / TRADING_DAYS)); // $ change per 1 trading day
    }

    @Override
    public void updateCurrentPnl(double pnl) {
        if(hasManualUnderlyingPrice()) return;
        updateManualCurrentPnl(pnl);
    }

    @Override
    public void updateClosePnl(double pnl) {
        if(hasManualUnderlyingPrice()) return;
        updateManualClosePnl(pnl);
    }

    private void updateManualClosePnl(double pnl) {
        mClosePnl.setText(String.format("%.2f", pnl));
    }

    public void updateManualCurrentPnl(double pnl) {
        mCurrentPnl.setText(String.format("%.2f", pnl));
    }

    @Override
    public void updatePnl(Map<Double, Double> priceToPnl) {
        if(hasManualUnderlyingPrice()) return;
        updateManualPnl(priceToPnl);
    }

    public void updateManualPnl(Map<Double, Double> priceToPnl) {
    }

    private class PnlAndTheoResult {
        String priceToPnlDescription;
        double currentPnl;
        double currentClosePnl;
        double currentUnderlying;
        Theo currentTheo;
    }

    public void updateStockPriceOnUi(final double underlying) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                updateStockPrice(underlying);
            }
        });
    }

    public void updateTotalTheoOnUi(final Theo totalTheo) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                updateTotalTheo(totalTheo);
            }
        });
    }

    @Override
    public void updateCurrentPnlOnUi(final double pnl) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                updateCurrentPnl(pnl);
            }
        });
    }

    @Override
    public void updateClosePnlOnUi(final double pnl) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                updateClosePnl(pnl);
            }
        });
    }

    public void updatePnlOnUi(final Map<Double, Double> priceToPnl) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                updatePnl(priceToPnl);
            }
        });
    }
}


