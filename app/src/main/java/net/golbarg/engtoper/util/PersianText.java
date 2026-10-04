package net.golbarg.engtoper.util;

/**
 * The dictionary data writes Persian "yeh" as Arabic alef maksura (U+0649) in about 40% of
 * headwords, while Persian keyboards type U+06CC; Arabic yeh (U+064A) and kaf (U+0643) also turn
 * up in typed text. Searches compare both sides in this normalised form so they match either way.
 */
public final class PersianText {

    private static final char ARABIC_YEH = 0x064A;
    private static final char ALEF_MAKSURA = 0x0649;
    private static final char PERSIAN_YEH = 0x06CC;
    private static final char ARABIC_KAF = 0x0643;
    private static final char PERSIAN_KAF = 0x06A9;

    /** SQL expression giving {@code column} in normalised form (same mapping as {@link #normalize}). */
    public static String sqlNormalized(String column) {
        return "REPLACE(REPLACE(REPLACE(" + column
                + ", char(" + (int) ALEF_MAKSURA + "), char(" + (int) PERSIAN_YEH + "))"
                + ", char(" + (int) ARABIC_YEH + "), char(" + (int) PERSIAN_YEH + "))"
                + ", char(" + (int) ARABIC_KAF + "), char(" + (int) PERSIAN_KAF + "))";
    }

    private PersianText() {
    }

    public static String normalize(String text) {
        if (text == null) return "";
        return text.replace(ALEF_MAKSURA, PERSIAN_YEH)
                .replace(ARABIC_YEH, PERSIAN_YEH)
                .replace(ARABIC_KAF, PERSIAN_KAF);
    }

    /** True if the text contains Arabic-script letters (Persian). */
    public static boolean containsPersian(String text) {
        if (text == null) return false;
        for (int i = 0; i < text.length(); i++) {
            int c = text.charAt(i);
            // Arabic, Arabic Presentation Forms-A and -B: the blocks Persian text uses
            if ((c >= 0x0600 && c <= 0x06FF) || (c >= 0xFB50 && c <= 0xFDFF) || (c >= 0xFE70 && c <= 0xFEFF)) {
                return true;
            }
        }
        return false;
    }
}
