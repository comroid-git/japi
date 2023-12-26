package org.comroid.test;

import org.comroid.annotations.Alias;
import org.comroid.annotations.Ignore;
import org.comroid.api.tree.Component;
import org.comroid.api.data.seri.DataStructure;

public class Dummy {
    public static final String AliasFruit = "food";
    public static final String AliasApple = "red delight";
    public static final String AliasBanana = "yellow bean";
    public static final String AliasBananaColor = "ripeness";

    public SomethingElse getSomethingElse() {
        return null;
    }

    public @Ignore static class SomethingElse {}

    @Alias(AliasFruit)
    public static class Fruit extends Component.Base {
        public double getPrice() {
            return 1.99;
        }

        public double setPrice(double price) {
            return price;
        }

        @Ignore(DataStructure.class)
        public int getColor() {
            return 0xffffff;
        }
    }

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
        @Ignore.Ancestor(Alias.class)
        public double getPrice() {
            return 0.99;
        }
    }

    @Ignore.Ancestor
    @Alias(AliasBanana)
    public static class Banana extends Fruit {
        public Banana() {
        }

        @Override
        @Alias(AliasBananaColor)
        @Ignore.Ancestor(Ignore.class)
        public int getColor() {
            return 0x22dd88;
        }
    }
}
