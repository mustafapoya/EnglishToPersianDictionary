package net.golbarg.engtoper.ads;

import com.google.android.gms.ads.RequestConfiguration;

/**
 * The only file to edit to control ads. Change a value and rebuild.
 * See docs/ads/README.md (MobileApps workspace) for the quick card.
 */
public final class AdConfig {

    // ── Master switches ─────────────────────────────────────────────────────
    // false = no ads anywhere: no SDK start, no consent form, no ad requests
    public static final boolean ADS_ENABLED = false;
    // true = Google's demo ad units (while developing), false = the real units below
    public static final boolean USE_TEST_ADS = true;

    // ── Rules ───────────────────────────────────────────────────────────────
    // A learning app used by all ages: keep ad content family-friendly
    static final String MAX_CONTENT_RATING = RequestConfiguration.MAX_AD_CONTENT_RATING_PG;
    // Shortest time between two full-screen ads (interstitial or rewarded), any placement
    static final long FULLSCREEN_MIN_GAP_MS = 10 * 60 * 1000L;

    // ── Placements: every spot in the app that can show an ad ───────────────
    // ON / OFF turns one spot on or off. Real unit IDs come from the AdMob console.
    // An empty real ID keeps that spot off while USE_TEST_ADS is false.
    private static final boolean ON = true;
    private static final boolean OFF = false;

    public enum Placement {
        DICTIONARY_BANNER       (AdFormat.BANNER,       ON, "ca-app-pub-8976959600358837/6320606502"),
        SAVED_BANNER            (AdFormat.BANNER,       ON, "ca-app-pub-8976959600358837/8005096953"),
        HOME_NATIVE             (AdFormat.NATIVE,       ON, "ca-app-pub-8976959600358837/2081978595"),
        FLASHCARD_NATIVE        (AdFormat.NATIVE,       ON, "ca-app-pub-8976959600358837/1259851510"),
        SESSION_END_INTERSTITIAL(AdFormat.INTERSTITIAL, ON, "ca-app-pub-8976959600358837/7405709033"),
        AD_FREE_REWARDED        (AdFormat.REWARDED,     ON, "ca-app-pub-8976959600358837/8946769846");

        final AdFormat format;
        final boolean on;
        final String realUnitId;

        Placement(AdFormat format, boolean on, String realUnitId) {
            this.format = format;
            this.on = on;
            this.realUnitId = realUnitId;
        }
    }

    private AdConfig() {
    }
}
