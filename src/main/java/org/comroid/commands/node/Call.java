package org.comroid.commands.node;

import lombok.Singular;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.func.util.Invocable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

@Value
@SuperBuilder
public class Call extends Callable {
    @Nullable Object          target;
    @NotNull  Method          method;
    @NotNull  Invocable<?>    callable;
    @Singular List<Parameter> parameters;

    @Override
    public String getAlternateName() {
        return callable.getName();
    }

    @Override
    public @Nullable org.comroid.commands.node.Call asCall() {
        return this;
    }

    @Override
    public Stream<Parameter> nodes() {
        return parameters.stream().sorted(Parameter.COMPARATOR);
    }

    @Override
    public Wrap<AnnotatedElement> element() {
        return callable::accessor;
    }
}
