package org.comroid.commands.impl;

import org.comroid.commands.model.CommandInfo;
import org.comroid.commands.model.CommandResponseHandler;

import java.util.stream.Stream;

import static java.util.stream.Stream.*;

public abstract class AbstractCommandAdapter implements CommandInfo, CommandResponseHandler {
    @Override
    public void initialize() {
    }

    @Override
    public Stream<Object> expandContext(Object... context) {
        return of(context);
    }

    protected String[] strings(String label, String[] args) {
        var strings = new String[args.length + 1];
        strings[0] = label;
        System.arraycopy(args, 0, strings, 1, args.length);
        return strings;
    }
}
