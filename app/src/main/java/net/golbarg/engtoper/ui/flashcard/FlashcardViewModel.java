package net.golbarg.engtoper.ui.flashcard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import net.golbarg.engtoper.db.StudyRepository;
import net.golbarg.engtoper.db.StudyRepository.Deck;
import net.golbarg.engtoper.models.CardProgress;
import net.golbarg.engtoper.models.Flashcard;
import net.golbarg.engtoper.util.AppPreferences;
import net.golbarg.engtoper.util.Leitner;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class FlashcardViewModel extends AndroidViewModel {
    private static final int SESSION_SIZE = 20;

    public enum Type { LOADING, EMPTY, SHOWING, FINISHED }

    /** Everything the flashcard screen renders, for one of the four {@link Type}s. */
    public static class State {
        public final Type type;
        public final Deck deck;
        // SHOWING
        @Nullable
        public final Flashcard card;
        public final int position;
        public final int total;
        public final boolean flipped;
        public final boolean canUndo;
        public final boolean isRepeat;
        public final int nextIntervalDays;
        // FINISHED
        public final int knownFirstTry;
        public final int missed;
        // Live tally while studying
        public int knownAnswers;
        public int againAnswers;

        private State(Type type, Deck deck, @Nullable Flashcard card, int position, int total, boolean flipped,
                      boolean canUndo, boolean isRepeat, int nextIntervalDays, int knownFirstTry, int missed) {
            this.type = type;
            this.deck = deck;
            this.card = card;
            this.position = position;
            this.total = total;
            this.flipped = flipped;
            this.canUndo = canUndo;
            this.isRepeat = isRepeat;
            this.nextIntervalDays = nextIntervalDays;
            this.knownFirstTry = knownFirstTry;
            this.missed = missed;
        }

        static State simple(Type type, Deck deck) {
            return new State(type, deck, null, 0, 0, false, false, false, 0, 0, 0);
        }

        State withFlipped(boolean flipped) {
            State copy = new State(type, deck, card, position, total, flipped, canUndo, isRepeat, nextIntervalDays,
                    knownFirstTry, missed);
            copy.knownAnswers = knownAnswers;
            copy.againAnswers = againAnswers;
            return copy;
        }
    }

    /** One answered card; {@code previous} is filled in once the review has been written. */
    private static class UndoEntry {
        final Flashcard card;
        @Nullable
        CardProgress previous;

        UndoEntry(Flashcard card) {
            this.card = card;
        }
    }

    private final StudyRepository repository;
    private final MutableLiveData<Deck> deck = new MutableLiveData<>(Deck.DUE);
    private final MutableLiveData<State> state = new MutableLiveData<>(State.simple(Type.LOADING, Deck.DUE));
    private final MutableLiveData<Boolean> reverse;
    private final MutableLiveData<StudyRepository.Stats> stats = new MutableLiveData<>();

    private FlashcardSession<Flashcard> session = new FlashcardSession<>(Collections.emptyList());
    private final Deque<UndoEntry> undoStack = new ArrayDeque<>();
    /** Reviews still being written; undo waits for them so it restores the right progress. */
    private int pendingReviews = 0;
    private boolean undoQueued = false;
    private boolean autoPickDeck = true;
    private int loadGeneration = 0;

    public FlashcardViewModel(@NonNull Application application) {
        super(application);
        repository = StudyRepository.getInstance(application);
        reverse = new MutableLiveData<>(AppPreferences.isFlashcardReverse(application));
    }

    public LiveData<Deck> getDeck() {
        return deck;
    }

    public LiveData<State> getState() {
        return state;
    }

    public LiveData<Boolean> getReverse() {
        return reverse;
    }

    public LiveData<StudyRepository.Stats> getStats() {
        return stats;
    }

    /** Loads the first deck: Due if there is anything to review, otherwise Random. */
    public void start(@Nullable Deck requested) {
        if (requested != null) {
            autoPickDeck = false;
            selectDeck(requested, true);
            return;
        }
        State current = state.getValue();
        // An empty deck may have filled up meanwhile (e.g. words starred on another tab)
        if (current != null && current.type == Type.EMPTY) {
            loadDeck();
            return;
        }
        if (current != null && current.type != Type.LOADING) return;
        if (!autoPickDeck) return;
        autoPickDeck = false;
        repository.getStats(s -> {
            stats.setValue(s);
            selectDeck(s.due > 0 ? Deck.DUE : Deck.RANDOM, true);
        });
    }

    public void selectDeck(Deck newDeck) {
        selectDeck(newDeck, false);
    }

    private void selectDeck(Deck newDeck, boolean force) {
        State current = state.getValue();
        boolean sameDeck = newDeck == deck.getValue();
        if (!force && sameDeck && current != null && current.type != Type.FINISHED) return;
        deck.setValue(newDeck);
        loadDeck();
    }

    public void restart() {
        loadDeck();
    }

    public void reviewMissed() {
        List<Flashcard> missed = session.getMissedCards();
        if (missed.isEmpty()) {
            loadDeck();
            return;
        }
        Collections.shuffle(missed);
        startSession(missed);
    }

    public void toggleReverse() {
        boolean value = !Boolean.TRUE.equals(reverse.getValue());
        AppPreferences.setFlashcardReverse(getApplication(), value);
        reverse.setValue(value);
        State current = state.getValue();
        if (current != null && current.type == Type.SHOWING) state.setValue(current.withFlipped(false));
    }

    /** Picks up a "meaning first" change made in Settings. */
    public void syncSettings() {
        boolean saved = AppPreferences.isFlashcardReverse(getApplication());
        if (saved == Boolean.TRUE.equals(reverse.getValue())) return;
        reverse.setValue(saved);
        State current = state.getValue();
        if (current != null && current.type == Type.SHOWING) state.setValue(current.withFlipped(false));
    }

    public void flip() {
        State current = state.getValue();
        if (current != null && current.type == Type.SHOWING) state.setValue(current.withFlipped(!current.flipped));
    }

    public void answer(boolean known) {
        Flashcard card = session.current();
        if (card == null) return;
        UndoEntry entry = new UndoEntry(card);
        undoStack.push(entry);
        pendingReviews++;
        repository.review(card, known, previous -> {
            entry.previous = previous;
            pendingReviews--;
            if (pendingReviews == 0) {
                refreshStats();
                if (undoQueued) {
                    undoQueued = false;
                    undo();
                }
            }
        });
        session.answer(known);
        showCurrent();
    }

    public void undo() {
        if (!session.canUndo()) return;
        if (pendingReviews > 0) {
            undoQueued = true;
            return;
        }
        UndoEntry entry = undoStack.poll();
        if (entry == null) return;
        repository.restore(entry.card, entry.previous, () -> {
            session.undo();
            showCurrent();
            refreshStats();
        });
    }

    @Nullable
    public Flashcard currentCard() {
        State current = state.getValue();
        return current != null && current.type == Type.SHOWING ? current.card : null;
    }

    public void refreshStats() {
        repository.getStats(stats::setValue);
    }

    private void loadDeck() {
        Deck current = deck.getValue() != null ? deck.getValue() : Deck.RANDOM;
        state.setValue(State.simple(Type.LOADING, current));
        int generation = ++loadGeneration;
        repository.loadDeck(current, SESSION_SIZE, cards -> {
            // A newer deck selection superseded this load
            if (generation != loadGeneration) return;
            startSession(cards);
        });
        refreshStats();
    }

    private void startSession(List<Flashcard> cards) {
        session = new FlashcardSession<>(cards);
        undoStack.clear();
        showCurrent();
    }

    private void showCurrent() {
        Deck current = deck.getValue() != null ? deck.getValue() : Deck.RANDOM;
        Flashcard card = session.current();
        if (session.isEmpty()) {
            state.setValue(State.simple(Type.EMPTY, current));
        } else if (card == null) {
            int missed = session.getMissedCards().size();
            int knownFirstTry = session.getKnownFirstTry();
            state.setValue(new State(Type.FINISHED, current, null, 0, knownFirstTry + missed, false,
                    session.canUndo(), false, 0, knownFirstTry, missed));
        } else {
            int box = session.isRepeat() || card.getBox() == null ? 0 : card.getBox();
            State showing = new State(Type.SHOWING, current, card, session.getPosition(), session.getTotal(), false,
                    session.canUndo(), session.isRepeat(), Leitner.nextIntervalDays(box), 0, 0);
            showing.knownAnswers = session.getKnownAnswers();
            showing.againAnswers = session.getAgainAnswers();
            state.setValue(showing);
        }
    }
}
