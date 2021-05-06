package org.comroid.api;

import java.util.Set;

public interface Updateable<P> {
    <R extends Named & ValueBox> Set<? extends R> updateFrom(P param);
}
