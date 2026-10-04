package net.golbarg.engtoper.ads;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import net.golbarg.engtoper.R;

/**
 * "No ads for 24 hours": the user chooses to watch one rewarded video, and once it has been watched
 * every ad in the app goes away for a day ({@link AdFreePass}). Always opt-in, never shown unasked.
 */
public final class AdFreeOffer {

    private AdFreeOffer() {
    }

    /** Whether the offer can be made right now (ads are showing and a rewarded unit is set up). */
    public static boolean isAvailable(Activity activity) {
        return AdUtil.isEnabled(activity, AdUtil.Placement.AD_FREE_REWARDED);
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
                .setIcon(R.drawable.ic_sparkle)
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

        RewardedAd.load(activity, AdUtil.unitId(AdUtil.Placement.AD_FREE_REWARDED), new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        if (cancelled[0] || activity.isFinishing() || activity.isDestroyed()) return;
                        loading.dismiss();
                        play(activity, ad);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        if (cancelled[0] || activity.isFinishing() || activity.isDestroyed()) return;
                        loading.dismiss();
                        message(activity, R.string.ad_free_unavailable);
                    }
                });
    }

    private static void play(Activity activity, RewardedAd ad) {
        boolean[] earned = {false};
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                if (earned[0]) {
                    message(activity, activity.getString(R.string.ad_free_granted, AdFreePass.formatEnd(activity)));
                } else {
                    message(activity, R.string.ad_free_not_earned);
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                message(activity, R.string.ad_free_unavailable);
            }
        });
        ad.show(activity, reward -> {
            earned[0] = true;
            AdFreePass.grant(activity);
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
        View navBar = activity.findViewById(R.id.nav_container);
        if (navBar != null && navBar.getVisibility() == View.VISIBLE) snackbar.setAnchorView(navBar);
        snackbar.show();
    }
}
