package net.golbarg.engtoper.ui.flashcard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

import net.golbarg.engtoper.MainActivity;
import net.golbarg.engtoper.R;
import net.golbarg.engtoper.ads.AdFreeOffer;
import net.golbarg.engtoper.ads.AdUtil;
import net.golbarg.engtoper.ads.NativeAdBinder;
import net.golbarg.engtoper.ads.SessionInterstitial;
import net.golbarg.engtoper.databinding.FragmentFlashcardBinding;
import net.golbarg.engtoper.databinding.ViewStatBinding;
import net.golbarg.engtoper.db.StudyRepository;
import net.golbarg.engtoper.db.StudyRepository.Deck;
import net.golbarg.engtoper.models.Flashcard;
import net.golbarg.engtoper.models.PhraseEnglish;
import net.golbarg.engtoper.models.PhrasePersian;
import net.golbarg.engtoper.ui.DictionaryViewModel;
import net.golbarg.engtoper.ui.WordDetailBottomSheet;
import net.golbarg.engtoper.ui.common.MeaningList;
import net.golbarg.engtoper.util.Leitner;
import net.golbarg.engtoper.util.LocaleUtil;
import net.golbarg.engtoper.util.TTSManager;

/** Leitner flashcards: tap to flip, swipe right if you knew it, left to see it again. */
public class FlashcardFragment extends Fragment {

    private static final int CAMERA_DISTANCE = 8000;
    private static final float FLIP_ANGLE = 90f;
    private static final long FLIP_MS = 180;
    private static final long SWIPE_MS = 220;
    private static final float EXIT_FACTOR = 1.4f;
    private static final float ROTATION_PER_PX = 0.05f;
    private static final float ENTER_SCALE = 0.92f;
    private static final float MIN_DRAG_ALPHA = 0.4f;
    private static final float SWIPE_THRESHOLD = 0.28f;
    private static final float MAX_TINT_ALPHA = 0.28f;
    private static final int BACK_MAX_MEANINGS = 6;

    private FragmentFlashcardBinding binding;
    private FlashcardViewModel viewModel;
    private DictionaryViewModel dictionaryViewModel;

    private boolean isAnimating = false;
    private boolean showingBack = false;
    private boolean reverse = false;
    private int boundPosition = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFlashcardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(FlashcardViewModel.class);
        dictionaryViewModel = new ViewModelProvider(requireActivity()).get(DictionaryViewModel.class);

        float distance = CAMERA_DISTANCE * getResources().getDisplayMetrics().density;
        binding.cardFront.setCameraDistance(distance);
        binding.cardBack.setCameraDistance(distance);
        boundPosition = -1;

