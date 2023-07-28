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
        busA = new Event.Bus<>();
        busB = new Event.Bus<>().setUpstream(busA);
        busC = busB.mapData(Object::toString).mapData(StandardValueType.INTEGER::parse);

        busA.listen().subscribe(e -> System.out.println("Bus A had data: "+e));
        busB.listen().setType(CharSequence.class).subscribe(e-> System.out.println("Bus B had char sequence: "+e));
        busC.listen().setPredicate(e->e.getData()>400).subscribe(e->System.out.println("Bus C was larger than 400: "+e));
    }

    @Test
    public void testEventBus() {
        busA.accept("123");
        busB.accept(420);
        busB.accept("666");
    }
}
