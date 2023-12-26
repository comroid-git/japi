package org.comroid.test.api;

import org.comroid.api.Component;
import org.comroid.test.Dummy;
import org.junit.Test;

import static org.junit.Assert.*;

public class ComponentTest {
    @Test
    public void testInject() {
        var basket = new Basket();

        basket.addChildren(new Dummy.Fruit(), new Dummy.Apple(), new Dummy.Banana());

        assertNull(basket.fruit);
        assertNull(basket.apple);

        basket.initialize();

        assertNotNull("Fruit was not injected", basket.fruit);
        assertNotNull("Apple was not injected", basket.apple);
        //assertNotNull("Banana was not injected", basket.yellow_bean);
    }

    public static class Basket extends Component.Base {
        private @Inject Dummy.Fruit fruit;
        private @Inject Dummy.Apple apple;
        //private @Inject Dummy.Banana yellow_bean;
    }
}
