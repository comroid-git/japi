package org.comroid.test.util;

import org.comroid.api.func.util.Bitmask;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BitmaskTest {
    @Test
    public void testArrange_1() {
        Assertions.assertEquals(0, Bitmask.arrange(false, false, false));
        Assertions.assertEquals(1, Bitmask.arrange(true, false, false));
        Assertions.assertEquals(2, Bitmask.arrange(false, true, false));
        Assertions.assertEquals(3, Bitmask.arrange(true, true, false));
        Assertions.assertEquals(4, Bitmask.arrange(false, false, true));
        Assertions.assertEquals(5, Bitmask.arrange(true, false, true));
        Assertions.assertEquals(6, Bitmask.arrange(false, true, true));
        Assertions.assertEquals(7, Bitmask.arrange(true, true, true));
    }
}
