package org.comroid.test.api;

import org.comroid.annotations.Alias;
import org.comroid.annotations.Description;
import org.comroid.api.func.util.Command;
import org.junit.Assert;
import org.junit.Test;

public class CommandTest {
    public static final String Desc = "this is a cool command";

    @Command
    @Description(Desc)
    public static String test(@Alias("args") String[] args) {
        return String.valueOf(args.length);
    }

    @Test
    public void test() {
        var cmdr = new Command.Manager();
        cmdr.register(this);
        var cmd = cmdr.getBaseNodes().stream()
                .filter(node -> node.getName().equals("test"))
                .findAny().orElseThrow();

        Assert.assertEquals("description mismatch", Desc, cmd.getDescription());
        Assert.assertEquals("failed to execute command", "2", cmdr.execute("test first second"));
    }
}
