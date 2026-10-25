package net.golbarg.engtoper.ads;

/** The ad formats the apps use, each with Google's demo unit used while {@link AdConfig#USE_TEST_ADS} is true. */
public enum AdFormat {
    // Google's demo ad units: https://developers.google.com/admob/android/test-ads
    BANNER("ca-app-pub-3940256099942544/9214589741"), // adaptive banner
    INTERSTITIAL("ca-app-pub-3940256099942544/1033173712"),
    REWARDED("ca-app-pub-3940256099942544/5224354917"),
    NATIVE("ca-app-pub-3940256099942544/2247696110");

    final String testUnitId;

    AdFormat(String testUnitId) {
        this.testUnitId = testUnitId;
    }
}
