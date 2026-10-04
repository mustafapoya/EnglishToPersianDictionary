package net.golbarg.engtoper.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import net.golbarg.engtoper.util.AppPreferences;

import java.util.Calendar;

/**
 * Schedules the daily word reminder: a one-off alarm with a short delivery window, set again for
 * the next day each time it fires. Needs no exact-alarm permission. (An inexact repeating alarm
 * may be delivered up to 75% of its interval late — for a daily alarm, the next morning.)
 */
public final class ReminderScheduler {

    static final String ACTION_SHOW = "net.golbarg.engtoper.action.SHOW_DAILY_REMINDER";
    private static final int REQUEST_CODE = 1001;
    private static final long WINDOW_MS = 15 * 60 * 1000L;

    private ReminderScheduler() {
    }

    /** Schedules or cancels the alarm to match the user's setting. */
    public static void sync(Context context) {
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        if (alarms == null) return;
        PendingIntent pending = pendingIntent(context);
        alarms.cancel(pending);
        if (!AppPreferences.isReminderEnabled(context)) return;
        alarms.setWindow(AlarmManager.RTC_WAKEUP, nextTrigger(AppPreferences.getReminderMinutes(context)),
                WINDOW_MS, pending);
    }

    /** The next time today (or tomorrow, if already past) at {@code minutesOfDay}. */
    static long nextTrigger(int minutesOfDay) {
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, minutesOfDay / 60);
        next.set(Calendar.MINUTE, minutesOfDay % 60);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (next.getTimeInMillis() <= System.currentTimeMillis()) next.add(Calendar.DAY_OF_YEAR, 1);
        return next.getTimeInMillis();
    }

    private static PendingIntent pendingIntent(Context context) {
        Intent intent = new Intent(context, ReminderReceiver.class).setAction(ACTION_SHOW);
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
