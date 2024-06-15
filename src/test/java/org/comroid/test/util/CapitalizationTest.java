package org.comroid.test.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.comroid.api.text.Capitalization.*;

public class CapitalizationTest {
    @Test
    public void testDetect() {
        Assertions.assertEquals(lowerCamelCase, of("lowerCamelCase").get());
        Assertions.assertEquals(UpperCamelCase, of("UpperCamelCase").get());
        Assertions.assertEquals(lower_snake_case, of("lower_snake_case").get());
        Assertions.assertEquals(Upper_Snake_Case, of("Upper_Snake_Case").get());
        Assertions.assertEquals(CAPS_SNAKE_CASE, of("CAPS_SNAKE_CASE").get());
        Assertions.assertEquals(lower_hyphen_case, of("lower-hyphen-case").get());
        Assertions.assertEquals(Upper_Hyphen_Case, of("Upper-Hyphen-Case").get());
        Assertions.assertEquals(CAPS_HYPHEN_CASE, of("CAPS-HYPHEN-CASE").get());
        Assertions.assertEquals(lower_dot_case, of("lower.dot.case").get());
        Assertions.assertEquals(Upper_Dot_Case, of("Upper.Dot.Case").get());
        Assertions.assertEquals(CAPS_DOT_CASE, of("CAPS.DOT.CASE").get());
        Assertions.assertEquals(Title_Case, of("Title Case").get());
    }

    @Test
    public void testConvert() {
        Assertions.assertEquals("lowerCamelCase", lowerCamelCase.convert(lowerCamelCase, "lowerCamelCase"));
        Assertions.assertEquals("LowerCamelCase", lowerCamelCase.convert(UpperCamelCase, "lowerCamelCase"));
        Assertions.assertEquals("lower_camel_case", lowerCamelCase.convert(lower_snake_case, "lowerCamelCase"));
        Assertions.assertEquals("Lower_Camel_Case", lowerCamelCase.convert(Upper_Snake_Case, "lowerCamelCase"));
        Assertions.assertEquals("LOWER_CAMEL_CASE", lowerCamelCase.convert(CAPS_SNAKE_CASE, "lowerCamelCase"));
        Assertions.assertEquals("lower-camel-case", lowerCamelCase.convert(lower_hyphen_case, "lowerCamelCase"));
        Assertions.assertEquals("Lower-Camel-Case", lowerCamelCase.convert(Upper_Hyphen_Case, "lowerCamelCase"));
        Assertions.assertEquals("LOWER-CAMEL-CASE", lowerCamelCase.convert(CAPS_HYPHEN_CASE, "lowerCamelCase"));
        Assertions.assertEquals("lower.camel.case", lowerCamelCase.convert(lower_dot_case, "lowerCamelCase"));
        Assertions.assertEquals("Lower.Camel.Case", lowerCamelCase.convert(Upper_Dot_Case, "lowerCamelCase"));
        Assertions.assertEquals("LOWER.CAMEL.CASE", lowerCamelCase.convert(CAPS_DOT_CASE, "lowerCamelCase"));
        Assertions.assertEquals("Lower Camel Case", lowerCamelCase.convert(Title_Case, "lowerCamelCase"));
    }
}
