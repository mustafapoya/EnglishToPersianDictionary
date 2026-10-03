package net.golbarg.engtoper.models;

/** Leitner progress of one flashcard. A word is identified by its id and language ("en" / "fa"). */
public class CardProgress {
    private final int wordId;
    private final String lang;
    private final int box;
    private final long dueAt;
    private final long reviewedAt;

    public CardProgress(int wordId, String lang, int box, long dueAt, long reviewedAt) {
        this.wordId = wordId;
        this.lang = lang;
        this.box = box;
        this.dueAt = dueAt;
        this.reviewedAt = reviewedAt;
    }

    public int getWordId() {
        return wordId;
    }

    public String getLang() {
        return lang;
    }

    public int getBox() {
        return box;
    }

    public long getDueAt() {
        return dueAt;
    }

    public long getReviewedAt() {
        return reviewedAt;
    }
}
