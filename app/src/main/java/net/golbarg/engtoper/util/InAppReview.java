package net.golbarg.engtoper.util;

import android.app.Activity;
import android.util.Log;

import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

/**
 * Google Play's in-app rating sheet, asked for at a good moment: right after a flashcard session
 * the user did well in, once they clearly use the app, and rarely. Following Play's guidelines
 * there is no "Do you like the app?" question first, and it is never tied to a button (Play may
 * decide not to show it, so "Rate the app" in Settings opens the store page instead).
 *
 * <p>The sheet only appears in builds installed from Google Play (e.g. an internal testing track),
 * not in a debug build from Android Studio; there the request completes without showing anything.
 */
public final class InAppReview {

    private static final String TAG = "InAppReview";

    private static final int MIN_SESSIONS = 3;
    private static final int MIN_ACTIVE_DAYS = 3;
    private static final long MIN_INSTALL_AGE_MS = 3 * 24 * 60 * 60 * 1000L;
    /** Our own cap; Play also limits how often the sheet can appear. */
    private static final long MIN_GAP_MS = 90 * 24 * 60 * 60 * 1000L;
    private static final int MIN_CARDS = 5;
    /** "Did well": at least this share of cards known on the first try. */
    private static final float MIN_SCORE = 0.7f;

    private InAppReview() {
    }

    /**
     * Call once when a flashcard session ends. Counts the session and, if this is a good moment,
     * shows Play's rating sheet over {@code activity}.
     */
    public static void onSessionFinished(Activity activity, int knownFirstTry, int total) {
        int sessions = AppPreferences.incrementSessionsCompleted(activity);
        if (!isGoodMoment(activity, sessions, knownFirstTry, total)) return;
        // Counted as asked even if Play decides not to show it, so we never ask again soon
        AppPreferences.setReviewAskedAt(activity, System.currentTimeMillis());

        ReviewManager manager = ReviewManagerFactory.create(activity);
        manager.requestReviewFlow().addOnCompleteListener(request -> {
            if (!request.isSuccessful()) {
                Log.d(TAG, "Review flow not available", request.getException());
                return;
            }
            if (activity.isFinishing() || activity.isDestroyed()) return;
            ReviewInfo info = request.getResult();
            // Play doesn't say whether the sheet was shown or what was rated; the app just carries on
            manager.launchReviewFlow(activity, info);
        });
    }

    private static boolean isGoodMoment(Activity activity, int sessions, int knownFirstTry, int total) {
        long now = System.currentTimeMillis();
        return total >= MIN_CARDS
                && knownFirstTry >= total * MIN_SCORE
                && sessions >= MIN_SESSIONS
                && AppPreferences.getActiveDays(activity) >= MIN_ACTIVE_DAYS
                && now - AppPreferences.getFirstOpenAt(activity) >= MIN_INSTALL_AGE_MS
                && now - AppPreferences.getReviewAskedAt(activity) >= MIN_GAP_MS;
    }
}
