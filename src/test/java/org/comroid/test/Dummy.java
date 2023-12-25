package org.comroid.test;

import org.comroid.annotations.Alias;
import org.comroid.annotations.Ignore;

public class Dummy {
    public static final String AliasFruit = "food";
    public static final String AliasApple = "red delight";
    public static final String AliasBanana = "yellow bean";

    public Fruit getFruit() {
        return null;
    }

    @Ignore
    @Alias(AliasFruit)
    public static class Fruit {
        public double getPrice() {
            return 1.99;
        }
    }

    @Alias(AliasApple)
    public static class Apple extends Fruit {
        @Override
        @Ignore.Ancestor(Alias.class)
        public double getPrice() {
            return 0.99;
        }
    }

    @Ignore.Ancestor
    @Alias(AliasBanana)
    public static class Banana extends Fruit {
    }
}
