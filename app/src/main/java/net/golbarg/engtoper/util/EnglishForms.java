package net.golbarg.engtoper.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Turns text selected in another app into dictionary headwords to try, in order:
 * the word as written, then simple base forms ("running" → "run", "studies" → "study").
 */
public final class EnglishForms {

    private EnglishForms() {
    }

    /** Trims surrounding punctuation and spaces: "“Hello,”" → "Hello". */
    public static String clean(String text) {
        if (text == null) return "";
        String firstLine = text.trim().split("\\R", 2)[0];
        return firstLine.replaceAll("^[\\p{P}\\p{S}\\s]+|[\\p{P}\\p{S}\\s]+$", "").trim();
    }

    public static List<String> candidates(String word) {
        Set<String> forms = new LinkedHashSet<>();
        String w = word.trim();
        if (w.isEmpty()) return new ArrayList<>(forms);
        forms.add(w);
        String lower = w.toLowerCase(Locale.ROOT);
        forms.add(lower);

        // Only single words get base forms; phrases are looked up as written
        if (!lower.matches("[a-z']+") || lower.length() < 4) return new ArrayList<>(forms);

        // The first form found in the dictionary wins, so the order matters:
        // "notes" must reach "note" before "not", "hoping" "hope" before "hop"
        if (lower.endsWith("ies")) forms.add(lower.substring(0, lower.length() - 3) + "y");      // studies → study
        if (lower.endsWith("s") && !lower.endsWith("ss")) forms.add(lower.substring(0, lower.length() - 1)); // notes → note
        if (lower.endsWith("es")) forms.add(lower.substring(0, lower.length() - 2));             // boxes → box
        if (lower.endsWith("ied")) forms.add(lower.substring(0, lower.length() - 3) + "y");      // studied → study
        if (lower.endsWith("ed")) addStem(forms, lower.substring(0, lower.length() - 2));        // walked, hoped, stopped
        if (lower.endsWith("ing")) addStem(forms, lower.substring(0, lower.length() - 3));       // walking, making, running
        if (lower.endsWith("ier")) forms.add(lower.substring(0, lower.length() - 3) + "y");      // happier → happy
        if (lower.endsWith("iest")) forms.add(lower.substring(0, lower.length() - 4) + "y");     // happiest → happy
        if (lower.endsWith("er")) addStem(forms, lower.substring(0, lower.length() - 2));        // faster, nicer, bigger
        if (lower.endsWith("est")) addStem(forms, lower.substring(0, lower.length() - 3));       // fastest, nicest
        if (lower.endsWith("ily")) forms.add(lower.substring(0, lower.length() - 3) + "y");      // happily → happy
        if (lower.endsWith("ly")) forms.add(lower.substring(0, lower.length() - 2));             // quickly → quick
        return new ArrayList<>(forms);
    }

    /**
     * Adds the base forms of a stem left by removing a suffix. A stem like "hop" or "car" (one
     * vowel, then one consonant) usually lost a silent e, so "hope" is tried before "hop";
     * a doubled final letter ("stopp") is tried undoubled after the stem itself ("add" stays "add").
     */
    private static void addStem(Set<String> forms, String stem) {
        if (stem.length() < 2) return;
        if (lostSilentE(stem)) {
            forms.add(stem + "e");
            forms.add(stem);
        } else {
            forms.add(stem);
            forms.add(stem + "e");
        }
        int n = stem.length();
        if (n >= 3 && stem.charAt(n - 1) == stem.charAt(n - 2)) forms.add(stem.substring(0, n - 1));
    }

    private static boolean lostSilentE(String stem) {
        int n = stem.length();
        char last = stem.charAt(n - 1);
        return !isVowel(last) && "wxy".indexOf(last) < 0
                && isVowel(stem.charAt(n - 2))
                && (n == 2 || !isVowel(stem.charAt(n - 3)));
    }

    private static boolean isVowel(char c) {
        return "aeiou".indexOf(c) >= 0;
    }
}
