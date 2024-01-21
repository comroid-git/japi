package org.comroid.test.api;

import org.comroid.api.java.SoftDepend;
import org.junit.Assert;
import org.junit.Test;

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
        Assert.assertEquals("existent field assertion", Integer.MAX_VALUE, SoftDepend.run("java.lang.Integer.MAX_VALUE").get());
        Assert.assertEquals("existent call assertion", Dummy, SoftDepend.run("org.comroid.test.api.SoftDependTest.test").get());
        Assert.assertEquals("existent call() assertion ", Dummy, SoftDepend.run("org.comroid.test.api.SoftDependTest.test()").get());
        Assert.assertNull("invalid call(param) assertion ", SoftDepend.run("org.comroid.test.api.SoftDependTest.invalid(3)").get());
        Assert.assertNull("nonexistent assertion", SoftDepend.run("org.comroid.mcsd.api.Defaults.Enabled").get());
    }

    @Test
    public void testType() {
        Assert.assertNotNull("existent assertion", SoftDepend.type("java.lang.String").get());
        Assert.assertNull("nonexistent assertion", SoftDepend.type("javax.persistence.Id").get());
    }
}
