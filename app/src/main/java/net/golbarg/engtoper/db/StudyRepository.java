package net.golbarg.engtoper.db;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import net.golbarg.engtoper.models.CardProgress;
import net.golbarg.engtoper.models.Flashcard;
import net.golbarg.engtoper.models.PhraseEnglish;
import net.golbarg.engtoper.models.PhrasePersian;
import net.golbarg.engtoper.util.AppPreferences;
import net.golbarg.engtoper.util.Leitner;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Flashcard decks, Leitner progress and study statistics. */
public class StudyRepository {

    public enum Deck { DUE, SAVED, RANDOM }

    public static class Stats {
        public final int streak;
        public final int saved;
        public final int learned;
        public final int due;
        public final int reviewedToday;

        Stats(int streak, int saved, int learned, int due, int reviewedToday) {
            this.streak = streak;
            this.saved = saved;
            this.learned = learned;
            this.due = due;
            this.reviewedToday = reviewedToday;
        }
    }

    private static StudyRepository instance;

    private final Context appContext;
    private final DictionaryRepository dictionary;
    private final TableCardProgress tableProgress;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private StudyRepository(Context context) {
        appContext = context.getApplicationContext();
        dictionary = DictionaryRepository.getInstance(appContext);
        tableProgress = new TableCardProgress(DatabaseHandler.getInstance(appContext));
    }

    public static synchronized StudyRepository getInstance(Context context) {
        if (instance == null) {
            instance = new StudyRepository(context);
        }
        return instance;
    }

    public void loadDeck(Deck deck, int size, DictionaryRepository.Callback<List<Flashcard>> callback) {
        executor.execute(() -> {
            List<Flashcard> cards = cardsFor(deck, size);
            for (Flashcard card : cards) {
                CardProgress progress = tableProgress.get(card.getWordId(), card.getLang());
                card.setBox(progress != null ? progress.getBox() : null);
            }
            mainHandler.post(() -> callback.onResult(cards));
        });
    }

    /**
     * Records an answer and returns the progress the card had before, so it can be undone.
     * The callback receives null for a card that had never been studied.
     */
    public void review(Flashcard card, boolean known, DictionaryRepository.Callback<CardProgress> callback) {
        executor.execute(() -> {
            CardProgress previous = tableProgress.get(card.getWordId(), card.getLang());
            int box = previous != null ? previous.getBox() : 0;
            tableProgress.upsert(Leitner.review(card.getWordId(), card.getLang(), box, known, System.currentTimeMillis()));
            AppPreferences.recordStudyDay(appContext);
            mainHandler.post(() -> callback.onResult(previous));
        });
    }

    public void restore(Flashcard card, @Nullable CardProgress previous, Runnable onDone) {
        executor.execute(() -> {
            if (previous == null) {
                tableProgress.delete(card.getWordId(), card.getLang());
            } else {
                tableProgress.upsert(previous);
            }
            mainHandler.post(onDone);
        });
    }

    /** Forgets every card's Leitner box plus the streak and study days. */
    public void resetProgress(Runnable onDone) {
        executor.execute(() -> {
            tableProgress.deleteAll();
            AppPreferences.resetStudyHistory(appContext);
            mainHandler.post(onDone);
        });
    }

    public void getStats(DictionaryRepository.Callback<Stats> callback) {
        executor.execute(() -> {
            int saved = dictionary.englishTable().countBookmarks() + dictionary.persianTable().countBookmarks();
            Stats stats = new Stats(
                    AppPreferences.getStreak(appContext),
                    saved,
                    tableProgress.countLearned(Leitner.LEARNED_BOX),
                    tableProgress.countDue(endOfToday()),
                    tableProgress.countReviewedSince(startOfToday())
            );
            mainHandler.post(() -> callback.onResult(stats));
        });
    }

    private List<Flashcard> cardsFor(Deck deck, int size) {
        List<Flashcard> cards = new ArrayList<>();
        switch (deck) {
            case DUE:
                for (CardProgress progress : tableProgress.getDue(endOfToday(), size)) {
                    Flashcard card = load(progress.getWordId(), progress.getLang());
                    if (card != null) cards.add(card);
                }
                break;
            case SAVED:
                for (PhraseEnglish phrase : dictionary.englishTable().getBookmarks()) cards.add(Flashcard.from(phrase));
                for (PhrasePersian phrase : dictionary.persianTable().getBookmarks()) cards.add(Flashcard.from(phrase));
                Collections.shuffle(cards);
                if (cards.size() > size) cards = new ArrayList<>(cards.subList(0, size));
                break;
            case RANDOM:
                for (PhraseEnglish phrase : dictionary.englishTable().getRandomWords(size, -1)) cards.add(Flashcard.from(phrase));
                break;
        }
        return cards;
    }

    @Nullable
    private Flashcard load(int wordId, String lang) {
        if (Flashcard.LANG_FA.equals(lang)) {
            PhrasePersian phrase = dictionary.persianTable().get(wordId);
            return phrase != null ? Flashcard.from(phrase) : null;
        }
        PhraseEnglish phrase = dictionary.englishTable().get(wordId);
        return phrase != null ? Flashcard.from(phrase) : null;
    }

    private static long startOfToday() {
        return LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static long endOfToday() {
        return LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1;
    }
}
