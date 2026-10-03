package net.golbarg.engtoper.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import net.golbarg.engtoper.db.DictionaryRepository;
import net.golbarg.engtoper.db.StudyRepository;
import net.golbarg.engtoper.models.PhraseEnglish;
import net.golbarg.engtoper.models.PhrasePersian;
import net.golbarg.engtoper.models.QuizQuestion;
import net.golbarg.engtoper.models.SearchFilter;
import net.golbarg.engtoper.models.SearchHistoryItem;
import net.golbarg.engtoper.util.AppPreferences;

import java.util.ArrayList;
import java.util.List;

/** Activity-scoped state shared by the dictionary, bookmarks, home and quiz screens. */
public class DictionaryViewModel extends AndroidViewModel {
    public static final String LANG_EN = "en";
    public static final String LANG_FA = "fa";

    /** A request from another screen to open the dictionary in a given direction, optionally with a query. */
    public static class DictionaryRequest {
        public final String lang;
        @Nullable
        public final String query;
        public final boolean focusSearch;

        public DictionaryRequest(String lang, @Nullable String query, boolean focusSearch) {
            this.lang = lang;
            this.query = query;
            this.focusSearch = focusSearch;
        }
    }

    private final DictionaryRepository repository;

    private final MutableLiveData<String> dictionaryLang;
    private final MutableLiveData<List<PhraseEnglish>> englishSearchResults = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<PhrasePersian>> persianSearchResults = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<PhraseEnglish>> englishBookmarks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<PhrasePersian>> persianBookmarks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<SearchHistoryItem>> recentSearches = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<PhraseEnglish> wordOfTheDay = new MutableLiveData<>();
    private final MutableLiveData<List<QuizQuestion>> quizQuestions = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<SearchFilter> searchFilter;
    private final MutableLiveData<DictionaryRequest> dictionaryRequest = new MutableLiveData<>();
    private final MutableLiveData<Integer> bookmarksVersion = new MutableLiveData<>(0);

    @Nullable
    private StudyRepository.Deck pendingFlashcardDeck;

    private String lastEnglishQuery = "";
    private String lastPersianQuery = "";

    public DictionaryViewModel(@NonNull Application application) {
        super(application);
        repository = DictionaryRepository.getInstance(application);
        dictionaryLang = new MutableLiveData<>(AppPreferences.getDictionaryLang(application));
        searchFilter = new MutableLiveData<>(AppPreferences.getSearchFilter(application));
        loadWordOfTheDay();
    }

    // ── Direction (English → Persian / Persian → English) ─────────────────────

    public LiveData<String> getDictionaryLang() {
        return dictionaryLang;
    }

    public String currentLang() {
        String lang = dictionaryLang.getValue();
        return lang != null ? lang : LANG_EN;
    }

    public void setDictionaryLang(String lang) {
        if (lang.equals(dictionaryLang.getValue())) return;
        AppPreferences.setDictionaryLang(getApplication(), lang);
        dictionaryLang.setValue(lang);
        loadRecentSearches(lang);
    }

    // ── Cross-tab requests ────────────────────────────────────────────────────

    public LiveData<DictionaryRequest> getDictionaryRequest() {
        return dictionaryRequest;
    }

    public void requestDictionary(String lang, @Nullable String query, boolean focusSearch) {
        dictionaryRequest.setValue(new DictionaryRequest(lang, query, focusSearch));
    }

    public void consumeDictionaryRequest() {
        dictionaryRequest.setValue(null);
    }

    public void requestFlashcardDeck(StudyRepository.Deck deck) {
        pendingFlashcardDeck = deck;
    }

    @Nullable
    public StudyRepository.Deck consumeFlashcardDeck() {
        StudyRepository.Deck deck = pendingFlashcardDeck;
        pendingFlashcardDeck = null;
        return deck;
    }

    // ── Search ────────────────────────────────────────────────────────────────

    public LiveData<List<PhraseEnglish>> getEnglishSearchResults() {
        return englishSearchResults;
    }

    public LiveData<List<PhrasePersian>> getPersianSearchResults() {
        return persianSearchResults;
    }

