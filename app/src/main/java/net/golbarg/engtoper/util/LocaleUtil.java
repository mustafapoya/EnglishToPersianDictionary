package net.golbarg.engtoper.util;

import android.content.Context;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DecimalStyle;
import java.util.Locale;

/**
 * Numbers and dates in the app's current language (Persian digits when the UI is Persian).
 * Uses the resources' locale, which follows the in-app language choice on every Android version.
 */
public final class LocaleUtil {

    private LocaleUtil() {
    }

    public static Locale current(Context context) {
        return context.getResources().getConfiguration().getLocales().get(0);
    }

    public static String number(Context context, long value) {
        return NumberFormat.getIntegerInstance(current(context)).format(value);
    }

    /** Formats a date with a pattern from resources, using the locale's own digits. */
    public static String date(Context context, LocalDate date, String pattern) {
        Locale locale = current(context);
        return date.format(DateTimeFormatter.ofPattern(pattern, locale).withDecimalStyle(DecimalStyle.of(locale)));
    }
}
