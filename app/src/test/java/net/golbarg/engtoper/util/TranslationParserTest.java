package net.golbarg.engtoper.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

/** Samples are real rows from assets/eng_per.db. */
public class TranslationParserTest {

    @Test
    public void englishEntry_splitsGeneralMeaningsAndSubjects() {
        TranslationParser.Parsed parsed = TranslationParser.parse(
                "ترک کردن ،ترک گفتن ،واگذارکردن ،تسلیم شدن<BR>کامپیوتر : رها کردن<BR>علوم نظامى : رها کردن<br><br></font>');");

        assertEquals(Arrays.asList("ترک کردن", "ترک گفتن", "واگذارکردن", "تسلیم شدن"), parsed.meanings);
        assertEquals(2, parsed.subjects.size());
        assertEquals("کامپیوتر", parsed.subjects.get(0).subject);
        assertEquals("رها کردن", parsed.subjects.get(0).meaning);
        assertEquals("علوم نظامى", parsed.subjects.get(1).subject);
    }

    @Test
    public void englishEntry_withOnlySubjectLine() {
        TranslationParser.Parsed parsed = TranslationParser.parse("ورزش : محل صرف نوشابه در باشگاه گلف<br><br></font>');");

        assertTrue(parsed.meanings.isEmpty());
        assertEquals("ورزش", parsed.subjects.get(0).subject);
        assertEquals("محل صرف نوشابه در باشگاه گلف", parsed.subjects.get(0).meaning);
    }

    @Test
    public void mirroredParentheses_areRestored() {
        TranslationParser.Parsed parsed = TranslationParser.parse(")intradermal(واقع در زیر پوست ،درون پوستى<br><br></font>');");

        assertEquals(Arrays.asList("(intradermal)واقع در زیر پوست", "درون پوستى"), parsed.meanings);
    }

    @Test
    public void persianEntry_splitsSynonymsAndReadsRelatedCount() {
        TranslationParser.Parsed parsed = TranslationParser.parse(
                "anticipant ; big ; enceinte ; pregnant واژه هاى شامل آبستن ـ (10)<br><br></font>');");

        assertEquals(Arrays.asList("anticipant", "big", "enceinte", "pregnant"), parsed.meanings);
        assertEquals(10, parsed.relatedCount);
    }

    @Test
    public void persianEntry_withOnlyRelatedCount_isEmpty() {
        TranslationParser.Parsed parsed = TranslationParser.parse("واژه هاى شامل قیمتها ـ (1)<br><br></font>');");

        assertTrue(parsed.isEmpty());
        assertEquals(1, parsed.relatedCount);
    }

    @Test
    public void duplicatesAreDropped_andPlainTextJoinsEverything() {
        TranslationParser.Parsed parsed = TranslationParser.parse("نور ،روشنایى ،نور<BR>معمارى : نور");

        assertEquals(Arrays.asList("نور", "روشنایى"), parsed.meanings);
        assertEquals("نور، روشنایى\nمعمارى: نور", parsed.toPlainText("، "));
    }
}
