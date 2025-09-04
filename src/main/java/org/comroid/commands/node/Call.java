package org.comroid.commands.node;

import lombok.Singular;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import org.comroid.api.data.seri.type.ValueType;
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
    public org.comroid.commands.node.Call asCall() {
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

    public ParameterAdapter[] adaptParameters() {
        var javaParameters = method.getParameters();
        var adapters       = new ParameterAdapter[javaParameters.length];

        for (var i = 0; i < javaParameters.length; i++) {
            var javaParam      = javaParameters[i];
            var cmdParamResult = parameters.stream().filter(p -> p.getParam().equals(javaParam)).findAny();
            adapters[i] = new ParameterAdapter(ValueType.of(javaParam.getType()),
                    javaParam,
                    cmdParamResult.orElse(null));
        }
        return adapters;
    }

    public record ParameterAdapter(
            ValueType<?> type, java.lang.reflect.Parameter javaParameter, @Nullable Parameter commandParameter
    ) {}
}
