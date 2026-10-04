package net.golbarg.engtoper.reminder;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import net.golbarg.engtoper.MainActivity;
import net.golbarg.engtoper.R;
import net.golbarg.engtoper.SplashScreenActivity;
import net.golbarg.engtoper.db.DictionaryRepository;
import net.golbarg.engtoper.models.PhraseEnglish;
import net.golbarg.engtoper.util.AppPreferences;
import net.golbarg.engtoper.util.TranslationParser;

import java.util.List;

/** Posts the daily word notification, and re-schedules it after a reboot, update or clock change. */
public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "daily_word";
    private static final int NOTIFICATION_ID = 2001;
    private static final int MEANINGS_IN_NOTIFICATION = 3;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ReminderScheduler.ACTION_SHOW.equals(intent.getAction())) {
            // Boot, app update, time or timezone change: alarms are cleared or shifted, so set again
            ReminderScheduler.sync(context);
            return;
        }
        // The alarm is one-off: set tomorrow's now (the trigger time has passed, so it moves a day on)
        ReminderScheduler.sync(context);
        if (!AppPreferences.isReminderEnabled(context) || !canNotify(context)) return;

        PendingResult pending = goAsync();
        Context appContext = context.getApplicationContext();
        DictionaryRepository.getInstance(appContext).getWordOfTheDay(word -> {
            try {
                show(appContext, word);
            } finally {
                pending.finish();
            }
        });
    }

    private static boolean canNotify(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    private static void show(Context context, @Nullable PhraseEnglish word) {
        if (!canNotify(context)) return;
        createChannel(context);

        String title = word != null
                ? context.getString(R.string.reminder_title, word.getFromLanguage())
                : context.getString(R.string.word_of_the_day);
        String text = context.getString(R.string.reminder_text_fallback);
        if (word != null) {
            List<String> meanings = TranslationParser.parse(word.getToLanguage()).meanings;
            if (!meanings.isEmpty()) {
                text = String.join("، ", meanings.subList(0, Math.min(MEANINGS_IN_NOTIFICATION, meanings.size())));
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(openApp(context, null))
                .addAction(R.drawable.ic_flashcard_outline, context.getString(R.string.reminder_action_review),
                        openApp(context, MainActivity.DESTINATION_FLASHCARDS))
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            // Permission revoked between the check and the post
        }
    }

    /** Opens the app on the home screen, or on a deep-link destination. */
    private static PendingIntent openApp(Context context, @Nullable String destination) {
        Intent intent = new Intent(context, SplashScreenActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (destination != null) intent.putExtra(MainActivity.EXTRA_DESTINATION, destination);
        return PendingIntent.getActivity(context, destination == null ? 0 : 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void createChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.reminder_channel_description));
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);
    }
}
