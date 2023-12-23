package org.comroid.test.util;

import org.comroid.util.Capitalization;
import org.junit.Assert;
import org.junit.Test;

import static org.comroid.util.Capitalization.*;

public class CapitalizationTest {
    @Test
    public void testDetect() {
        Assert.assertEquals(lowerCamelCase, of("lowerCamelCase").get());
        Assert.assertEquals(UpperCamelCase, of("UpperCamelCase").get());
        Assert.assertEquals(lower_snake_case, of("lower_snake_case").get());
        Assert.assertEquals(Upper_Snake_Case, of("Upper_Snake_Case").get());
        Assert.assertEquals(CAPS_SNAKE_CASE, of("CAPS_SNAKE_CASE").get());
        Assert.assertEquals(lower_hyphen_case, of("lower-hyphen-case").get());
        Assert.assertEquals(Upper_Hyphen_Case, of("Upper-Hyphen-Case").get());
        Assert.assertEquals(CAPS_HYPHEN_CASE, of("CAPS-HYPHEN-CASE").get());
        Assert.assertEquals(lower_dot_case, of("lower.dot.case").get());
        Assert.assertEquals(Upper_Dot_Case, of("Upper.Dot.Case").get());
        Assert.assertEquals(CAPS_DOT_CASE, of("CAPS.DOT.CASE").get());
        Assert.assertEquals(Title_Case, of("Title Case").get());
    }

    @Test
    public void testConvert() {
        Assert.assertEquals("lowerCamelCase", lowerCamelCase.convert(lowerCamelCase, "lowerCamelCase"));
        Assert.assertEquals("LowerCamelCase", lowerCamelCase.convert(UpperCamelCase, "lowerCamelCase"));
        Assert.assertEquals("lower_camel_case", lowerCamelCase.convert(lower_snake_case, "lowerCamelCase"));
        Assert.assertEquals("Lower_Camel_Case", lowerCamelCase.convert(Upper_Snake_Case, "lowerCamelCase"));
        Assert.assertEquals("LOWER_CAMEL_CASE", lowerCamelCase.convert(CAPS_SNAKE_CASE, "lowerCamelCase"));
        Assert.assertEquals("lower-camel-case", lowerCamelCase.convert(lower_hyphen_case, "lowerCamelCase"));
        Assert.assertEquals("Lower-Camel-Case", lowerCamelCase.convert(Upper_Hyphen_Case, "lowerCamelCase"));
        Assert.assertEquals("LOWER-CAMEL-CASE", lowerCamelCase.convert(CAPS_HYPHEN_CASE, "lowerCamelCase"));
        Assert.assertEquals("lower.camel.case", lowerCamelCase.convert(lower_dot_case, "lowerCamelCase"));
        Assert.assertEquals("Lower.Camel.Case", lowerCamelCase.convert(Upper_Dot_Case, "lowerCamelCase"));
        Assert.assertEquals("LOWER.CAMEL.CASE", lowerCamelCase.convert(CAPS_DOT_CASE, "lowerCamelCase"));
        Assert.assertEquals("Lower Camel Case", lowerCamelCase.convert(Title_Case, "lowerCamelCase"));
    }
}
