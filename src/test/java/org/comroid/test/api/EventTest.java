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

        busA.listen(e -> System.out.println("Bus A had data: "+e));
        busB.listen(CharSequence.class, e-> System.out.println("Bus B had char sequence: "+e));
        busC.listen(e->e.getData()>400, e->System.out.println("Bus C was larger than 400: "+e));

        busA.publish("123");
        busB.publish(420);
        busB.publish("666");
    }
}
