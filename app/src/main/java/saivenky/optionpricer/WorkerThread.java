package saivenky.optionpricer;

import android.os.Handler;
import android.os.Looper;

/**
 * Created by saivenky on 1/1/17.
 */

public class WorkerThread extends Thread {
    private Handler workerHandler;

    public WorkerThread() {
    }

    @Override
    public void run() {
        Looper.prepare();
        workerHandler = new Handler();
        Looper.loop();
    }

    public Handler getHandler() {
        while(workerHandler == null);
        return workerHandler;
    }
}
