package org.comroid.test.util;

import org.comroid.util.Stopwatch;

public class StopwatchTest {
    public static void main(String[] args) throws InterruptedException {
        Stopwatch.start("");
        Thread.sleep(1000);
        var duration = Stopwatch.stop("");
        System.out.println("duration = " + duration);
    }
}