        setupControls();
        setupResult();
        setupGestures();
        NativeAd restoredAd = viewModel.getAdSlot().showing();
        if (restoredAd != null) bindAd(restoredAd);
        observeViewModel();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Home's "Review cards" asks for a specific deck; otherwise pick Due or Random on first open
        viewModel.syncSettings();
        viewModel.start(dictionaryViewModel.consumeFlashcardDeck());
        viewModel.refreshStats();
    }

    private float swipeThreshold() {
        return binding.cardContainer.getWidth() * SWIPE_THRESHOLD;
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupControls() {
        binding.chipsDeck.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            if (id == R.id.chip_deck_due) viewModel.selectDeck(Deck.DUE);
            else if (id == R.id.chip_deck_saved) viewModel.selectDeck(Deck.SAVED);
            else if (id == R.id.chip_deck_random) viewModel.selectDeck(Deck.RANDOM);
        });
        binding.btnShowAnswer.setOnClickListener(v -> flipCard());
        binding.btnAgain.setOnClickListener(v -> answer(false));
        binding.btnKnow.setOnClickListener(v -> answer(true));
        binding.btnListen.setOnClickListener(v -> speakCurrent());
        binding.btnListenBack.setOnClickListener(v -> speakCurrent());
        binding.btnOpenWord.setOnClickListener(v -> openCurrentWord());
        binding.btnUndo.setOnClickListener(v -> {
            if (isAnimating) return;
            if (isShowingAd()) closeAd();
            viewModel.undo();
        });
        binding.btnRemoveAds.setOnClickListener(v -> AdFreeOffer.show(requireActivity()));
        binding.btnAdContinue.setOnClickListener(v -> {
            if (!isAnimating) swipeAdAway(binding.cardContainer.getWidth() * EXIT_FACTOR * endDirection());
        });
        binding.btnReverse.setOnClickListener(v -> toggleReverse());
    }

    private void setupResult() {
        bindStatLabel(binding.result.statKnown, R.drawable.ic_check, R.string.flashcard_stat_known);
        bindStatLabel(binding.result.statMissed, R.drawable.ic_refresh, R.string.flashcard_stat_missed);
        bindStatLabel(binding.result.statLearned, R.drawable.ic_trophy, R.string.flashcard_stat_learned);
        binding.result.btnReviewMissed.setOnClickListener(v -> viewModel.reviewMissed());
        binding.result.btnNewSession.setOnClickListener(v -> viewModel.restart());
    }

    private void bindStatLabel(ViewStatBinding stat, @DrawableRes int icon, @StringRes int label) {
        stat.icon.setImageResource(icon);
        stat.textLabel.setText(label);
    }

    private void observeViewModel() {
        viewModel.getDeck().observe(getViewLifecycleOwner(), deck -> {
            int id = deck == Deck.DUE ? R.id.chip_deck_due
                    : deck == Deck.SAVED ? R.id.chip_deck_saved
                    : R.id.chip_deck_random;
            if (binding.chipsDeck.getCheckedChipId() != id) binding.chipsDeck.check(id);
        });
        viewModel.getStats().observe(getViewLifecycleOwner(), this::renderStats);
        viewModel.getReverse().observe(getViewLifecycleOwner(), isReverse -> {
            reverse = Boolean.TRUE.equals(isReverse);
            boundPosition = -1;
            FlashcardViewModel.State state = viewModel.getState().getValue();
            if (state != null) render(state);
        });
        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
        // Ads turned off (e.g. the user earned an ad-free day): take the ad card away at once
        AdUtil.allowedState().observe(getViewLifecycleOwner(), allowed -> {
            if (Boolean.TRUE.equals(allowed)) return;
            boolean wasShowing = isShowingAd();
            viewModel.getAdSlot().dropAds();
            if (wasShowing) closeAd();
        });
    }

    private void toggleReverse() {
        if (isAnimating) return;
        viewModel.toggleReverse();
        Snackbar snackbar = Snackbar.make(binding.getRoot(),
                reverse ? R.string.flashcard_mode_meaning_first : R.string.flashcard_mode_word_first,
                Snackbar.LENGTH_SHORT);
        // Keep it above the bottom navigation instead of covering the tabs
        View navBar = requireActivity().findViewById(R.id.nav_container);
        if (navBar != null && navBar.getVisibility() == View.VISIBLE) snackbar.setAnchorView(navBar);
        snackbar.show();
    }

    private void speakCurrent() {
        Flashcard card = viewModel.currentCard();
        if (card != null && card.isEnglish()) {
            TTSManager.getInstance(requireContext()).speak(card.getWord(), requireContext());
        }
    }

    private void openCurrentWord() {
        Flashcard card = viewModel.currentCard();
        if (card == null) return;
        WordDetailBottomSheet sheet;
        if (card.isEnglish()) {
            PhraseEnglish phrase = card.toPhraseEnglish();
            sheet = WordDetailBottomSheet.newInstance(phrase);
            sheet.setOnBookmarkToggleListener((id, isEnglish, newState) ->
                    dictionaryViewModel.setFavoriteEnglish(phrase, newState, () -> onCardBookmarkChanged(card, newState)));
        } else {
            PhrasePersian phrase = card.toPhrasePersian();
            sheet = WordDetailBottomSheet.newInstance(phrase);
            sheet.setOnBookmarkToggleListener((id, isEnglish, newState) ->
                    dictionaryViewModel.setFavoritePersian(phrase, newState, () -> onCardBookmarkChanged(card, newState)));
        }
        sheet.show(getChildFragmentManager(), "WORD_DETAIL");
    }

    private void onCardBookmarkChanged(Flashcard card, int favorite) {
        card.setFavorite(favorite);
        viewModel.refreshStats();
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private void renderStats(@Nullable StudyRepository.Stats stats) {
        if (stats == null) return;
        binding.textStats.setText(getString(R.string.flashcard_stats, stats.learned, stats.due));
        setChipCount(binding.chipDeckDue, R.string.deck_due, stats.due);
        setChipCount(binding.chipDeckSaved, R.string.deck_saved, stats.saved);
        binding.result.statLearned.textValue.setText(LocaleUtil.number(requireContext(), stats.learned));
    }

    private void setChipCount(Chip chip, @StringRes int label, int count) {
        chip.setText(count > 0 ? getString(R.string.deck_with_count, getString(label), count) : getString(label));
    }

    private void render(FlashcardViewModel.State state) {
        FlashcardViewModel.Type type = state.type;
        binding.panelLoading.setVisibility(type == FlashcardViewModel.Type.LOADING ? View.VISIBLE : View.GONE);
        binding.panelStudy.setVisibility(type == FlashcardViewModel.Type.SHOWING ? View.VISIBLE : View.GONE);
        binding.panelEmpty.setVisibility(type == FlashcardViewModel.Type.EMPTY ? View.VISIBLE : View.GONE);
        binding.result.getRoot().setVisibility(type == FlashcardViewModel.Type.FINISHED ? View.VISIBLE : View.GONE);
        binding.btnUndo.setVisibility(state.canUndo ? View.VISIBLE : View.GONE);
        if (type != FlashcardViewModel.Type.SHOWING) {
            boundPosition = -1;
            viewModel.getAdSlot().dismiss();
        }

        switch (type) {
            case SHOWING:
                renderCard(state);
                break;
            case EMPTY:
                renderEmpty(state.deck);
                break;
            case FINISHED:
                renderFinished(state);
                break;
            default:
                break;
        }
        renderAd();
    }

    private void renderCard(FlashcardViewModel.State state) {
        binding.progressSession.setMax(state.total);
        binding.progressSession.setProgressCompat(state.position, true);
        binding.textProgress.setText(getString(R.string.flashcard_progress, state.position + 1, state.total));
        binding.textLevel.setText(levelText(state));
        binding.textHint.setText(getResources().getQuantityString(
                R.plurals.flashcard_hint_next, state.nextIntervalDays, state.nextIntervalDays));
        renderTally(binding.textTallyKnown, state.knownAnswers);
        renderTally(binding.textTallyAgain, state.againAnswers);

        // Cards still waiting after this one peek out from behind it
        int remaining = state.total - state.position - 1;
        // A long enough session may end with a full-screen ad: get it ready near the end
        if (remaining < 3 && state.total >= SessionInterstitial.MIN_ANSWERS) SessionInterstitial.preload(requireContext());
        binding.cardGhostNear.setVisibility(remaining >= 1 ? View.VISIBLE : View.INVISIBLE);
        binding.cardGhostFar.setVisibility(remaining >= 2 ? View.VISIBLE : View.INVISIBLE);

        if (state.position != boundPosition) {
            showNewCard(state);
        } else if (!isAnimating && state.flipped != showingBack) {
            showFace(state.flipped);
        }
    }

    private void renderTally(TextView pill, int count) {
        pill.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        pill.setText(LocaleUtil.number(requireContext(), count));
    }

    private void showNewCard(FlashcardViewModel.State state) {
        bindCard(state.card);
        boundPosition = state.position;
        showFace(state.flipped);
        if (!state.flipped) animateCardIn();
    }

    private String levelText(FlashcardViewModel.State state) {
        Integer box = state.card != null ? state.card.getBox() : null;
        if (state.isRepeat) return getString(R.string.flashcard_level_repeat);
        if (box == null) return getString(R.string.flashcard_level_new);
        if (box == 0) return getString(R.string.flashcard_level_learning);
        return getString(R.string.flashcard_level, box, Leitner.MAX_BOX);
    }

    private void bindCard(@Nullable Flashcard card) {
        if (card == null) return;
        boolean english = card.isEnglish();
        boolean meaningFirst = reverse && card.hasMeaning();

        binding.textFrontLang.setText(meaningFirst
                ? (english ? R.string.flashcard_lang_persian_meaning : R.string.flashcard_lang_english_meaning)
                : (english ? R.string.flashcard_lang_english : R.string.flashcard_lang_persian));
        binding.textFrontWord.setText(meaningFirst ? card.getShortMeaning() : card.getWord());
        binding.textFrontWord.setTextSize(meaningFirst ? 24 : 38);
        binding.btnListen.setVisibility(english && !meaningFirst ? View.VISIBLE : View.INVISIBLE);

        binding.textBackWord.setText(card.getWord());
        binding.btnListenBack.setVisibility(english ? View.VISIBLE : View.GONE);
        binding.textBackLabel.setText(english ? R.string.flashcard_lang_persian_meaning : R.string.flashcard_lang_english_meaning);
        int hidden = MeaningList.render(binding.listBackMeanings, card.getParsed(), BACK_MAX_MEANINGS, english);
        binding.textBackMore.setVisibility(hidden > 0 ? View.VISIBLE : View.GONE);
        binding.textBackMore.setText(getResources().getQuantityString(R.plurals.wod_more_meanings, hidden,
                LocaleUtil.number(requireContext(), hidden)));
    }

    private void renderEmpty(Deck deck) {
        boolean saved = deck == Deck.SAVED;
        binding.imgEmpty.setImageResource(saved ? R.drawable.ic_star_outline : R.drawable.ic_trophy);
        binding.textEmptyTitle.setText(saved ? R.string.deck_saved_empty_title : R.string.deck_due_empty_title);
        binding.textEmptyMessage.setText(saved ? R.string.deck_saved_empty : R.string.deck_due_empty);
        binding.btnEmptyAction.setText(saved ? R.string.bookmarks_explore : R.string.deck_study_random);
        binding.btnEmptyAction.setOnClickListener(v -> {
            if (saved && requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openTab(R.id.navigation_dictionary);
            } else {
                viewModel.selectDeck(Deck.RANDOM);
            }
        });
    }

    private void renderFinished(FlashcardViewModel.State state) {
        binding.result.textScore.setText(getString(R.string.flashcard_score, state.knownFirstTry, state.total));
        binding.result.statKnown.textValue.setText(LocaleUtil.number(requireContext(), state.knownFirstTry));
        binding.result.statMissed.textValue.setText(LocaleUtil.number(requireContext(), state.missed));
        binding.result.btnReviewMissed.setVisibility(state.missed > 0 ? View.VISIBLE : View.GONE);
        binding.result.btnReviewMissed.setText(getString(R.string.flashcard_review_missed, state.missed));
        binding.result.getRoot().scrollTo(0, 0);
    }

    private void showFace(boolean back) {
        showingBack = back;
        for (View face : new View[]{binding.cardFront, binding.cardBack}) {
            face.setRotationY(0f);
            face.setTranslationX(0f);
            face.setRotation(0f);
            face.setAlpha(1f);
        }
        binding.cardFront.setVisibility(back ? View.GONE : View.VISIBLE);
        binding.cardBack.setVisibility(back ? View.VISIBLE : View.GONE);
        binding.tintFront.setAlpha(0f);
        binding.tintBack.setAlpha(0f);
        binding.btnShowAnswer.setVisibility(back ? View.GONE : View.VISIBLE);
        binding.layoutRate.setVisibility(back ? View.VISIBLE : View.GONE);
        binding.textHint.setVisibility(back ? View.VISIBLE : View.INVISIBLE);
    }

    /** The card being dragged: the ad while one is showing, otherwise the visible face. */
    private View currentFace() {
        if (isShowingAd()) return binding.adCard.getRoot();
        return showingBack ? binding.cardBack : binding.cardFront;
    }

    // ── Native ad between cards ───────────────────────────────────────────────

    private boolean isShowingAd() {
        return viewModel.getAdSlot().showing() != null;
    }

    /**
     * After an answer: shows an ad card if one is due and the session goes on, or, when the
     * session has just ended, possibly a full-screen ad before its results (a natural break).
     */
    private void maybeShowAd() {
        FlashcardAdSlot slot = viewModel.getAdSlot();
        slot.onCardAnswered();
        FlashcardViewModel.State state = viewModel.getState().getValue();
        if (state != null && state.type == FlashcardViewModel.Type.FINISHED) {
            SessionInterstitial.showAtSessionEnd(requireActivity(), state.total);
            return;
        }
        if (state == null || state.type != FlashcardViewModel.Type.SHOWING) return;
        NativeAd ad = slot.takeDueAd();
        if (ad == null) return;
        bindAd(ad);
        renderAd();
        animateIn(binding.adCard.getRoot());
    }

    private void bindAd(NativeAd ad) {
        NativeAdBinder.bind(binding.adCard.nativeAdView, ad);
    }

    /** While an ad shows it covers the card, and "Remove ads" / "Continue" replace the answer buttons. */
    private void renderAd() {
        boolean ad = isShowingAd();
        View adCard = binding.adCard.getRoot();
        adCard.setVisibility(ad ? View.VISIBLE : View.GONE);
        binding.layoutAdActions.setVisibility(ad ? View.VISIBLE : View.GONE);
        if (!ad) return;
        adCard.setTranslationX(0f);
        adCard.setRotation(0f);
        adCard.setAlpha(1f);
        binding.cardFront.setVisibility(View.INVISIBLE);
        binding.cardBack.setVisibility(View.INVISIBLE);
        binding.btnShowAnswer.setVisibility(View.GONE);
        binding.layoutRate.setVisibility(View.GONE);
        binding.textHint.setVisibility(View.INVISIBLE);
    }

    private void swipeAdAway(float exitX) {
        isAnimating = true;
        binding.adCard.getRoot().animate()
                .translationX(exitX)
                .rotation(exitX * ROTATION_PER_PX)
                .alpha(0f)
                .setDuration(SWIPE_MS)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    isAnimating = false;
                    if (binding == null) return;
                    closeAd();
                    if (!showingBack) animateIn(binding.cardFront);
                })
                .start();
    }

    /** Removes the ad and brings back the card that was waiting under it. */
    private void closeAd() {
        viewModel.getAdSlot().dismiss();
        renderAd();
        showFace(showingBack);
    }

    // ── Animations ────────────────────────────────────────────────────────────

    private void flipCard() {
        if (isAnimating || isShowingAd()) return;
        View outView = currentFace();
        View inView = showingBack ? binding.cardFront : binding.cardBack;
        float direction = showingBack ? -1f : 1f;
        inView.setRotationY(FLIP_ANGLE * direction);
        inView.setVisibility(View.VISIBLE);

        ObjectAnimator flipOut = ObjectAnimator.ofFloat(outView, View.ROTATION_Y, 0f, -FLIP_ANGLE * direction).setDuration(FLIP_MS);
        ObjectAnimator flipIn = ObjectAnimator.ofFloat(inView, View.ROTATION_Y, FLIP_ANGLE * direction, 0f).setDuration(FLIP_MS);
        isAnimating = true;
        AnimatorSet set = new AnimatorSet();
        set.playSequentially(flipOut, flipIn);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimating = false;
                if (binding == null) return;
                showFace(!showingBack);
                viewModel.flip();
            }
        });
        set.start();
    }

    private void answer(boolean known) {
        if (isAnimating || isShowingAd()) return;
        isAnimating = true;
        binding.getRoot().performHapticFeedback(answerHaptic(known));
        // "Know it" sits at the end side (right in English, left in Persian): known cards leave that way
        float exitX = binding.cardContainer.getWidth() * EXIT_FACTOR * (known ? 1f : -1f) * endDirection();
        showSwipeLabel(exitX);
        currentFace().animate()
                .translationX(exitX)
                .rotation(exitX * ROTATION_PER_PX)
                .alpha(0f)
                .setDuration(SWIPE_MS)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    isAnimating = false;
                    if (binding == null) return;
                    hideSwipeLabel();
                    viewModel.answer(known);
                    maybeShowAd();
                })
                .start();
    }

    private static int answerHaptic(boolean known) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return known ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.REJECT;
        }
        return HapticFeedbackConstants.VIRTUAL_KEY;
    }

    private void animateCardIn() {
        animateIn(binding.cardFront);
    }

    private void animateIn(View card) {
        card.setAlpha(0f);
        card.setScaleX(ENTER_SCALE);
        card.setScaleY(ENTER_SCALE);
        card.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(SWIPE_MS).setInterpolator(new DecelerateInterpolator()).start();
    }

    /** +1 when the end side is to the right (left-to-right UI), -1 in a right-to-left UI. */
    private float endDirection() {
        return binding.getRoot().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL ? -1f : 1f;
    }

    private void snapBack() {
        hideSwipeLabel();
        currentFace().animate().translationX(0f).rotation(0f).alpha(1f)
                .setDuration(SWIPE_MS).setInterpolator(new OvershootInterpolator()).start();
    }

    private void drag(float dx) {
        View card = currentFace();
        card.setTranslationX(dx);
        card.setRotation(dx * ROTATION_PER_PX);
        card.setAlpha(Math.max(MIN_DRAG_ALPHA, 1f - Math.abs(dx) / binding.cardContainer.getWidth()));
        if (!isShowingAd()) showSwipeLabel(dx);
    }

    private void showSwipeLabel(float dx) {
        boolean known = dx * endDirection() > 0;
        int content = ContextCompat.getColor(requireContext(), known ? R.color.success : R.color.error);
        int container = ContextCompat.getColor(requireContext(), known ? R.color.success_container : R.color.error_container);
        binding.textSwipeLabel.setText(known ? R.string.flashcard_know : R.string.flashcard_again);
        binding.textSwipeLabel.setTextColor(content);
        binding.textSwipeLabel.setBackgroundTintList(ColorStateList.valueOf(container));
        float progress = Math.min(1f, Math.abs(dx) / Math.max(1f, swipeThreshold()));
        binding.textSwipeLabel.setAlpha(progress);

        View tint = showingBack ? binding.tintBack : binding.tintFront;
        tint.setBackgroundColor(content);
        tint.setAlpha(progress * MAX_TINT_ALPHA);
    }

    private void hideSwipeLabel() {
        binding.textSwipeLabel.animate().alpha(0f).setDuration(SWIPE_MS).start();
        binding.tintFront.animate().alpha(0f).setDuration(SWIPE_MS).start();
        binding.tintBack.animate().alpha(0f).setDuration(SWIPE_MS).start();
    }

    // ── Gestures ──────────────────────────────────────────────────────────────

    private void setupGestures() {
        binding.cardBackContent.setOnClickListener(v -> flipCard());
        binding.cardContainer.setListener(new SwipeCardLayout.Listener() {
            @Override
            public boolean isBlocked() {
                return isAnimating;
            }

            @Override
            public void onTap() {
                flipCard();
            }

            @Override
            public void onDrag(float dx) {
                drag(dx);
            }

            @Override
            public void onRelease(float dx) {
                if (isShowingAd()) {
                    // Either direction dismisses the ad; it never counts as an answer
                    if (Math.abs(dx) > swipeThreshold()) {
                        swipeAdAway(binding.cardContainer.getWidth() * EXIT_FACTOR * Math.signum(dx));
                    } else {
                        snapBack();
                    }
                    return;
                }
                float towardEnd = dx * endDirection();
                if (towardEnd > swipeThreshold()) {
                    answer(true);
                } else if (towardEnd < -swipeThreshold()) {
                    answer(false);
                } else {
                    snapBack();
                }
            }

            @Override
            public void onDragCancelled() {
                snapBack();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // The ad itself stays in the ViewModel for rotation; only this view lets go of it
        binding.adCard.nativeAdView.destroy();
        binding = null;
    }
}
