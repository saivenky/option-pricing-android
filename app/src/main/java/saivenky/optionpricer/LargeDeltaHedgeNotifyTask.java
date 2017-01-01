package saivenky.optionpricer;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.renderscript.RenderScript;
import android.support.v4.app.NotificationCompat;

import saivenky.trading.LargeDeltaHedgeChecker;
import saivenky.trading.TradeSet;

/**
 * Created by saivenky on 1/1/17.
 */

public class LargeDeltaHedgeNotifyTask implements Runnable {
    private static final int NOTIFICATION_ID = 001;
    private final LargeDeltaHedgeChecker largeDeltaHedgeChecker;
    private NotificationManager notificationManager;
    private final TradeSet tradeSet;
    private NotificationCompat.Builder notificationBuilder;

    public LargeDeltaHedgeNotifyTask(NotificationManager notificationManager, Context context, TradeSet tradeSet) {
        this.largeDeltaHedgeChecker = new LargeDeltaHedgeChecker();
        this.notificationManager = notificationManager;
        this.tradeSet = tradeSet;
        notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_optionpricer)
                .setContentTitle("Hedge trades")
                .setContentText("Large delta position. Hedge trades now.")
                .setPriority(Notification.PRIORITY_MAX);
    }

    @Override
    public void run() {
        if(largeDeltaHedgeChecker.check(tradeSet)) {
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }
        else {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }
}