    public LiveData<List<SearchHistoryItem>> getRecentSearches() {
        return recentSearches;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<SearchFilter> getSearchFilter() {
        return searchFilter;
    }

    public void setSearchFilter(SearchFilter filter) {
        if (filter == searchFilter.getValue()) return;
        AppPreferences.setSearchFilter(getApplication(), filter);
        searchFilter.setValue(filter);
        if (!lastEnglishQuery.isEmpty()) {
            searchEnglish(lastEnglishQuery);
        }
        if (!lastPersianQuery.isEmpty()) {
            searchPersian(lastPersianQuery);
        }
    }

    public void searchEnglish(String query) {
        lastEnglishQuery = query != null ? query.trim() : "";
        if (lastEnglishQuery.isEmpty()) {
            englishSearchResults.setValue(new ArrayList<>());
            isLoading.setValue(false);
            return;
        }

        String requested = lastEnglishQuery;
        isLoading.setValue(true);
        repository.searchEnglish(requested, currentFilter(), results -> {
            // Ignore results of a query the user has already typed past
            if (!requested.equals(lastEnglishQuery)) return;
            englishSearchResults.setValue(results);
            isLoading.setValue(false);
        });
    }

    public void searchPersian(String query) {
        lastPersianQuery = query != null ? query.trim() : "";
        if (lastPersianQuery.isEmpty()) {
            persianSearchResults.setValue(new ArrayList<>());
            isLoading.setValue(false);
            return;
        }

        String requested = lastPersianQuery;
        isLoading.setValue(true);
        repository.searchPersian(requested, currentFilter(), results -> {
            if (!requested.equals(lastPersianQuery)) return;
            persianSearchResults.setValue(results);
            isLoading.setValue(false);
        });
    }

    private SearchFilter currentFilter() {
        return searchFilter.getValue() != null ? searchFilter.getValue() : SearchFilter.STARTS_WITH;
    }

    /** Saves a finished search (submitted or a result was opened) — not every keystroke. */
    public void saveSearch(String query, String lang) {
        if (query == null || query.trim().isEmpty()) return;
        repository.addSearchHistory(query.trim(), lang);
    }

    public void loadRecentSearches(String lang) {
        repository.getRecentSearches(lang, 15, recentSearches::setValue);
    }

    public void deleteSearchHistoryItem(int id, String lang) {
        repository.deleteSearchHistoryItem(id, () -> loadRecentSearches(lang));
    }

    public void clearSearchHistory(String lang) {
        repository.clearSearchHistory(lang, () -> loadRecentSearches(lang));
    }

    // ── Bookmarks ─────────────────────────────────────────────────────────────

    public LiveData<List<PhraseEnglish>> getEnglishBookmarks() {
        return englishBookmarks;
    }

    public LiveData<List<PhrasePersian>> getPersianBookmarks() {
        return persianBookmarks;
    }

    /** Bumped whenever a bookmark changes, so screens showing counts can refresh. */
    public LiveData<Integer> getBookmarksVersion() {
        return bookmarksVersion;
    }

    public void loadEnglishBookmarks() {
        isLoading.setValue(true);
        repository.getBookmarksEnglish(bookmarks -> {
            englishBookmarks.setValue(bookmarks);
            isLoading.setValue(false);
        });
    }

    public void loadPersianBookmarks() {
        isLoading.setValue(true);
        repository.getBookmarksPersian(bookmarks -> {
            persianBookmarks.setValue(bookmarks);
            isLoading.setValue(false);
        });
    }

    public void toggleFavoriteEnglish(PhraseEnglish phrase) {
        toggleFavoriteEnglish(phrase, null);
    }

    public void toggleFavoriteEnglish(PhraseEnglish phrase, @Nullable Runnable onDone) {
        repository.toggleFavoriteEnglish(phrase, isFav -> onEnglishBookmarkChanged(onDone));
    }

    public void toggleFavoritePersian(PhrasePersian phrase) {
        toggleFavoritePersian(phrase, null);
    }

    public void toggleFavoritePersian(PhrasePersian phrase, @Nullable Runnable onDone) {
        repository.toggleFavoritePersian(phrase, isFav -> onPersianBookmarkChanged(onDone));
    }

    /** Sets an explicit bookmark state, e.g. the state the word detail sheet already shows. */
    public void setFavoriteEnglish(PhraseEnglish phrase, int favorite, @Nullable Runnable onDone) {
        repository.setFavoriteEnglish(phrase, favorite, isFav -> onEnglishBookmarkChanged(onDone));
    }

    public void setFavoritePersian(PhrasePersian phrase, int favorite, @Nullable Runnable onDone) {
        repository.setFavoritePersian(phrase, favorite, isFav -> onPersianBookmarkChanged(onDone));
    }

    private void onEnglishBookmarkChanged(@Nullable Runnable onDone) {
        loadEnglishBookmarks();
        bumpBookmarksVersion();
        if (onDone != null) onDone.run();
    }

    private void onPersianBookmarkChanged(@Nullable Runnable onDone) {
        loadPersianBookmarks();
        bumpBookmarksVersion();
        if (onDone != null) onDone.run();
    }

    private void bumpBookmarksVersion() {
        Integer version = bookmarksVersion.getValue();
        bookmarksVersion.setValue(version == null ? 1 : version + 1);
    }

    // ── Word of the day / random / quiz ───────────────────────────────────────

    public LiveData<PhraseEnglish> getWordOfTheDay() {
        return wordOfTheDay;
    }

    public void loadWordOfTheDay() {
        repository.getWordOfTheDay(wordOfTheDay::setValue);
    }

    public void getRandomWord(DictionaryRepository.Callback<PhraseEnglish> callback) {
        repository.getRandomEnglishWord(callback);
    }

    public LiveData<List<QuizQuestion>> getQuizQuestions() {
        return quizQuestions;
    }

    public void startQuiz(int questionCount) {
        isLoading.setValue(true);
        repository.generateQuizQuestions(questionCount, questions -> {
            quizQuestions.setValue(questions);
            isLoading.setValue(false);
        });
    }
}
