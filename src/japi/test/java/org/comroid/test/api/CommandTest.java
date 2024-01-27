package org.comroid.test.api;

import org.comroid.api.func.util.Command;
import org.junit.Assert;
import org.junit.Test;

public class CommandTest {
    @Command
    public static String test(String[] args) {
        return String.valueOf(args.length);
    }

    @Test
    public void test() {
        var cmd = new Command.Manager();
        cmd.register(this);

        Assert.assertEquals("2", cmd.execute("test first second"));
    }
}
