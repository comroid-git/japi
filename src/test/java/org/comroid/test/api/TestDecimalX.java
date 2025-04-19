package org.comroid.test.api;

import org.comroid.api.data.DecimalX;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class TestDecimalX {
    static final Random rng = new Random();

    @Test
    public void testSimple1() {
        attempt(123);
    }

    @Test
    public void testSimple2() {
        attempt(1001);
    }

    @Test
    public void testSimple3() {
        attempt(100.1);
    }

    @Test
    public void testSimple4() {
        attempt(10.01);
    }

    @RepeatedTest(value = 100, failureThreshold = 3)
    public void testInteger() {
        attempt(rng.nextInt());
    }

    @RepeatedTest(value = 100, failureThreshold = 3)
    public void testDouble() {
        attempt(rng.nextInt(1000) + rng.nextDouble());
    }

    private void attempt(double expected) {
        var actual = DecimalX.parse(String.valueOf(expected));
        checks(expected, actual);
    }

    private void checks(double expected, DecimalX actual) {
        //assertEquals(expected % 1 != 0 ? String.valueOf(expected) : String.valueOf((long) expected), actual.toString());
        assertEquals((int) expected, actual.intValue());
        assertEquals((long) expected, actual.longValue());
        assertEquals((float) expected, actual.floatValue());
        assertEquals(expected, actual.doubleValue());
    }
}
