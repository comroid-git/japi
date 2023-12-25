package org.comroid.test;

import org.comroid.annotations.Alias;
import org.comroid.annotations.Ignore;
import org.comroid.api.DataStructure;

import java.awt.*;

public class Dummy {
    public static final String AliasFruit = "food";
    public static final String AliasApple = "red delight";
    public static final String AliasBanana = "yellow bean";
    public static final String AliasBananaColor = "ripeness";

    public Fruit getFruit() {
        return null;
    }

    @Ignore
    @Alias(AliasFruit)
    public static class Fruit {
        public double getPrice() {
            return 1.99;
        }

        @Ignore(DataStructure.class)
        public int getColor() {
            return 0xffffff;
        }
    }

    @Alias(AliasApple)
    public static class Apple extends Fruit {
        public final double hardness = 0.9;

        @Override
        @Ignore.Ancestor(Alias.class)
        public double getPrice() {
            return 0.99;
        }
    }

    @Ignore.Ancestor
    @Alias(AliasBanana)
    public static class Banana extends Fruit {
        @Override
        @Alias(AliasBananaColor)
        @Ignore.Ancestor(Ignore.class)
        public int getColor() {
            return 0x22dd88;
        }
    }
}
