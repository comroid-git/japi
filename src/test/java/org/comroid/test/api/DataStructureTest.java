package org.comroid.test.api;

import org.comroid.api.data.seri.DataStructure;
import org.comroid.api.data.seri.StandardValueType;
import org.comroid.api.data.seri.ValueType;
import org.comroid.api.func.util.Debug;
import org.comroid.api.func.util.Invocable;
import org.comroid.test.Dummy;
import org.junit.Test;

import static org.junit.Assert.*;

public class DataStructureTest {
    @Test
    public void testFruit() {
        var struct = DataStructure.of(Dummy.Fruit.class);
        //System.out.println(Debug.createObjectDump(struct));

        assertEquals("invalid fruit constructor count", 1, struct.getConstructors().size());
        assertEquals("invalid fruit property count", 1, struct.getDeclaredProperties().size());

        //price
        testProp(struct, StandardValueType.DOUBLE, "price", 1.99);
    }

    @Test
    public void testApple() {
        var struct = DataStructure.of(Dummy.Apple.class);
        //System.out.println(Debug.createObjectDump(struct));

        assertEquals("invalid apple constructor count", 2, struct.getConstructors().size());
        assertEquals("invalid apple property count", 2, struct.getDeclaredProperties().size());

        // price
        testProp(struct, StandardValueType.DOUBLE, "price", 0.99);

        // hardness
        testProp(struct, StandardValueType.DOUBLE, "hardness", 0.8);
    }

    @Test
    public void testBanana() {
        var struct = DataStructure.of(Dummy.Banana.class);
        //System.out.println(Debug.createObjectDump(struct));

        assertEquals("invalid banana constructor count", 1, struct.getConstructors().size());
        assertEquals("invalid banana property count", 3, struct.getDeclaredProperties().size());

        // price
        testProp(struct, StandardValueType.DOUBLE, "price", 1.99);

        // ripeness -> color, color
        testProp(struct, StandardValueType.INTEGER, "color", 0x22dd88);
        testProp(struct, StandardValueType.INTEGER, "ripeness", 0x22dd88);
    }

    private void testProp(DataStructure<?> struct, ValueType<?> type, String key, Object expectedValue) {
        assertTrue(key+" property: missing", struct.getProperty(key).isNonNull());
        var prop = struct.getProperty(key).assertion();
        assertNotNull(prop);
        var dummy = struct.getConstructors()
                .stream()
                .filter(ctor->ctor.getArgs().isEmpty())
                .findAny()
                .orElseThrow()
                .getCtor()
                .autoInvoke();

        // type
        assertEquals(key+" property: wrong type", type, prop.getType());

        // accessors
        Invocable<?> accessor = prop.getGetter();
        assertNotNull(key+" property: getter missing", accessor);
        assertEquals(key+" property: getter unusable", expectedValue, accessor.invokeSilent(dummy));
        if ("price".equals(prop.getName())) {
            final var newValue = 1.49;
            accessor = prop.getSetter();
            assertNotNull(key+" property: setter missing", accessor);
            assertEquals(key+" property: setter unusable", newValue, accessor.invokeSilent(dummy, newValue));
        }
    }
}
