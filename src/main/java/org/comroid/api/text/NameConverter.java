package org.comroid.api.text;

import static java.lang.Character.*;

public enum NameConverter {
    GETTER("get"),
    SETTER("set");

    private final String prefix;

    NameConverter(String prefix) {this.prefix = prefix;}

    public String normalize(String input) {
        if (input.length() <= prefix.length() || !input.startsWith(prefix))
            return input;
        if (input.length() == prefix.length() + 1)
            return String.valueOf(toLowerCase(input.charAt(3)));
        return toLowerCase(input.charAt(3)) + input.substring(4);
    }

    public String decorate(String input) {
        if (input.startsWith(prefix))
            return input;
        var result = prefix + toUpperCase(input.charAt(0));
        if (input.length() > 1)
            result += input.substring(1);
        return result;
    }
}
