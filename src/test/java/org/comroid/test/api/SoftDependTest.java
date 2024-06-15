package org.comroid.test.api;

import org.comroid.api.java.SoftDepend;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class SoftDependTest {
    public static final String Dummy = UUID.randomUUID().toString();

    public static String test() {
        return Dummy;
    }

    public static String invalid(int arg) {
        return null;
    }

    @Test
    public void testStatic() {
        Assertions.assertEquals(Integer.MAX_VALUE, SoftDepend.run("java.lang.Integer.MAX_VALUE").get(), "existent field assertion");
        Assertions.assertEquals(Dummy, SoftDepend.run("org.comroid.test.api.SoftDependTest.test").get(), "existent call assertion");
        Assertions.assertEquals(Dummy, SoftDepend.run("org.comroid.test.api.SoftDependTest.test()").get(), "existent call() assertion ");
        Assertions.assertNull(SoftDepend.run("org.comroid.test.api.SoftDependTest.invalid(3)").get(), "invalid call(param) assertion ");
        Assertions.assertNull(SoftDepend.run("org.comroid.mcsd.api.Defaults.Enabled").get(), "nonexistent assertion");
    }

    @Test
    public void testType() {
        Assertions.assertNotNull(SoftDepend.type("java.lang.String").get(), "existent assertion");
        Assertions.assertNull(SoftDepend.type("javax.persistence.Id").get(), "nonexistent assertion");
    }
}
