package org.comroid.test.util;

import org.comroid.api.Polyfill;
import org.junit.Assert;
import org.junit.Test;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.comroid.util.Streams.Multi.*;
import static org.comroid.util.Streams.Multi.Adapter.*;

public class StreamExtTest {
    @Test
    public void testBatches() {
        var list = Polyfill.batches(50, IntStream.range(0, 250).boxed()).toList();

        Assert.assertEquals("Batch count", 5, list.size());
        for (var batch : list)
            Assert.assertEquals("Batch length", 50, batch.size());
    }

    @Test
    public void testEntries() {
        Stream.of("mushroom", "doretta", "rock and stone!")
                .map(explode(String::length))
                .flatMap(flatMap(sideA(), s->s.chars().boxed()))
                .map(combine((s,l)->s+l))
                .forEach(System.out::println);
    }
}
