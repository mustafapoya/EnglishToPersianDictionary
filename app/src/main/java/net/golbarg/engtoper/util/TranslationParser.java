package net.golbarg.engtoper.util;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a raw dictionary translation into structured meanings.
 *
 * <p>The data comes in two shapes:
 * <ul>
 *     <li>English → Persian: general meanings separated by the Persian comma "،", followed by
 *     {@code <BR>} lines of subject-specific meanings such as "کامپیوتر : رها کردن".</li>
 *     <li>Persian → English: synonyms separated by " ; ", sometimes followed by
 *     "واژه هاى شامل … ـ (N)" — the number of other phrases that contain the word.</li>
 * </ul>
 * Every entry also ends with leftover markup ({@code <br><br></font>');}).
 */
public final class TranslationParser {

    /** A meaning that only applies in one subject, e.g. Computing → "رها کردن". */
    public static class SubjectMeaning {
        public final String subject;
        public final String meaning;

        SubjectMeaning(String subject, String meaning) {
            this.subject = subject;
            this.meaning = meaning;
        }
    }

    public static class Parsed {
        public final List<String> meanings;
        public final List<SubjectMeaning> subjects;
        /** How many other dictionary phrases contain the word (0 when unknown). */
        public final int relatedCount;

        Parsed(List<String> meanings, List<SubjectMeaning> subjects, int relatedCount) {
            this.meanings = Collections.unmodifiableList(meanings);
            this.subjects = Collections.unmodifiableList(subjects);
            this.relatedCount = relatedCount;
        }

        public boolean isEmpty() {
            return meanings.isEmpty() && subjects.isEmpty();
        }

        /** All meanings on one line, for previews, sharing and flashcards. */
        public String joined(String separator) {
            List<String> all = new ArrayList<>(meanings);
            for (SubjectMeaning subject : subjects) all.add(subject.meaning);
            return join(all, separator);
        }

        /** Plain multi-line text: general meanings, then one "subject: meaning" line each. */
        public String toPlainText(String separator) {
            StringBuilder text = new StringBuilder(join(meanings, separator));
            for (SubjectMeaning subject : subjects) {
                if (text.length() > 0) text.append('\n');
                text.append(subject.subject).append(": ").append(subject.meaning);
            }
            return text.toString();
        }
    }

    // "واژه هاى شامل آبستن ـ (10)" — "words containing آبستن — (10)"; ى/ی spellings both occur
    private static final Pattern RELATED = Pattern.compile("واژه\\s*ها[یى]\\s*شامل.*?\\((\\d+)\\)");
    // A short label before " : " marks a subject line ("علوم نظامى : رها کردن")
    private static final Pattern SUBJECT = Pattern.compile("^([^:،;,()\\d]{2,40}?)\\s*:\\s*(.+)$");
    // Parentheses stored in visual order: ")intradermal(" → "(intradermal)"
    private static final Pattern MIRRORED_PARENS = Pattern.compile("\\)([^()]+)\\(");
    private static final Pattern SEPARATORS = Pattern.compile("\\s*[،؛;]\\s*");

    private TranslationParser() {
    }

    @NonNull
    public static Parsed parse(String raw) {
        List<String> meanings = new ArrayList<>();
        List<SubjectMeaning> subjects = new ArrayList<>();
        int related = 0;
        if (raw == null) return new Parsed(meanings, subjects, related);

        String text = UtilController.removeHTMLTags(raw);
        Matcher relatedMatcher = RELATED.matcher(text);
        if (relatedMatcher.find()) {
            related = parseInt(relatedMatcher.group(1));
            text = relatedMatcher.replaceAll("");
        }
        text = MIRRORED_PARENS.matcher(text).replaceAll("($1)");

        Set<String> seen = new LinkedHashSet<>();
        for (String line : text.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;

            Matcher subject = SUBJECT.matcher(line);
            if (subject.matches()) {
                String meaning = clean(subject.group(2));
                if (!meaning.isEmpty()) subjects.add(new SubjectMeaning(clean(subject.group(1)), meaning));
                continue;
            }
            for (String part : SEPARATORS.split(line)) {
                String meaning = clean(part);
                if (!meaning.isEmpty() && seen.add(meaning)) meanings.add(meaning);
            }
        }
        return new Parsed(meanings, subjects, related);
    }

    private static String clean(String value) {
        return value.replaceAll("\\s+", " ")
                .replaceAll("^[\\s\\-ـ.,]+|[\\s\\-ـ.,]+$", "")
                .trim();
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static String join(List<String> parts, String separator) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (builder.length() > 0) builder.append(separator);
            builder.append(part);
        }
        return builder.toString();
    }
}
