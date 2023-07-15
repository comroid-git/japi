package org.comroid.test.api;

import org.comroid.api.Context;
import org.comroid.api.Event;
import org.comroid.util.StandardValueType;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Executor;

public class EventTest {
    private Event.Bus<String> busA;
    private Event.Bus<Object> busB;
    private Event.Bus<Integer> busC;

    @Before
    public void setup() {
        Context.root().addToContext((Executor)Runnable::run);

        busA = new Event.Bus<>();
        busB = new Event.Bus<>().setUpstream(busA);
        busC = busB.map(Object::toString).map(StandardValueType.INTEGER::parse);

        busA.listen(e -> System.out.println("Bus A had data: "+e));
        busB.listen(CharSequence.class, e-> System.out.println("Bus B had char sequence: "+e));
        busC.listen(e->e.getData()>400, e->System.out.println("Bus C was larger than 400: "+e));
    }

    @Test
    public void testEventBus() {
        busA.accept("123");
        busB.accept(420);
        busB.accept("666");
    }
}
