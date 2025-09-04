package org.comroid.commands.node;

import lombok.Singular;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import org.comroid.annotations.AnnotatedTarget;
import org.comroid.api.func.ext.Wrap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Stream.*;
import static org.comroid.api.func.util.Streams.*;

@Value
@SuperBuilder
public class Group extends Callable {
    @NotNull @AnnotatedTarget         Class<?>                              source;
    @Singular                         List<org.comroid.commands.node.Group> groups;
    @Singular                         List<Call>                            calls;
    @Nullable @lombok.Builder.Default Call                                  defaultCall = null;

    @Override
    public @Nullable Call asCall() {
        return defaultCall;
    }

    @Override
    public Stream<Callable> nodes() {
        var stream = concat(groups.stream(), calls.stream());
        if (defaultCall != null) stream = stream.collect(append(defaultCall));
        return stream;
    }

    @Override
    public Wrap<AnnotatedElement> element() {
        return () -> source;
    }
}
