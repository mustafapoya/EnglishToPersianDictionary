package net.golbarg.engtoper.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class EnglishFormsTest {

    @Test
    public void clean_stripsQuotesAndPunctuation() {
        assertEquals("Hello", EnglishForms.clean("  “Hello,”  "));
        assertEquals("well-known", EnglishForms.clean("(well-known)."));
        assertEquals("first line", EnglishForms.clean("first line\nsecond line"));
    }

    @Test
    public void wordAsWrittenComesFirst() {
        List<String> forms = EnglishForms.candidates("Running");
        assertEquals("Running", forms.get(0));
        assertEquals("running", forms.get(1));
    }

    @Test
    public void baseFormsAreTried() {
        assertTrue(EnglishForms.candidates("running").contains("run"));
        assertTrue(EnglishForms.candidates("making").contains("make"));
        assertTrue(EnglishForms.candidates("studies").contains("study"));
        assertTrue(EnglishForms.candidates("stopped").contains("stop"));
        assertTrue(EnglishForms.candidates("boxes").contains("box"));
        assertTrue(EnglishForms.candidates("quickly").contains("quick"));
    }

    /** Lookup takes the first form the dictionary has, so the right word must come before a wrong one. */
    @Test
    public void likelyBaseFormComesBeforeAWrongRealWord() {
        assertBefore("hope", "hop", "hoping");
        assertBefore("hope", "hop", "hopes");
        assertBefore("care", "car", "caring");
        assertBefore("note", "not", "notes");
        assertBefore("use", "us", "uses");
        assertBefore("dine", "din", "dined");
        assertBefore("add", "ad", "added");
        assertBefore("walk", "walke", "walked");
        assertBefore("happy", "happi", "happier");
    }

    private static void assertBefore(String expected, String wrong, String word) {
        List<String> forms = EnglishForms.candidates(word);
        assertTrue(word + " should try " + expected, forms.contains(expected));
        assertTrue(word + ": " + forms, !forms.contains(wrong) || forms.indexOf(expected) < forms.indexOf(wrong));
    }

    @Test
    public void phrasesAndShortWordsAreLeftAlone() {
        assertEquals(2, EnglishForms.candidates("Pay dividends").size());
        assertEquals(1, EnglishForms.candidates("is").size());
    }
}
