package saivenky.optionpricer;

import android.os.Handler;

/**
 * Created by saivenky on 1/1/17.
 */

public class LoopingTask implements Runnable {
    private final Handler handler;
    private Runnable runnable;
    private long delayTimeMillis;
    private volatile boolean isCanceled;

    public LoopingTask(Handler handler, Runnable runnable, long delayTimeMillis) {
        this.handler = handler;
        this.runnable = runnable;
        this.delayTimeMillis = delayTimeMillis;
        this.isCanceled = false;
    }

    @Override
    public void run() {
        handler.removeCallbacks(this);
        if(isCanceled) return;
        runnable.run();
        handler.postDelayed(this, delayTimeMillis);
    }

    public void start() {
        isCanceled = false;
        run();
    }

    public void cancel() {
        isCanceled = true;
    }
}
