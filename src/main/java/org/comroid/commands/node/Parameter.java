package org.comroid.commands.node;

import lombok.Singular;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import org.comroid.annotations.AnnotatedTarget;
import org.comroid.annotations.Default;
import org.comroid.api.func.ext.Wrap;
import org.comroid.commands.Command;
import org.comroid.commands.autofill.IAutoFillProvider;
import org.comroid.commands.impl.CommandUsage;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.AnnotatedElement;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Value
@SuperBuilder
public class Parameter extends Node implements IAutoFillProvider, Default.Extension {
    public static             Comparator<? super org.comroid.commands.node.Parameter> COMPARATOR = Comparator.comparingInt(
            param -> param.index);
    @NotNull                  Command.Arg                                             attribute;
    @NotNull @AnnotatedTarget java.lang.reflect.Parameter                             param;
    boolean required;
    int     index;
    @Singular List<IAutoFillProvider> autoFillProviders;

    @Override
    public Stream<? extends CharSequence> autoFill(CommandUsage usage, String argName, String currentValue) {
        return autoFillProviders.stream()
                .filter(Objects::nonNull)
                .flatMap(provider -> provider.autoFill(usage, argName, currentValue))
                .distinct();
    }

    @Override
    public Wrap<AnnotatedElement> element() {
        return () -> param;
    }
}
