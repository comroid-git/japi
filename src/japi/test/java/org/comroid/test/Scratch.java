package org.comroid.test;

import org.comroid.api.func.util.DelegateStream;
import org.comroid.api.func.util.Event;

public class Scratch {
    public static void main(String[] args) {
        var stdio = new Event.Bus<String>();
        var io = new DelegateStream.IO().redirectToEventBus(stdio);

        stdio.publish("no print :(");
        stdio.publish(DelegateStream.IO.EventKey_Input, "hello world");
        stdio.publish(DelegateStream.IO.EventKey_Error, "hello error");
    }
}
