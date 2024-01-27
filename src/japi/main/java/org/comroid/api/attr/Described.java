package org.comroid.api.attr;

public interface Described {
    default String getDescription() {
        return "No description";
    }
}
