package saivenky.optionpricer;

import android.app.Activity;
import android.app.NotificationManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.StringReader;

import saivenky.data.OptionChainRetriever;
import saivenky.trading.TradeSet;
import saivenky.trading.TradeSetReader;

public class MainActivity extends AppCompatActivity {
    private static final long OPTION_CHAIN_LOOP_INTERVAL_MILLIS = 180000;
    private static final long LARGE_HEDGE_NOTIFY_LOOP_INTERVAL_MILLIS = 10000;
    private EditText mTrades;
    private Button mPnlButton;
    private TextView mPnlView;
    private WorkerThread workerThread;

    LoopingTask optionChainDataUpdateLoopingTask;
    LoopingTask largeDeltaHedgeNotifyLoopingTask;

    private TradeSet tradeSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTrades = (EditText) findViewById(R.id.trades);
        mPnlView = (TextView) findViewById(R.id.pnl_view);
        mPnlButton = (Button) findViewById(R.id.pnl_button);
        mPnlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                readTradesAndCalculatePnl();
            }
        });

        tradeSet = new TradeSet();
        workerThread = new WorkerThread();
        workerThread.start();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        optionChainDataUpdateLoopingTask =
                new LoopingTask(workerThread.getHandler(), new OptionChainDataUpdateTask(), OPTION_CHAIN_LOOP_INTERVAL_MILLIS);
        LargeDeltaHedgeNotifyTask deltaHedgeTask = new LargeDeltaHedgeNotifyTask(notificationManager, this, tradeSet);
        largeDeltaHedgeNotifyLoopingTask = new LoopingTask(workerThread.getHandler(), deltaHedgeTask, LARGE_HEDGE_NOTIFY_LOOP_INTERVAL_MILLIS);

        optionChainDataUpdateLoopingTask.start();
    }

    private void readTradesAndCalculatePnl() {
        final String tradesText = mTrades.getText().toString();

        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    largeDeltaHedgeNotifyLoopingTask.cancel();
                    StringReader reader = new StringReader(tradesText);
                    TradeSetReader tsr = new TradeSetReader();
                    StringBuilder sb = new StringBuilder();
                    tradeSet.clearTrades();
                    tsr.addToSet(reader, tradeSet);

                    OptionChainRetriever.DEFAULT.retrieveDataForAll();
                    sb.append(tradeSet.describePnL());
                    sb.append(tradeSet.describeTheo());
                    largeDeltaHedgeNotifyLoopingTask.start();
                    return sb.toString();
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

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                activity.getCurrentFocus().getWindowToken(), 0);
    }
}
