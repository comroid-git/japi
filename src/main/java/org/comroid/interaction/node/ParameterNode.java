package org.comroid.interaction.node;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.comroid.interaction.annotation.Completion;
import org.comroid.interaction.annotation.Parameter;
import org.comroid.interaction.node.model.Node;
import org.comroid.interaction.registry.RegistrySource;
import org.jspecify.annotations.NonNull;

import java.util.Collection;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PROTECTED)
public class ParameterNode extends Node {
    java.lang.reflect.Parameter      reflect;
    Parameter.Resolved               parameter;
    Collection<? extends Completion> completion;

    public ParameterNode(
            @NonNull RegistrySource source, java.lang.reflect.Parameter reflect, Parameter.Resolved parameter, Collection<? extends Completion> completion) {
        super(source);

        this.reflect    = reflect;
        this.parameter  = parameter;
        this.completion = completion;
    }
}
