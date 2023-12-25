package org.comroid.test;

import org.comroid.annotations.Ignore;

public class Dummy {
    public TestObj1 getTestObj1() {return null;}

    @Ignore
    public static class TestObj1 {}
}
