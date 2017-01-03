package saivenky.optionpricer;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;
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
    private static final long[] VIBRATE_PATTERN = { 0, 300, 100, 300, 100, 300 };
    private static final Uri NOTIFICATION_SOUND = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

    private final LargeDeltaHedgeChecker largeDeltaHedgeChecker;
    private NotificationManager notificationManager;
    private final TradeSet tradeSet;
    private NotificationCompat.Builder notificationBuilder;
    private long validityTimeMillis;

    public StockUpdateAndLargeDeltaHedgeNotifyTask(NotificationManager notificationManager, Context context, TradeSet tradeSet, long validityTimeMillis) {
        this.largeDeltaHedgeChecker = new LargeDeltaHedgeChecker();
        this.notificationManager = notificationManager;
        this.tradeSet = tradeSet;
        this.validityTimeMillis = validityTimeMillis;
        notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_optionpricer)
                .setContentTitle("Large Delta Position")
                .setPriority(Notification.PRIORITY_MAX)
                .setVibrate(VIBRATE_PATTERN)
                .setSound(NOTIFICATION_SOUND);
    }

    @Override
    public void run() {
        System.out.println("Updating stock and checking for large delta");
        if (Stock.DEFAULT.lastUpdate + validityTimeMillis > System.currentTimeMillis()) {
            System.out.println("Stock data still valid. Skipping update");
        }
        else {
            Stock.DEFAULT.getData();
        }

        DeltaHedgeResult result = largeDeltaHedgeChecker.check(tradeSet, Stock.DEFAULT.regularMarketPrice);
        if(result.isHedgeNeeded) {
            String message = result.toString();
            notificationBuilder.setContentText(message);
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }
        else {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }
}
