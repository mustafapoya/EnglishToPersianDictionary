package net.golbarg.engtoper.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * "Did you mean…?" — ranks dictionary words by how close they are to a misspelled query.
 * Distance is optimal-string-alignment (Levenshtein plus swapping two neighbouring letters,
 * the most common typo), so "recieve" is one step from "receive".
 */
public final class SpellSuggester {

    /** Longer words tolerate more mistakes. */
    public static int maxDistance(int queryLength) {
        if (queryLength <= 3) return 1;
        return queryLength <= 7 ? 2 : 3;
    }

    private SpellSuggester() {
    }

    /**
     * @param candidates words to rank (typically those of a similar length starting with the same letter)
     * @return up to {@code limit} closest words, nearest first, never the query itself
     */
    public static List<String> rank(String query, Collection<String> candidates, int limit) {
        String q = normalize(query);
        int max = maxDistance(q.length());

        List<Scored> scored = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String candidate : candidates) {
            if (candidate == null) continue;
            String c = normalize(candidate);
            if (c.isEmpty() || c.equals(q) || !seen.add(c)) continue;
            if (Math.abs(c.length() - q.length()) > max) continue;
            int distance = distance(q, c, max);
            if (distance <= max) scored.add(new Scored(candidate.trim(), distance, Math.abs(c.length() - q.length())));
        }
        scored.sort((a, b) -> a.distance != b.distance ? Integer.compare(a.distance, b.distance)
                : a.lengthGap != b.lengthGap ? Integer.compare(a.lengthGap, b.lengthGap)
                : a.word.compareToIgnoreCase(b.word));

        List<String> result = new ArrayList<>();
        for (int i = 0; i < scored.size() && result.size() < limit; i++) result.add(scored.get(i).word);
        return result;
    }

    /** Edit distance with adjacent transpositions; returns {@code max + 1} as soon as it is exceeded. */
    static int distance(String a, String b, int max) {
        int n = a.length();
        int m = b.length();
        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) d[i][0] = i;
        for (int j = 0; j <= m; j++) d[0][j] = j;
        for (int i = 1; i <= n; i++) {
            int rowMin = Integer.MAX_VALUE;
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                int value = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)) {
                    value = Math.min(value, d[i - 2][j - 2] + 1);
                }
                d[i][j] = value;
                rowMin = Math.min(rowMin, value);
            }
            if (rowMin > max) return max + 1;
        }
        return d[n][m];
    }

    private static String normalize(String s) {
        return PersianText.normalize(s.trim().toLowerCase(Locale.ROOT));
    }

    private static class Scored {
        final String word;
        final int distance;
        final int lengthGap;

        Scored(String word, int distance, int lengthGap) {
            this.word = word;
            this.distance = distance;
            this.lengthGap = lengthGap;
        }
    }
}
