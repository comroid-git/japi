package org.comroid.test.util;

import org.comroid.api.Polyfill;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.comroid.api.func.util.Streams.Multi.*;

public class StreamExtTest {
    @Test
    public void testBatches() {
        var list = Polyfill.batches(50, IntStream.range(0, 250).boxed()).toList();

        Assertions.assertEquals(5, list.size(), "Batch count");
        for (var batch : list)
            Assertions.assertEquals(50, batch.size(), "Batch length");
    }

    @Test
    public void testEntries() {
        Stream.of("mushroom", "doretta", "rock and stone!")
                .map(expand(String::length))
                .map(combine((s, l) -> s + l))
                .forEach(System.out::println);
    }
}
