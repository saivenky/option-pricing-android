package saivenky.trading;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by saivenky on 1/3/17.
 */

public final class TradingHours {
    private static final int MARKET_OPEN_HOUR = 9;
    private static final int MARKET_OPEN_MINUTE = 30;

    private static final int BUSY_OPEN_END_HOUR = 12;
    private static final int BUSY_OPEN_END_MINUTE = 0;

    private static final int BUSY_CLOSE_START_HOUR = 3;
    private static final int BUSY_CLOSE_START_MINUTE = 0;

    private static final int MARKET_CLOSE_HOUR = 4;
    private static final int MARKET_CLOSE_MINUTE = 0;

    private static final TimeZone EASTERN = TimeZone.getTimeZone("America/New_York");

    private static Calendar getTradingCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(EASTERN);
        return calendar;
    }

    private static long getTimeMillis(int hour, int minute) {
        Calendar calendar = getTradingCalendar();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        calendar.set(Calendar.HOUR, hour);
        calendar.set(Calendar.MINUTE, minute);
        return calendar.getTimeInMillis();
    }

    private static long getMarketOpen() {
        return getTimeMillis(MARKET_OPEN_HOUR, MARKET_OPEN_MINUTE);
    }

    private static long getMarketClose() {
        return getTimeMillis(MARKET_CLOSE_HOUR, MARKET_CLOSE_MINUTE);
    }

    private static long getBusyOpenEnd() {
        return getTimeMillis(BUSY_OPEN_END_HOUR, BUSY_OPEN_END_MINUTE);
    }

    private static long getBusyCloseStart() {
        return getTimeMillis(BUSY_CLOSE_START_HOUR, BUSY_CLOSE_START_MINUTE);
    }

    private static boolean isTradingDay() {
        int dayOfWeek = getTradingCalendar().get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SUNDAY || dayOfWeek == Calendar.SATURDAY) {
            return false;
        }

        return true;
    }
}
