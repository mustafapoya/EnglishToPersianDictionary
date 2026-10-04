package net.golbarg.engtoper.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import net.golbarg.engtoper.ads.AdUtil;
import net.golbarg.engtoper.ads.CachedNativeAd;
import net.golbarg.engtoper.db.DictionaryRepository;
import net.golbarg.engtoper.db.StudyRepository;
import net.golbarg.engtoper.models.PhraseEnglish;
import net.golbarg.engtoper.models.SearchHistoryItem;
import net.golbarg.engtoper.util.AppPreferences;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Dashboard data: study stats, weekly activity, words to explore and recent searches. */
public class HomeViewModel extends AndroidViewModel {
    private static final int RECENT_LIMIT = 5;
    private static final int EXPLORE_COUNT = 8;

    private final DictionaryRepository dictionaryRepository;
    private final StudyRepository studyRepository;

    private final MutableLiveData<StudyRepository.Stats> stats = new MutableLiveData<>();
    private final MutableLiveData<Set<LocalDate>> studyDays = new MutableLiveData<>();
    private final MutableLiveData<List<SearchHistoryItem>> recentSearches = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<PhraseEnglish>> exploreWords = new MutableLiveData<>();
    /** The sponsored card, kept across tab switches and rotation. */
    private final CachedNativeAd sponsored = new CachedNativeAd(AdUtil.Placement.HOME_NATIVE);

    public HomeViewModel(@NonNull Application application) {
        super(application);
        dictionaryRepository = DictionaryRepository.getInstance(application);
        studyRepository = StudyRepository.getInstance(application);
        shuffleExplore();
    }

    CachedNativeAd getSponsored() {
        return sponsored;
    }

    @Override
    protected void onCleared() {
        sponsored.clear();
    }

    public LiveData<StudyRepository.Stats> getStats() {
        return stats;
    }

    public LiveData<Set<LocalDate>> getStudyDays() {
        return studyDays;
    }

    public LiveData<List<SearchHistoryItem>> getRecentSearches() {
        return recentSearches;
    }

    public LiveData<List<PhraseEnglish>> getExploreWords() {
        return exploreWords;
    }

    public void refresh() {
        refreshStats();
        studyDays.setValue(AppPreferences.getRecentStudyDays(getApplication()));
        dictionaryRepository.getRecentSearches(null, RECENT_LIMIT, recentSearches::setValue);
    }

    public void refreshStats() {
        studyRepository.getStats(stats::setValue);
    }

    public void shuffleExplore() {
        dictionaryRepository.getRandomEnglishWords(EXPLORE_COUNT, exploreWords::setValue);
    }

    public void clearRecentSearches() {
        dictionaryRepository.clearSearchHistory("en", () ->
                dictionaryRepository.clearSearchHistory("fa", () -> recentSearches.setValue(new ArrayList<>())));
    }
}
