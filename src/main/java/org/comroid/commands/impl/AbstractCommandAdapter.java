package org.comroid.commands.impl;

import org.comroid.commands.model.CommandInfoProvider;
import org.comroid.commands.model.CommandResponseHandler;

public abstract class AbstractCommandAdapter implements CommandInfoProvider, CommandResponseHandler {
    @Override
    public void initialize() {
    }

    protected String[] strings(String label, String[] args) {
        var strings = new String[args.length + 1];
        strings[0] = label;
        System.arraycopy(args, 0, strings, 1, args.length);
        return strings;
    }
}
