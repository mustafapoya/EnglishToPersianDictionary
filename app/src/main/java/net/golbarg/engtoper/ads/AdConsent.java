package net.golbarg.engtoper.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

/**
 * Privacy consent through Google's User Messaging Platform. Where the law requires it (e.g. the EEA
 * and UK) users are asked once before ads load, and Settings offers "Ad privacy choices" to change
 * their answer. Elsewhere this is invisible and ads start right away.
 */
public final class AdConsent {

    private static final String TAG = "AdConsent";
    private static boolean requested = false;

    private AdConsent() {
    }

    /** Checks for a consent update once per launch, asks if needed, then lets ads start. */
    public static void gather(Activity activity) {
        // A choice made on an earlier launch already counts: start ads straight away if allowed
        AdUtil.refresh(activity);
        if (requested || !AdUtil.isEnabled()) return;
        requested = true;

        ConsentInformation info = UserMessagingPlatform.getConsentInformation(activity);
        info.requestConsentInfoUpdate(activity, new ConsentRequestParameters.Builder().build(),
                () -> {
                    if (activity.isFinishing() || activity.isDestroyed()) {
                        AdUtil.refresh(activity);
                        return;
                    }
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, formError -> {
                        if (formError != null) Log.w(TAG, "Consent form: " + formError.getMessage());
                        AdUtil.refresh(activity);
                    });
                },
                error -> {
                    Log.w(TAG, "Consent info update failed: " + error.getMessage());
                    AdUtil.refresh(activity);
                });
    }

    static boolean canRequestAds(Context context) {
        return UserMessagingPlatform.getConsentInformation(context).canRequestAds();
    }

    /** True where users must be able to change their ad privacy choice (shown as a Settings row). */
    public static boolean isPrivacyOptionsRequired(Context context) {
        return AdUtil.isEnabled() && UserMessagingPlatform.getConsentInformation(context).getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    public static void showPrivacyOptions(Activity activity, Runnable onDone) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, formError -> {
            if (formError != null) Log.w(TAG, "Privacy options form: " + formError.getMessage());
            AdUtil.refresh(activity);
            onDone.run();
        });
    }
}
