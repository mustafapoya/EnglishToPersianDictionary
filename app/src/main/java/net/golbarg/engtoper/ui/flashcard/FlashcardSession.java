package net.golbarg.engtoper.ui.flashcard;

import androidx.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Queue for one study session. A missed card is re-queued at the end (at most {@link #MAX_REPEATS}
 * times) so it comes back before the session ends. Every answer can be undone.
 */
public class FlashcardSession<T> {
    private static final int MAX_REPEATS = 2;

    private static class Step {
        final boolean known;
        final boolean requeued;
        final boolean firstMiss;

        Step(boolean known, boolean requeued, boolean firstMiss) {
            this.known = known;
            this.requeued = requeued;
            this.firstMiss = firstMiss;
        }
    }

    private final int originalSize;
    private final List<T> queue;
    private final Deque<Step> history = new ArrayDeque<>();
    private final Set<T> missed = new LinkedHashSet<>();
    private int position = 0;

    public FlashcardSession(List<T> cards) {
        originalSize = cards.size();
        queue = new ArrayList<>(cards);
    }

    @Nullable
    public T current() {
        return position < queue.size() ? queue.get(position) : null;
    }

    public int getPosition() {
        return position;
    }

    public int getTotal() {
        return queue.size();
    }

    public boolean isEmpty() {
        return originalSize == 0;
    }

    /** True once the card being shown is a re-queued miss rather than a first appearance. */
    public boolean isRepeat() {
        return position >= originalSize;
    }

    public boolean canUndo() {
        return !history.isEmpty();
    }

    public List<T> getMissedCards() {
        return new ArrayList<>(missed);
    }

    public int getKnownFirstTry() {
        return originalSize - missed.size();
    }

    /** Answers given so far this session (including repeats), for the live tally. */
    public int getKnownAnswers() {
        int count = 0;
        for (Step step : history) if (step.known) count++;
        return count;
    }

    public int getAgainAnswers() {
        return history.size() - getKnownAnswers();
    }

    public void answer(boolean known) {
        T card = current();
        if (card == null) return;
        boolean firstMiss = !known && missed.add(card);
        boolean requeued = !known && Collections.frequency(queue, card) <= MAX_REPEATS;
        if (requeued) queue.add(card);
        history.push(new Step(known, requeued, firstMiss));
        position++;
    }

    public boolean undo() {
        Step step = history.poll();
        if (step == null) return false;
        position--;
        if (step.requeued) queue.remove(queue.size() - 1);
        if (step.firstMiss) missed.remove(queue.get(position));
        return true;
    }
}
