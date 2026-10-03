package net.golbarg.engtoper.models;

import androidx.annotation.Nullable;

import net.golbarg.engtoper.util.TranslationParser;

import java.util.List;

import java.util.Objects;

public class Flashcard {
    public static final String LANG_EN = "en";
    public static final String LANG_FA = "fa";

    private static final int SHORT_MEANING_LENGTH = 90;
    private static final int SHORT_MEANING_COUNT = 3;

    private final int wordId;
    private final String lang;
    private final String word;
    private final String translation;
    private final TranslationParser.Parsed parsed;
    private int favorite;
    @Nullable
    private Integer box;

    public Flashcard(int wordId, String lang, String word, String translation, int favorite) {
        this.wordId = wordId;
        this.lang = lang;
        this.word = word;
        this.translation = translation;
        this.favorite = favorite;
        this.parsed = TranslationParser.parse(translation);
    }

    public static Flashcard from(PhraseEnglish phrase) {
        return new Flashcard(phrase.getId(), LANG_EN, phrase.getFromLanguage(), phrase.getToLanguage(), phrase.getFavorite());
    }

    public static Flashcard from(PhrasePersian phrase) {
        return new Flashcard(phrase.getId(), LANG_FA, phrase.getFromLanguage(), phrase.getToLanguage(), phrase.getFavorite());
    }

    public PhraseEnglish toPhraseEnglish() {
        return new PhraseEnglish(wordId, word, translation, favorite, "", 0);
    }

    public PhrasePersian toPhrasePersian() {
        return new PhrasePersian(wordId, word, translation, favorite, "", 0);
    }

    public int getFavorite() {
        return favorite;
    }

    public void setFavorite(int favorite) {
        this.favorite = favorite;
    }

    public int getWordId() {
        return wordId;
    }

    public String getLang() {
        return lang;
    }

    public boolean isEnglish() {
        return LANG_EN.equals(lang);
    }

    public String getWord() {
        return word;
    }

    /** Raw translation (may contain HTML) — used by the word detail sheet. */
    public String getTranslation() {
        return translation;
    }

    /** Structured meanings shown on the back of the card. */
    public TranslationParser.Parsed getParsed() {
        return parsed;
    }

    public boolean hasMeaning() {
        return !parsed.isEmpty();
    }

    /** The first few meanings, shortened — shown on the front in "meaning first" mode. */
    public String getShortMeaning() {
        String separator = isEnglish() ? "، " : ", ";
        List<String> first = parsed.meanings.subList(0, Math.min(SHORT_MEANING_COUNT, parsed.meanings.size()));
        String text = first.isEmpty() ? parsed.joined(separator) : String.join(separator, first);
        if (text.length() <= SHORT_MEANING_LENGTH) return text;
        return text.substring(0, SHORT_MEANING_LENGTH).trim() + "…";
    }

    /** Leitner box, or null when the word has never been studied. */
    @Nullable
    public Integer getBox() {
        return box;
    }

    public void setBox(@Nullable Integer box) {
        this.box = box;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Flashcard)) return false;
        Flashcard other = (Flashcard) o;
        return wordId == other.wordId && lang.equals(other.lang);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wordId, lang);
    }
}
