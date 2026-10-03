package net.golbarg.engtoper.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import net.golbarg.engtoper.R;
import net.golbarg.engtoper.databinding.DialogQuizBinding;
import net.golbarg.engtoper.models.QuizQuestion;
import net.golbarg.engtoper.util.TTSManager;

import java.util.ArrayList;
import java.util.List;

public class QuizBottomSheetDialog extends BottomSheetDialogFragment {

    private DialogQuizBinding binding;
    private List<QuizQuestion> questions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int score = 0;
    private int streak = 0;
    private boolean isAnswered = false;

    private MaterialButton[] optionButtons;

    public static QuizBottomSheetDialog newInstance() {
        return new QuizBottomSheetDialog();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogQuizBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        optionButtons = new MaterialButton[]{
                binding.quizOption0,
                binding.quizOption1,
                binding.quizOption2,
                binding.quizOption3
        };

        for (int i = 0; i < optionButtons.length; i++) {
            final int index = i;
            optionButtons[i].setOnClickListener(v -> handleOptionSelected(index));
        }

        binding.quizBtnNext.setOnClickListener(v -> moveToNextQuestion());

        binding.quizBtnPlayAgain.setOnClickListener(v -> startNewQuiz());
        binding.quizBtnClose.setOnClickListener(v -> dismiss());

        binding.quizBtnSpeak.setOnClickListener(v -> {
            if (currentQuestionIndex < questions.size()) {
                TTSManager.getInstance(requireContext()).speak(
                        questions.get(currentQuestionIndex).getWord(),
                        requireContext()
                );
            }
        });

        startNewQuiz();
    }

    private void startNewQuiz() {
        currentQuestionIndex = 0;
        score = 0;
        streak = 0;
        isAnswered = false;

        binding.quizContainer.setVisibility(View.VISIBLE);
        binding.quizSummaryContainer.setVisibility(View.GONE);

        DictionaryViewModel viewModel = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(DictionaryViewModel.class);
        viewModel.getQuizQuestions().observe(getViewLifecycleOwner(), newQuestions -> {
            if (newQuestions != null && !newQuestions.isEmpty()) {
                questions = new ArrayList<>(newQuestions);
                binding.quizProgressBar.setMax(questions.size());
                displayQuestion(currentQuestionIndex);
            }
        });
        viewModel.startQuiz(5);
    }

    private void displayQuestion(int index) {
        if (index >= questions.size()) {
            showQuizSummary();
            return;
        }

        isAnswered = false;
        QuizQuestion question = questions.get(index);

        binding.quizProgressBar.setProgress(index + 1);
        binding.quizProgressText.setText(getString(R.string.flashcard_progress, index + 1, questions.size()));
        binding.quizStreakText.setText(getString(R.string.quiz_streak, streak));

        binding.quizWordText.setText(question.getWord());
        binding.quizFeedbackText.setVisibility(View.GONE);
        binding.quizBtnNext.setVisibility(View.GONE);

        // Auto speak English word
        TTSManager.getInstance(requireContext()).speak(question.getWord(), requireContext());

        List<String> options = question.getOptions();
        for (int i = 0; i < optionButtons.length; i++) {
            MaterialButton btn = optionButtons[i];
            btn.setEnabled(true);
            btn.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent));
            btn.setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.card_border_light));
            btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary_light));

            if (i < options.size()) {
                btn.setText(options.get(i));
                btn.setVisibility(View.VISIBLE);
            } else {
                btn.setVisibility(View.GONE);
            }
        }
    }

    private void handleOptionSelected(int selectedIndex) {
        if (isAnswered || currentQuestionIndex >= questions.size()) return;
        isAnswered = true;

        QuizQuestion question = questions.get(currentQuestionIndex);
        int correctIndex = question.getCorrectIndex();

        // Disable all buttons to prevent multiple clicks
        for (MaterialButton btn : optionButtons) {
            btn.setEnabled(false);
        }

        if (selectedIndex == correctIndex) {
            score++;
            streak++;
            optionButtons[selectedIndex].setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.success_container));
            optionButtons[selectedIndex].setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.success));
            optionButtons[selectedIndex].setTextColor(ContextCompat.getColor(requireContext(), R.color.black));

            binding.quizFeedbackText.setText(R.string.quiz_correct);
            binding.quizFeedbackText.setTextColor(ContextCompat.getColor(requireContext(), R.color.success));
        } else {
            streak = 0;
            optionButtons[selectedIndex].setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.error_container));
            optionButtons[selectedIndex].setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.error));
            optionButtons[selectedIndex].setTextColor(ContextCompat.getColor(requireContext(), R.color.black));

            // Highlight correct one
            if (correctIndex >= 0 && correctIndex < optionButtons.length) {
                optionButtons[correctIndex].setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.success_container));
                optionButtons[correctIndex].setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.success));
                optionButtons[correctIndex].setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            }

            binding.quizFeedbackText.setText(getString(R.string.quiz_wrong, question.getCorrectTranslation()));
            binding.quizFeedbackText.setTextColor(ContextCompat.getColor(requireContext(), R.color.error));
        }

        binding.quizStreakText.setText(getString(R.string.quiz_streak, streak));
        binding.quizFeedbackText.setVisibility(View.VISIBLE);
        binding.quizBtnNext.setVisibility(View.VISIBLE);

        if (currentQuestionIndex == questions.size() - 1) {
            binding.quizBtnNext.setText(R.string.quiz_done);
        } else {
            binding.quizBtnNext.setText(R.string.quiz_next_word);
        }
    }

    private void moveToNextQuestion() {
        currentQuestionIndex++;
        if (currentQuestionIndex < questions.size()) {
            displayQuestion(currentQuestionIndex);
        } else {
            showQuizSummary();
        }
    }

    private void showQuizSummary() {
        binding.quizContainer.setVisibility(View.GONE);
        binding.quizSummaryContainer.setVisibility(View.VISIBLE);

        int total = questions.size();
        int percentage = total > 0 ? (score * 100) / total : 0;

        if (percentage >= 80) {
            binding.quizResultEmoji.setText("🏆");
        } else if (percentage >= 50) {
            binding.quizResultEmoji.setText("🌟");
        } else {
            binding.quizResultEmoji.setText("💪");
        }

        binding.quizResultScore.setText(getString(R.string.quiz_completed_msg, score, total, percentage));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
