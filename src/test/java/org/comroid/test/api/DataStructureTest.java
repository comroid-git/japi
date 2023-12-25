package org.comroid.test.api;

import lombok.extern.java.Log;
import org.comroid.api.DataStructure;
import org.comroid.api.ValueType;
import org.comroid.test.Dummy;
import org.comroid.util.StandardValueType;
import org.junit.Test;

import static org.junit.Assert.*;

@Log
public class DataStructureTest {
    @Test
    public void testFruit() {
        var struct = DataStructure.of(Dummy.Fruit.class);
        assertEquals("invalid fruit constructor count", 1, struct.getConstructors().size());
        assertEquals("invalid fruit property count", 1, struct.getProperties().size());

        //price
        testProp(struct, StandardValueType.DOUBLE, "price", 1.99);
    }

    @Test
    public void testApple() {
        var struct = DataStructure.of(Dummy.Apple.class);
        assertEquals("invalid apple constructor count", 2, struct.getConstructors().size());
        assertEquals("invalid apple property count", 2, struct.getProperties().size());

        // price
        testProp(struct, StandardValueType.DOUBLE, "price", 0.99);

        // hardness
        testProp(struct, StandardValueType.DOUBLE, "hardness", 0.8);
    }

    @Test
    public void testBanana() {
        var struct = DataStructure.of(Dummy.Banana.class);
        assertEquals("invalid banana constructor count", 1, struct.getConstructors().size());
        assertEquals("invalid banana property count", 3, struct.getProperties().size());

        // price
        testProp(struct, StandardValueType.DOUBLE, "price", 1.99);

        // ripeness -> color, color
        testProp(struct, StandardValueType.INTEGER, "color", 0x22dd88);
        testProp(struct, StandardValueType.INTEGER, "ripeness", 0x22dd88);
    }

    private void testProp(DataStructure<?> struct, ValueType<?> type, String key, Object expectedValue) {
        assertTrue(key+" property: missing", struct.getProperties().containsKey(key));
        var prop = struct.getProperties().get(key);
        var dummy = struct.getConstructors()
                .stream()
                .filter(ctor->ctor.getArgs().isEmpty())
                .findAny()
                .orElseThrow()
                .getFunc()
                .autoInvoke();

        // type
        assertEquals(key+" property: wrong type", type, prop.getType());

        // accessors
        var getter = prop.getGetter();
        assertNotNull(key+" property: getter missing", getter);
        assertEquals(key+" property: getter unusable", expectedValue, getter.invokeSilent(dummy));
        if ("price".equals(prop.getName())) {
            final var newValue = 1.49;
            assertNotNull(key+" property: setter missing", prop.getSetter());
            assertEquals(key+" property: setter unusable", newValue, getter.invokeSilent(dummy, newValue));
        }
    }
}
