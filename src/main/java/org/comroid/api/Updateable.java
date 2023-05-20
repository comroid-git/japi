package org.comroid.api;

import org.jetbrains.annotations.ApiStatus.Experimental;

import java.util.Set;

@Experimental
public interface Updateable<P> {
    <R extends Named & ValueBox<? super R>> Set<? extends R> updateFrom(P param);
}
