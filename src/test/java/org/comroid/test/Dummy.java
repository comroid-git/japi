package org.comroid.test;

import lombok.ToString;
import org.comroid.annotations.Alias;
import org.comroid.annotations.Ignore;
import org.comroid.api.data.bind.DataStructure;
import org.comroid.api.tree.Component;

public class Dummy {
    public static final String AliasFruit  = "food";
    public static final String AliasApple  = "red delight";
    public static final String AliasBanana = "yellow bean";
    public static final String AliasBananaColor = "ripeness";

    public SomethingElse getSomethingElse() {
        return null;
    }

    public @Ignore
    static class SomethingElse {}

    @ToString
    @Alias(AliasFruit)
    public static class Fruit extends Component.Base {
        public double getPrice() {
            return 1.99;
        }

        @Ignore(DataStructure.class)
        public int getColor() {
            return 0xffffff;
        }

        public double setPrice(double price) {
            return price;
        }
    }

    @ToString
    @Alias(AliasApple)
    public static class Apple extends Fruit {
        public final double hardness;

        public Apple() {
            this(0.8);
        }

        public Apple(double hardness) {
            this.hardness = hardness;
        }

        @Override
        public double getPrice() {
            return 0.99;
        }
    }

    @ToString
    @Ignore.Inherit
    @Alias(AliasBanana)
    public static class Banana extends Fruit {
        public Banana() {
        }

        @Override
        @Alias(AliasBananaColor)
        @Ignore.Inherit(Ignore.class)
        public int getColor() {
            return 0x22dd88;
        }
    }
}
