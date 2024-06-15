package org.comroid.test.api;

import org.comroid.api.attr.Named;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NamedTest {
    private Named named;

    @BeforeEach
    public void setup() {
        named = new Named() {
            @Override
            public String getName() {
                return "name";
            }
        };
    }

    @Test
    public void test() {
        Assertions.assertEquals("name", String.format("%s", named));
        Assertions.assertEquals("Name", String.format("%#s", named));
    }
}
