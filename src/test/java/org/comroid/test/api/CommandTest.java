package org.comroid.test.api;

import org.comroid.annotations.Alias;
import org.comroid.annotations.Description;
import org.comroid.api.func.util.Command;
import org.comroid.api.net.Token;
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
        var name = Token.random(8, false);
        Assert.assertEquals("failed to execute command", "hello %s: %d".formatted(name, 1), cmdr.execute("user create " + name));
    }

    @Command
    public static class user {
        @Command
        public static String create(@Alias("args") String[] args, @Command.Arg String name) {
            return "hello " + name + ": " + args.length;
        }
    }
}
