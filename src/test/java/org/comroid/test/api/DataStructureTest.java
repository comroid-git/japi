package org.comroid.test.api;

import org.comroid.api.DataStructure;
import org.comroid.test.Dummy;
import org.junit.Test;

import static org.comroid.annotations.Annotations.*;
import static org.comroid.test.Dummy.*;
import static org.junit.Assert.*;

public class DataStructureTest {
    @Test
    public void testFruit() {
        var struct = DataStructure.of(Dummy.Fruit.class);
        assertEquals("invalid property count", 1, struct.getProperties().size());

        //price
        assertTrue("price property missing", struct.getProperties().containsKey("price"));
    }

    @Test
    public void testApple() {
        var struct = DataStructure.of(Dummy.Apple.class);
        assertEquals("invalid property count", 2, struct.getProperties().size());

        // price
        assertTrue("price property missing", struct.getProperties().containsKey("price"));

        // hardness
        assertTrue("hardness property missing", struct.getProperties().containsKey("hardness"));
    }

    @Test
    public void testBanana() {
        var struct = DataStructure.of(Dummy.Banana.class);
        assertEquals("invalid property count", 2, struct.getProperties().size());

        // price
        assertTrue("price property missing", struct.getProperties().containsKey("price"));

        // ripeness -> color, color
        assertTrue("color property missing", struct.getProperties().containsKey("color"));
        assertTrue("ripeness property missing", struct.getProperties().containsKey("ripeness"));
    }
}
