package org.comroid.test.api;

import org.comroid.api.Event;
import org.comroid.util.StandardValueType;
import org.junit.Test;

public class EventTest {
    @Test
    public void testEventBus() {
        var busA = new Event.Bus<String>();
        var busB = new Event.Bus<Object>(busA);
        var busC = busB.map(Object::toString).map(StandardValueType.INTEGER::parse);

        busA.listen(it->it.ifPresent(e -> System.out.println("BusA had data: "+e)));
        busB.listen(CharSequence.class, seq-> System.out.println("BusB had char sequence: "+seq));
        busC.listen(e->e.getData()>400, x->System.out.println("BusC was larger than 400: "+x));

        busA.publish("123");
        busB.publish(420);
    }
}
