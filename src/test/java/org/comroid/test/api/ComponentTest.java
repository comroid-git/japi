package org.comroid.test.api;

import org.comroid.api.tree.Component;
import org.comroid.test.Dummy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ComponentTest {
    @Test
    public void testInject() {
        var basket = new Basket();

        basket.addChildren(new Dummy.Fruit(), new Dummy.Apple(), new Dummy.Banana());

        Assertions.assertNull(basket.fruit);
        Assertions.assertNull(basket.apple);

        basket.initialize();

        Assertions.assertNotNull(basket.fruit, "Fruit was not injected");
        Assertions.assertNotNull(basket.apple, "Apple was not injected");
        //assertNotNull("Banana was not injected", basket.yellow_bean);
    }

    public static class Basket extends Component.Base {
        private @Inject Dummy.Fruit fruit;
        private @Inject Dummy.Apple apple;
        //private @Inject Dummy.Banana yellow_bean;
    }
}
