package net.golbarg.engtoper.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import net.golbarg.engtoper.models.SearchFilter;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AppPreferences {
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_INTRO_COMPLETED = "intro_completed";
    private static final String KEY_DICTIONARY_LANG = "dictionary_lang";
    private static final String KEY_FLASHCARD_REVERSE = "flashcard_reverse";
    private static final String KEY_STREAK_COUNT = "streak_count";
    private static final String KEY_STREAK_LAST_DAY = "streak_last_day";
    private static final String KEY_WORD_OF_DAY_DATE = "word_of_day_date";
    private static final String KEY_WORD_OF_DAY_ID = "word_of_day_id";
    private static final String KEY_STUDY_DAYS = "study_days";
    private static final int STUDY_DAYS_KEPT = 14;
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_SEARCH_FILTER = "search_filter";
    private static final String KEY_SPEECH_RATE = "speech_rate";
    private static final String KEY_BRITISH_ACCENT = "british_accent";
    private static final String KEY_DAILY_GOAL = "daily_goal";

    public static final float SPEECH_RATE_SLOW = 0.7f;
    public static final float SPEECH_RATE_NORMAL = 0.9f;
    public static final float SPEECH_RATE_FAST = 1.15f;
    public static final int DEFAULT_DAILY_GOAL = 10;

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isIntroCompleted(Context context) {
        return prefs(context).getBoolean(KEY_INTRO_COMPLETED, false);
    }

    public static void setIntroCompleted(Context context, boolean completed) {
        prefs(context).edit().putBoolean(KEY_INTRO_COMPLETED, completed).apply();
    }

    /** Last search direction used in the dictionary: "en" (English → Persian) or "fa". */
    public static String getDictionaryLang(Context context) {
        return prefs(context).getString(KEY_DICTIONARY_LANG, "en");
    }

    public static void setDictionaryLang(Context context, String lang) {
        prefs(context).edit().putString(KEY_DICTIONARY_LANG, lang).apply();
    }

    /** When true, flashcards show the meaning first and the word on the back. */
    public static boolean isFlashcardReverse(Context context) {
        return prefs(context).getBoolean(KEY_FLASHCARD_REVERSE, false);
    }

    public static void setFlashcardReverse(Context context, boolean reverse) {
        prefs(context).edit().putBoolean(KEY_FLASHCARD_REVERSE, reverse).apply();
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    /** One of {@code AppCompatDelegate.MODE_NIGHT_*}; follows the system by default. */
    public static int getThemeMode(Context context) {
        return prefs(context).getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static void setThemeMode(Context context, int mode) {
        prefs(context).edit().putInt(KEY_THEME_MODE, mode).apply();
    }

    /** Name of a {@link net.golbarg.engtoper.models.SearchFilter}. */
    public static SearchFilter getSearchFilter(Context context) {
        try {
            return SearchFilter.valueOf(prefs(context).getString(KEY_SEARCH_FILTER, SearchFilter.STARTS_WITH.name()));
        } catch (IllegalArgumentException e) {
            return SearchFilter.STARTS_WITH;
        }
    }

    public static void setSearchFilter(Context context, SearchFilter filter) {
        prefs(context).edit().putString(KEY_SEARCH_FILTER, filter.name()).apply();
    }

    /** Text-to-speech rate: 1.0 is the engine's normal speed. */
    public static float getSpeechRate(Context context) {
        return prefs(context).getFloat(KEY_SPEECH_RATE, SPEECH_RATE_NORMAL);
    }

    public static void setSpeechRate(Context context, float rate) {
        prefs(context).edit().putFloat(KEY_SPEECH_RATE, rate).apply();
    }

    /** True for a British pronunciation, false for American. */
    public static boolean isBritishAccent(Context context) {
        return prefs(context).getBoolean(KEY_BRITISH_ACCENT, false);
    }

    public static void setBritishAccent(Context context, boolean british) {
        prefs(context).edit().putBoolean(KEY_BRITISH_ACCENT, british).apply();
    }

    /** Flashcards to review per day on the home screen's goal ring. */
    public static int getDailyGoal(Context context) {
        return prefs(context).getInt(KEY_DAILY_GOAL, DEFAULT_DAILY_GOAL);
    }

    public static void setDailyGoal(Context context, int goal) {
        prefs(context).edit().putInt(KEY_DAILY_GOAL, goal).apply();
    }

    /** Forgets the streak and study days (used when flashcard progress is reset). */
    public static void resetStudyHistory(Context context) {
        prefs(context).edit()
                .remove(KEY_STREAK_COUNT)
                .remove(KEY_STREAK_LAST_DAY)
                .remove(KEY_STUDY_DAYS)
                .apply();
    }

    // ── Study streak ──────────────────────────────────────────────────────────

    /** Marks today as a study day, extending the streak if yesterday was one too. */
    public static void recordStudyDay(Context context) {
        SharedPreferences prefs = prefs(context);
        LocalDate today = LocalDate.now();
        LocalDate last = parseDay(prefs.getString(KEY_STREAK_LAST_DAY, null));
        int count = prefs.getInt(KEY_STREAK_COUNT, 0);

        if (today.equals(last)) return;
        int next = today.minusDays(1).equals(last) ? count + 1 : 1;

        // Remember recent study days for the weekly activity strip (only the last two weeks)
        Set<String> days = new HashSet<>();
        LocalDate cutoff = today.minusDays(STUDY_DAYS_KEPT);
        for (String day : prefs.getStringSet(KEY_STUDY_DAYS, Collections.emptySet())) {
            LocalDate date = parseDay(day);
            if (date != null && date.isAfter(cutoff)) days.add(day);
        }
        days.add(today.toString());

        prefs.edit()
                .putInt(KEY_STREAK_COUNT, next)
                .putString(KEY_STREAK_LAST_DAY, today.toString())
                .putStringSet(KEY_STUDY_DAYS, days)
                .apply();
    }

    /** Days (within the last two weeks) on which the user studied. */
    public static Set<LocalDate> getRecentStudyDays(Context context) {
        SharedPreferences prefs = prefs(context);
        Set<LocalDate> days = new HashSet<>();
        for (String day : prefs.getStringSet(KEY_STUDY_DAYS, Collections.emptySet())) {
            LocalDate date = parseDay(day);
            if (date != null) days.add(date);
        }
        // A streak of N days ending on its last day means each of those days was a study day
        // (this also covers days recorded before the study-day set existed)
        LocalDate last = parseDay(prefs.getString(KEY_STREAK_LAST_DAY, null));
        int streak = Math.min(prefs.getInt(KEY_STREAK_COUNT, 0), STUDY_DAYS_KEPT);
        for (int i = 0; last != null && i < streak; i++) {
            days.add(last.minusDays(i));
        }
        return days;
    }

    /** Current streak; it stays alive until the end of the day after the last study day. */
    public static int getStreak(Context context) {
        SharedPreferences prefs = prefs(context);
        LocalDate today = LocalDate.now();
        LocalDate last = parseDay(prefs.getString(KEY_STREAK_LAST_DAY, null));
        if (today.equals(last) || today.minusDays(1).equals(last)) {
            return prefs.getInt(KEY_STREAK_COUNT, 0);
        }
        return 0;
    }

    // ── Word of the day ───────────────────────────────────────────────────────

    /** Id of today's word of the day, or -1 if none has been picked today. */
    public static int getWordOfDayId(Context context) {
        SharedPreferences prefs = prefs(context);
        if (!LocalDate.now().toString().equals(prefs.getString(KEY_WORD_OF_DAY_DATE, null))) return -1;
        return prefs.getInt(KEY_WORD_OF_DAY_ID, -1);
    }

    public static void setWordOfDayId(Context context, int id) {
        prefs(context).edit()
                .putString(KEY_WORD_OF_DAY_DATE, LocalDate.now().toString())
                .putInt(KEY_WORD_OF_DAY_ID, id)
                .apply();
    }

    private static LocalDate parseDay(String value) {
        if (value == null) return null;
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
    }
}
