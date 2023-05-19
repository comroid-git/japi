package org.comroid.uti;

import org.comroid.util.Bitmask;
import org.junit.Assert;
import org.junit.Test;

public class BitmaskTest {
    @Test
    public void testArrange_1() {
        Assert.assertEquals(0, Bitmask.arrange(false,false,false));
        Assert.assertEquals(1, Bitmask.arrange(true,false,false));
        Assert.assertEquals(2, Bitmask.arrange(false,true,false));
        Assert.assertEquals(3, Bitmask.arrange(true,true,false));
        Assert.assertEquals(4, Bitmask.arrange(false,false,true));
        Assert.assertEquals(5, Bitmask.arrange(true,false,true));
        Assert.assertEquals(6, Bitmask.arrange(false,true,true));
        Assert.assertEquals(7, Bitmask.arrange(true,true,true));
    }
}
