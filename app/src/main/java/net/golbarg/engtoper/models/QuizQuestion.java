package net.golbarg.engtoper.models;

import java.util.List;

public class QuizQuestion {
    private String word;
    private String correctTranslation;
    private List<String> options;
    private int correctIndex;
    private String lang;

    public QuizQuestion(String word, String correctTranslation, List<String> options, int correctIndex, String lang) {
        this.word = word;
        this.correctTranslation = correctTranslation;
        this.options = options;
        this.correctIndex = correctIndex;
        this.lang = lang;
    }

    public String getWord() {
        return word;
    }

    public String getCorrectTranslation() {
        return correctTranslation;
    }

    public List<String> getOptions() {
        return options;
    }

    public int getCorrectIndex() {
        return correctIndex;
    }

    public String getLang() {
        return lang;
    }
}
