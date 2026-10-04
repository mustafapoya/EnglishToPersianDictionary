package net.golbarg.engtoper.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class SpellSuggesterTest {

    @Test
    public void swappedLetters_countAsOneMistake() {
        assertEquals(1, SpellSuggester.distance("recieve", "receive", 3));
        assertEquals(1, SpellSuggester.distance("teh", "the", 3));
    }

    @Test
    public void distance_stopsEarlyPastTheLimit() {
        assertEquals(3, SpellSuggester.distance("apple", "zzzzz", 2));
    }

    @Test
    public void rank_ordersByClosenessThenLength_andSkipsTheQuery() {
        List<String> candidates = Arrays.asList("receive", "received", "recieve", "relieve", "recipe", "rocket");
        List<String> result = SpellSuggester.rank("recieve", candidates, 5);

        assertEquals("receive", result.get(0));
        assertTrue(result.contains("relieve"));
        assertFalse("never suggests the query itself", result.contains("recieve"));
        assertFalse("too far away", result.contains("rocket"));
    }

    @Test
    public void rank_respectsLimit_andDropsDuplicates() {
        // Every candidate is one edit away; same-length words rank first, then the rest
        List<String> result = SpellSuggester.rank("bok", Arrays.asList("book", "Book", "boo", "bog", "boa"), 2);
        assertEquals(Arrays.asList("boa", "bog"), result);

        List<String> withDuplicate = SpellSuggester.rank("bok", Arrays.asList("book", "Book", "boo"), 5);
        assertEquals(2, withDuplicate.size());
        assertEquals(1, withDuplicate.stream().filter(w -> w.equalsIgnoreCase("book")).count());
    }

    @Test
    public void persianYehVariants_areTheSameWord() {
        // Data writes final yeh as alef maksura (U+0649); keyboards type Persian yeh (U+06CC)
        String typed = "زندگی";   // زندگی
        String stored = "زندگى";  // زندگى
        assertEquals(PersianText.normalize(typed), PersianText.normalize(stored));
        // Identical after normalising, so it is the query itself, not a suggestion
        assertTrue(SpellSuggester.rank(typed, Arrays.asList(stored), 5).isEmpty());
    }

    @Test
    public void persianTypo_isSuggested() {
        String typed = "زندکی";   // زندکی (kaf instead of gaf)
        String stored = "زندگى";  // زندگى
        assertEquals(Arrays.asList(stored), SpellSuggester.rank(typed, Arrays.asList(stored), 5));
    }

    @Test
    public void detectsPersianScript() {
        assertTrue(PersianText.containsPersian("سلام"));
        assertFalse(PersianText.containsPersian("hello"));
    }
}
