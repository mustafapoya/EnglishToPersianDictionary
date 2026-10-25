package net.golbarg.engtoper.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

/**
 * Privacy consent through Google's User Messaging Platform. Where the law requires it (e.g. the EEA
 * and UK) users are asked once before ads load, and Settings offers "Ad privacy choices" to change
 * their answer. Elsewhere this is invisible and ads start right away.
 */
public final class AdConsent {

    private static boolean requested = false;

    private AdConsent() {
    }

    /** Call from MainActivity.onCreate. Checks for a consent update once per launch, asks if needed, then lets ads start. */
    public static void gather(Activity activity) {
        // A choice made on an earlier launch already counts: start ads straight away if allowed
        AdManager.refresh(activity);
        if (requested || !AdManager.isEnabled()) return;
        requested = true;

        ConsentInformation info = UserMessagingPlatform.getConsentInformation(activity);
        info.requestConsentInfoUpdate(activity, new ConsentRequestParameters.Builder().build(),
                () -> {
                    if (activity.isFinishing() || activity.isDestroyed()) {
                        AdManager.refresh(activity);
                        return;
                    }
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, formError -> {
                        if (formError != null) Log.w(AdManager.TAG, "Consent form: " + formError.getMessage());
                        AdManager.refresh(activity);
                    });
                },
                error -> {
                    Log.w(AdManager.TAG, "Consent info update failed: " + error.getMessage());
                    AdManager.refresh(activity);
                });
    }

    static boolean canRequestAds(Context context) {
        return UserMessagingPlatform.getConsentInformation(context).canRequestAds();
    }

    /** True where users must be able to change their ad privacy choice (shown as a Settings row). */
    public static boolean isPrivacyOptionsRequired(Context context) {
        return AdManager.isEnabled() && UserMessagingPlatform.getConsentInformation(context).getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    public static void showPrivacyOptions(Activity activity, @Nullable Runnable onDone) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, formError -> {
            if (formError != null) Log.w(AdManager.TAG, "Privacy options form: " + formError.getMessage());
            AdManager.refresh(activity);
            if (onDone != null) onDone.run();
        });
    }
}
