package org.comroid.api.data;

import org.comroid.api.attr.Named;
import org.comroid.api.func.ValueBox;
import org.jetbrains.annotations.ApiStatus.Experimental;

import java.util.Set;

@Experimental
public interface Updateable<P> {
    <R extends Named & ValueBox<? super R>> Set<? extends R> updateFrom(P param);
}
