package org.comroid.test.util;

import org.comroid.api.Polyfill;
import org.junit.Assert;
import org.junit.Test;

import java.util.stream.IntStream;

public class StreamExtTest {
    @Test
    public void testBatches() {
        var list = Polyfill.batches(50, IntStream.range(0, 250).boxed()).toList();

        Assert.assertEquals("Batch count", 5, list.size());
        for (var batch : list)
            Assert.assertEquals("Batch length", 50, batch.size());
    }
}
