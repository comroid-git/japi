package org.comroid.test.api;

import org.comroid.api.data.bind.DataStructure;
import org.comroid.api.data.seri.type.StandardValueType;
import org.comroid.api.data.seri.type.ValueType;
import org.comroid.api.func.util.Invocable;
import org.comroid.test.Dummy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DataStructureTest {
    @Test
    public void testFruit() {
        var struct = DataStructure.of(Dummy.Fruit.class);
        //System.out.println(Debug.createObjectDump(struct));

        Assertions.assertEquals(1, struct.getConstructors().size(), "invalid fruit constructor count");
        Assertions.assertEquals(2, struct.getDeclaredProperties().size(), "invalid fruit property count");

        //price
        testProp(struct, StandardValueType.DOUBLE, "price", 1.99);
    }

    @Test
    public void testApple() {
        var struct = DataStructure.of(Dummy.Apple.class);
        //System.out.println(Debug.createObjectDump(struct));

        Assertions.assertEquals(2, struct.getConstructors().size(), "invalid apple constructor count");
        Assertions.assertEquals(2, struct.getDeclaredProperties().size(), "invalid apple property count");

        // price
        testProp(struct, StandardValueType.DOUBLE, "price", 0.99);

        // hardness
        testProp(struct, StandardValueType.DOUBLE, "hardness", 0.8);
    }

    @Test
    public void testBanana() {
        var struct = DataStructure.of(Dummy.Banana.class);
        //System.out.println(Debug.createObjectDump(struct));

        Assertions.assertEquals(1, struct.getConstructors().size(), "invalid banana constructor count");
        Assertions.assertEquals(2, struct.getDeclaredProperties().size(), "invalid banana property count");

        // price
        testProp(struct, StandardValueType.DOUBLE, "price", 1.99);

        // ripeness -> color, color
        testProp(struct, StandardValueType.INTEGER, "color", 0x22dd88);
        testProp(struct, StandardValueType.INTEGER, "ripeness", 0x22dd88);
    }

    private void testProp(DataStructure<?> struct, ValueType<?> type, String key, Object expectedValue) {
        Assertions.assertTrue(struct.getProperty(key).isNonNull(), key + " property: missing");
        var prop = struct.getProperty(key).assertion();
        Assertions.assertNotNull(prop);
        var dummy = struct.getConstructors()
                .stream()
                .filter(ctor->ctor.getArgs().isEmpty())
                .findAny()
                .orElseThrow()
                .getCtor()
                .autoInvoke();

        // type
        Assertions.assertEquals(type, prop.getType(), key + " property: wrong type");

        // accessors
        Invocable<?> accessor = prop.getGetter();
        Assertions.assertNotNull(accessor, key + " property: getter missing");
        Assertions.assertEquals(expectedValue, accessor.invokeSilent(dummy), key + " property: getter unusable");
        if ("price".equals(prop.getName())) {
            final var newValue = 1.49;
            accessor = prop.getSetter();
            Assertions.assertNotNull(accessor, key + " property: setter missing");
            Assertions.assertEquals(newValue, accessor.invokeSilent(dummy, newValue), key + " property: setter unusable");
        }
    }
}
