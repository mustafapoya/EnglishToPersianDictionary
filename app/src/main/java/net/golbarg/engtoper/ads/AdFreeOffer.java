package net.golbarg.engtoper.ads;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import net.golbarg.engtoper.R;
import net.golbarg.engtoper.ads.AdConfig.Placement;

/**
 * "No ads for 24 hours": the user chooses to watch one rewarded video, and once it has been watched
 * every ad in the app goes away for a day ({@link AdFreePass}). Always opt-in, never shown unasked.
 * Uses the {@code ad_free_*} strings from the app's resources.
 */
public final class AdFreeOffer {

    private static final Placement PLACEMENT = Placement.AD_FREE_REWARDED;
    // APP-SPECIFIC: the offer dialog's icon, and the view messages sit above (the bottom navigation; 0 = none)
    @DrawableRes
    private static final int ICON = R.drawable.ic_sparkle;
    @IdRes
    private static final int MESSAGE_ANCHOR = R.id.nav_container;

    private AdFreeOffer() {
    }

    /** Whether the offer can be made right now (ads are showing and a rewarded unit is set up). */
    public static boolean isAvailable(Activity activity) {
        return AdManager.isAllowed(activity, PLACEMENT);
    }

    /** Explains the offer and, if the user agrees, plays the video. */
    public static void show(Activity activity) {
        if (AdFreePass.isActive(activity)) {
            new MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.ad_free_active_title)
                    .setMessage(activity.getString(R.string.ad_free_active, AdFreePass.formatEnd(activity)))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        if (!isAvailable(activity)) {
            message(activity, R.string.ad_free_unavailable);
            return;
        }
        new MaterialAlertDialogBuilder(activity)
                .setIcon(ICON)
                .setTitle(R.string.ad_free_offer_title)
                .setMessage(R.string.ad_free_offer_message)
                .setPositiveButton(R.string.ad_free_offer_watch, (dialog, which) -> loadAndPlay(activity))
                .setNegativeButton(R.string.ad_free_offer_later, null)
                .show();
    }

    private static void loadAndPlay(Activity activity) {
        boolean[] cancelled = {false};
        AlertDialog loading = new MaterialAlertDialogBuilder(activity)
                .setView(loadingView(activity))
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> cancelled[0] = true)
                .setOnCancelListener(dialog -> cancelled[0] = true)
                .show();

        RewardedAds.load(activity, PLACEMENT, available -> {
            if (cancelled[0] || activity.isFinishing() || activity.isDestroyed()) return;
            loading.dismiss();
            if (available) {
                play(activity);
            } else {
                message(activity, R.string.ad_free_unavailable);
            }
        });
    }

    private static void play(Activity activity) {
        boolean[] earned = {false};
        RewardedAds.show(activity, PLACEMENT,
                () -> {
                    earned[0] = true;
                    AdFreePass.grant(activity);
                },
                () -> {
                    if (earned[0]) {
                        message(activity, activity.getString(R.string.ad_free_granted, AdFreePass.formatEnd(activity)));
                    } else {
                        message(activity, R.string.ad_free_not_earned);
                    }
                });
    }

    private static View loadingView(Activity activity) {
        float density = activity.getResources().getDisplayMetrics().density;
        int padding = (int) (24 * density);
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(padding, padding, padding, 0);

        CircularProgressIndicator progress = new CircularProgressIndicator(activity);
        progress.setIndeterminate(true);
        progress.setIndicatorSize((int) (32 * density));
        row.addView(progress);

        TextView text = new TextView(activity);
        text.setText(R.string.ad_free_loading);
        text.setTextSize(16);
        text.setPaddingRelative(padding, 0, 0, 0);
        row.addView(text);
        return row;
    }

    private static void message(Activity activity, @StringRes int text) {
        message(activity, activity.getString(text));
    }

    /** A snackbar above the bottom navigation (when there is one). */
    private static void message(Activity activity, String text) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content), text, Snackbar.LENGTH_LONG);
        View anchor = MESSAGE_ANCHOR != 0 ? activity.findViewById(MESSAGE_ANCHOR) : null;
        if (anchor != null && anchor.getVisibility() == View.VISIBLE) snackbar.setAnchorView(anchor);
        snackbar.show();
    }
}
