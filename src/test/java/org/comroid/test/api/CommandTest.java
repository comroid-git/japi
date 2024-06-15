package org.comroid.test.api;

import org.comroid.annotations.Alias;
import org.comroid.annotations.Description;
import org.comroid.api.func.util.Command;
import org.comroid.api.net.Token;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

public class CommandTest {
    public static final String Desc = "this is a cool command";

    @Command
    @Description(Desc)
    public static String test(@Alias("args") String[] args) {
        return String.valueOf(args.length);
    }

    @RepeatedTest(100)
    public void test() {
        var cmdr = new Command.Manager();
        cmdr.register(this);
        var cmd = cmdr.getBaseNodes().stream()
                .filter(node -> node.getName().equals("test"))
                .findAny().orElseThrow();

        Assertions.assertEquals(Desc, cmd.getDescription(), "description mismatch");
        Assertions.assertEquals("2", cmdr.execute("test first second"), "failed to execute command");
        var name = Token.random(8, false);
        Assertions.assertEquals("hello %s: %d".formatted(name, 1), cmdr.execute("user create " + name), "failed to execute command");
        Assertions.assertEquals(name + " is lit af", cmdr.execute("user " + name), "failed to execute command");
    }

    @Command
    public static class user {
        @Command
        public static String $(@Command.Arg String name) {
            return name + " is lit af";
        }

        @Command
        public static String create(@Alias("args") String[] args, @Command.Arg String name) {
            return "hello " + name + ": " + args.length;
        }
    }
}
