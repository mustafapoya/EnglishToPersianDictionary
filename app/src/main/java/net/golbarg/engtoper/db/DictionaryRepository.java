package net.golbarg.engtoper.db;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import net.golbarg.engtoper.models.PhraseEnglish;
import net.golbarg.engtoper.models.PhrasePersian;
import net.golbarg.engtoper.models.QuizQuestion;
import net.golbarg.engtoper.models.SearchFilter;
import net.golbarg.engtoper.models.SearchHistoryItem;
import net.golbarg.engtoper.util.AppPreferences;
import net.golbarg.engtoper.util.EnglishForms;
import net.golbarg.engtoper.util.SpellSuggester;
import net.golbarg.engtoper.util.TranslationParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DictionaryRepository {
    private static DictionaryRepository instance;
    private final Context appContext;
    private final TablePhraseEnglish tablePhraseEnglish;
    private final TablePhrasePersian tablePhrasePersian;
    private final TableSearchHistory tableSearchHistory;
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback<T> {
        void onResult(T result);
    }

    private DictionaryRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.tablePhraseEnglish = new TablePhraseEnglish(appContext);
        this.tablePhrasePersian = new TablePhrasePersian(appContext);
        this.tableSearchHistory = new TableSearchHistory(DatabaseHandler.getInstance(appContext));
    }

    public static synchronized DictionaryRepository getInstance(Context context) {
        if (instance == null) {
            instance = new DictionaryRepository(context);
        }
        return instance;
    }

    TablePhraseEnglish englishTable() {
        return tablePhraseEnglish;
    }

    TablePhrasePersian persianTable() {
        return tablePhrasePersian;
    }

    public void searchEnglish(String query, SearchFilter filter, Callback<List<PhraseEnglish>> callback) {
        executorService.execute(() -> {
            List<PhraseEnglish> results = tablePhraseEnglish.search(query, filter, 100);
            mainHandler.post(() -> callback.onResult(results));
        });
    }

    public void searchPersian(String query, SearchFilter filter, Callback<List<PhrasePersian>> callback) {
        executorService.execute(() -> {
            List<PhrasePersian> results = tablePhrasePersian.search(query, filter, 100);
            mainHandler.post(() -> callback.onResult(results));
        });
    }

    public void getBookmarksEnglish(Callback<List<PhraseEnglish>> callback) {
        executorService.execute(() -> {
            List<PhraseEnglish> bookmarks = tablePhraseEnglish.getBookmarks();
            mainHandler.post(() -> callback.onResult(bookmarks));
        });
    }

    public void getBookmarksPersian(Callback<List<PhrasePersian>> callback) {
        executorService.execute(() -> {
            List<PhrasePersian> bookmarks = tablePhrasePersian.getBookmarks();
            mainHandler.post(() -> callback.onResult(bookmarks));
        });
    }

    public void toggleFavoriteEnglish(PhraseEnglish phrase, Callback<Boolean> callback) {
        executorService.execute(() -> {
            int newFav = phrase.getFavorite() == 1 ? 0 : 1;
            phrase.setFavorite(newFav);
            tablePhraseEnglish.updateFavorite(phrase);
            mainHandler.post(() -> {
                if (callback != null) callback.onResult(newFav == 1);
            });
        });
    }

    public void toggleFavoritePersian(PhrasePersian phrase, Callback<Boolean> callback) {
        executorService.execute(() -> {
            int newFav = phrase.getFavorite() == 1 ? 0 : 1;
            phrase.setFavorite(newFav);
            tablePhrasePersian.updateFavorite(phrase);
            mainHandler.post(() -> {
                if (callback != null) callback.onResult(newFav == 1);
            });
        });
    }

    /** Explicitly sets the bookmark state (unlike toggle, safe to call with a state the UI already chose). */
    public void setFavoriteEnglish(PhraseEnglish phrase, int favorite, Callback<Boolean> callback) {
        phrase.setFavorite(favorite);
        executorService.execute(() -> {
            tablePhraseEnglish.updateFavorite(phrase);
            mainHandler.post(() -> {
                if (callback != null) callback.onResult(favorite == 1);
            });
        });
    }

    public void setFavoritePersian(PhrasePersian phrase, int favorite, Callback<Boolean> callback) {
        phrase.setFavorite(favorite);
        executorService.execute(() -> {
            tablePhrasePersian.updateFavorite(phrase);
            mainHandler.post(() -> {
                if (callback != null) callback.onResult(favorite == 1);
            });
        });
    }

    /** The same word all day; a new one is picked the first time it's requested on a new day. */
    public void getWordOfTheDay(Callback<PhraseEnglish> callback) {
        executorService.execute(() -> {
            int savedId = AppPreferences.getWordOfDayId(appContext);
            PhraseEnglish word = savedId > 0 ? tablePhraseEnglish.get(savedId) : null;
            if (word == null) {
                word = tablePhraseEnglish.getRandomWord();
                if (word != null) AppPreferences.setWordOfDayId(appContext, word.getId());
            }
            PhraseEnglish result = word;
            mainHandler.post(() -> callback.onResult(result));
        });
    }

    /** Exact entry for text selected in another app; tries base forms ("running" → "run"). Null if none. */
    public void lookupEnglish(String text, Callback<PhraseEnglish> callback) {
        executorService.execute(() -> {
            PhraseEnglish found = null;
            for (String form : EnglishForms.candidates(text)) {
                List<PhraseEnglish> matches = tablePhraseEnglish.search(form, SearchFilter.EXACT, 1);
                if (!matches.isEmpty()) {
                    found = matches.get(0);
                    break;
                }
            }
            PhraseEnglish result = found;
            mainHandler.post(() -> callback.onResult(result));
        });
    }

    public void lookupPersian(String text, Callback<PhrasePersian> callback) {
        executorService.execute(() -> {
            List<PhrasePersian> matches = tablePhrasePersian.search(text, SearchFilter.EXACT, 1);
            PhrasePersian result = matches.isEmpty() ? null : matches.get(0);
            mainHandler.post(() -> callback.onResult(result));
        });
    }

    /** "Did you mean…?" spellings close to a query that found nothing. */
    public void suggestSpellings(String query, boolean english, Callback<List<String>> callback) {
        executorService.execute(() -> {
            String q = query == null ? "" : query.trim();
            List<String> suggestions = new ArrayList<>();
            if (q.length() >= 2) {
                int gap = SpellSuggester.maxDistance(q.length());
                List<String> candidates = english
                        ? tablePhraseEnglish.suggestionCandidates(q, gap)
                        : tablePhrasePersian.suggestionCandidates(q, gap);
                suggestions = SpellSuggester.rank(q, candidates, 5);
            }
            List<String> result = suggestions;
            mainHandler.post(() -> callback.onResult(result));
        });
    }

    /** Number of English and Persian entries, as {english, persian}. */
    public void getEntryCounts(Callback<int[]> callback) {
        executorService.execute(() -> {
            int[] counts = {tablePhraseEnglish.countEntries(), tablePhrasePersian.countEntries()};
            mainHandler.post(() -> callback.onResult(counts));
        });
    }

    public void clearAllSearchHistory(Runnable onDone) {
        executorService.execute(() -> {
            tableSearchHistory.clearAll("en");
            tableSearchHistory.clearAll("fa");
            if (onDone != null) mainHandler.post(onDone);
        });
    }

    public void getRandomEnglishWords(int count, Callback<List<PhraseEnglish>> callback) {
        executorService.execute(() -> {
            List<PhraseEnglish> words = tablePhraseEnglish.getRandomWords(count, -1);
            mainHandler.post(() -> callback.onResult(words));
        });
    }

    public void getRandomEnglishWord(Callback<PhraseEnglish> callback) {
        executorService.execute(() -> {
            PhraseEnglish word = tablePhraseEnglish.getRandomWord();
            mainHandler.post(() -> callback.onResult(word));
        });
    }

    public void generateQuizQuestions(int count, Callback<List<QuizQuestion>> callback) {
        executorService.execute(() -> {
            List<QuizQuestion> questions = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                PhraseEnglish correctWord = tablePhraseEnglish.getRandomWord();
                if (correctWord == null) continue;

                ArrayList<PhraseEnglish> wrongWords = tablePhraseEnglish.getRandomWords(3, correctWord.getId());
                if (wrongWords.size() < 3) continue;

                List<String> options = new ArrayList<>();
                String correctTrans = quizOption(correctWord.getToLanguage());
                if (correctTrans.isEmpty()) continue;

                options.add(correctTrans);
                for (PhraseEnglish wrong : wrongWords) {
                    String wrongTrans = quizOption(wrong.getToLanguage());
                    if (!options.contains(wrongTrans) && !wrongTrans.isEmpty()) {
                        options.add(wrongTrans);
                    } else {
                        options.add(wrong.getFromLanguage());
                    }
                }

                while (options.size() < 4) {
                    options.add("گزینه " + (options.size() + 1));
                }

                Collections.shuffle(options);
                int correctIdx = options.indexOf(correctTrans);

                questions.add(new QuizQuestion(
                        correctWord.getFromLanguage(),
                        correctTrans,
                        options,
                        correctIdx,
                        "en"
                ));
            }
            mainHandler.post(() -> callback.onResult(questions));
        });
    }

    /** A short answer option: the first two general meanings (or the first subject meaning). */
    private static String quizOption(String rawTranslation) {
        TranslationParser.Parsed parsed = TranslationParser.parse(rawTranslation);
        if (parsed.meanings.isEmpty()) {
            return parsed.subjects.isEmpty() ? "" : parsed.subjects.get(0).meaning;
        }
        return String.join("، ", parsed.meanings.subList(0, Math.min(2, parsed.meanings.size())));
    }

    public void addSearchHistory(String query, String lang) {
        executorService.execute(() -> tableSearchHistory.addOrUpdate(query, lang));
    }

    public void getRecentSearches(String lang, int limit, Callback<List<SearchHistoryItem>> callback) {
        executorService.execute(() -> {
            List<SearchHistoryItem> items = tableSearchHistory.getRecent(lang, limit);
            mainHandler.post(() -> callback.onResult(items));
        });
    }

    public void deleteSearchHistoryItem(int id, Runnable onDone) {
        executorService.execute(() -> {
            tableSearchHistory.delete(id);
            if (onDone != null) mainHandler.post(onDone);
        });
    }

    public void clearSearchHistory(String lang, Runnable onDone) {
        executorService.execute(() -> {
            tableSearchHistory.clearAll(lang);
            if (onDone != null) mainHandler.post(onDone);
        });
    }
}
