package org.comroid.test.api;

import org.comroid.api.text.Translation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;

public class TranslationTest {
    @Test
    public void testEnglish() {
        Assertions.assertEquals("This is a test string", Translation.get(Locale.ENGLISH).get("test"));
    }

    @Test
    public void testGerman() {
        Assertions.assertEquals("Dies ist ein Test String", Translation.get(Locale.GERMAN).get("test"));
    }
}
