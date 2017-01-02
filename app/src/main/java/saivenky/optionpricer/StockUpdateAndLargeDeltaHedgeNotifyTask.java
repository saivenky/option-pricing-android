package saivenky.optionpricer;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import saivenky.data.Stock;
import saivenky.trading.DeltaHedgeResult;
import saivenky.trading.LargeDeltaHedgeChecker;
import saivenky.trading.TradeSet;

/**
 * Created by saivenky on 1/1/17.
 */

public class StockUpdateAndLargeDeltaHedgeNotifyTask implements Runnable {
    private static final int NOTIFICATION_ID = 001;
    private final LargeDeltaHedgeChecker largeDeltaHedgeChecker;
    private NotificationManager notificationManager;
    private final TradeSet tradeSet;
    private NotificationCompat.Builder notificationBuilder;

    public StockUpdateAndLargeDeltaHedgeNotifyTask(NotificationManager notificationManager, Context context, TradeSet tradeSet) {
        this.largeDeltaHedgeChecker = new LargeDeltaHedgeChecker();
        this.notificationManager = notificationManager;
        this.tradeSet = tradeSet;
        notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_optionpricer)
                .setContentTitle("Large Delta Position")
                .setPriority(Notification.PRIORITY_MAX);
    }

    @Override
    public void run() {
        System.out.println("Updating stock and checking for large delta");
        Stock.DEFAULT.getData();
        DeltaHedgeResult result = largeDeltaHedgeChecker.check(tradeSet);
        if(result.isHedgeNeeded) {
            String message = createNotificationMessage(result);
            notificationBuilder.setContentText(message);
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }
        else {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    private String createNotificationMessage(DeltaHedgeResult result) {
        String message = String.format("Current delta: %.1f, Hedge needed", result.currentDelta);
        return message;
    }
}
